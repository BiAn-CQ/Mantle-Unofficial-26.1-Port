package slimeknights.mantle.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.model.FallbackModelLoader;
import slimeknights.mantle.client.model.ItemKey;
import slimeknights.mantle.client.model.ItemKeyModel;
import slimeknights.mantle.client.model.builder.FallbackModelBuilder;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.MultiNamedRecipe;
import slimeknights.mantle.recipe.MultiRecipeHolder;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.recipe.ingredient.PotionDisplayIngredient;
import slimeknights.mantle.recipe.ingredient.fluid.PotionFluidIngredient;
import slimeknights.mantle.registration.deferred.EnumDeferredRegister;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.util.CombatHelper;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeFullSyncTest {
  private static final RegistryAccess REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
  @BeforeAll static void components() {
    for (var item : List.of(Items.STICK, Items.APPLE, Items.POTION, Items.BUCKET)) {
      if (!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    for (var fluid : List.of(Fluids.WATER,Fluids.LAVA)) {
      if (!fluid.builtInRegistryHolder().areComponentsBound()) fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  @Test void sizedNativeFluidJsonPreservesTheNativeShapeAndAmount() {
    var json=JsonParser.parseString("{\"ingredient\":\"minecraft:water\",\"amount\":250}");
    var context=ContextKey.lookupContext(REGISTRIES).build();
    var fluid=FluidIngredient.LOADABLE.convert(json,"fluid",context);
    assertThat(fluid.test(new FluidStack(Fluids.WATER,250))).isTrue();
    assertThat(fluid.test(new FluidStack(Fluids.WATER,249))).isFalse();
    assertThat(fluid.test(new FluidStack(Fluids.LAVA,1000))).isFalse();
    assertThat(FluidIngredient.LOADABLE.serialize(fluid,context)).isEqualTo(json);
  }

  @Test void nativeFluidBridgeRetainsPotionRestrictionsAndDisplayComponents() {
    var potion=new PotionFluidIngredient(HolderSet.direct(Potions.HEALING),HolderSet.direct(Fluids.WATER.builtInRegistryHolder()));
    var fluid=FluidIngredient.of(new SizedFluidIngredient(potion,250));
    var healing=new FluidStack(Fluids.WATER,250);
    healing.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.HEALING));
    var swiftness=healing.copy();swiftness.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.SWIFTNESS));
    assertThat(fluid.test(healing)).isTrue();
    assertThat(fluid.test(swiftness)).isFalse();
    assertThat(fluid.test(new FluidStack(Fluids.WATER,250))).isFalse();
    assertThat(fluid.getFluids()).singleElement().satisfies(display -> {
      assertThat(display.getAmount()).isEqualTo(250);
      assertThat(display.get(DataComponents.POTION_CONTENTS).potion()).contains(Potions.HEALING);
    });
  }

  @Test void legacyCompoundFluidNetworkKeepsDifferentRequiredAmounts() {
    var fluid=FluidIngredient.of(FluidIngredient.of(Fluids.WATER,250),FluidIngredient.of(Fluids.LAVA,500));
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),REGISTRIES);
    try {
      FluidIngredient.LOADABLE.encode(buffer,fluid);
      var copy=FluidIngredient.LOADABLE.decode(buffer);
      assertThat(copy.test(new FluidStack(Fluids.WATER,250))).isTrue();
      assertThat(copy.test(new FluidStack(Fluids.LAVA,499))).isFalse();
      assertThat(copy.test(new FluidStack(Fluids.LAVA,500))).isTrue();
      assertThat(buffer.readableBytes()).isZero();
    } finally { buffer.release(); }
  }

  @Test void potionDisplayAcceptsNativeAndLegacyJsonAndRetainsHolderSetsOnTheWire() {
    var ops=REGISTRIES.createSerializationContext(JsonOps.INSTANCE);
    var modern=PotionDisplayIngredient.MAP_CODEC.codec().parse(ops,JsonParser.parseString("{\"items\":[\"minecraft:potion\"]}")).getOrThrow();
    var legacy=PotionDisplayIngredient.MAP_CODEC.codec().parse(ops,JsonParser.parseString("{\"item\":\"minecraft:potion\"}")).getOrThrow();
    assertThat(modern).isEqualTo(legacy);
    assertThat(modern.isSimple()).isFalse();
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),REGISTRIES);
    try {
      PotionDisplayIngredient.STREAM_CODEC.encode(buffer,modern);
      assertThat(PotionDisplayIngredient.STREAM_CODEC.decode(buffer)).isEqualTo(modern);
      assertThat(buffer.readableBytes()).isZero();
    } finally { buffer.release(); }
  }

  @Test void nativeFluidContainerCodecSupportsExplicitDisplayAndAutomaticContainer() {
    var ops=REGISTRIES.createSerializationContext(JsonOps.INSTANCE);
    var codec=slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient.CODEC.codec();
    for (String json:List.of("{\"fluid\":\"minecraft:water\",\"amount\":1000,\"display\":\"minecraft:bucket\"}",
        "{\"fluid\":\"minecraft:water\",\"amount\":1000,\"container\":\"minecraft:bucket\"}")) {
      var value=codec.parse(ops,JsonParser.parseString(json)).getOrThrow();
      assertThat(value.amount()).isEqualTo(1000);
      assertThat(codec.parse(ops,codec.encodeStart(ops,value).getOrThrow()).getOrThrow()).isEqualTo(value);
    }
  }

  @Test void fallbackModelsSelectAnAvailableLoaderWithoutDecodingAnUnavailableOne() {
    var optional=JsonParser.parseString("{\"type\":\"uninstalled:custom\"}").getAsJsonObject();
    var fallback=JsonParser.parseString("{\"model\":\"minecraft:block/stone\"}").getAsJsonObject();
    var json=new FallbackModelBuilder().fallback(optional).fallback(fallback).toJson();
    var candidates=json.getAsJsonArray("models").asList().stream().map(value -> value.getAsJsonObject()).toList();
    assertThat(FallbackModelLoader.select(candidates,mod -> false)).isEqualTo(fallback);
    assertThat(FallbackModelLoader.select(candidates,mod -> mod.equals("uninstalled"))).isEqualTo(optional);
    assertThatThrownBy(() -> new FallbackModelBuilder().fallback(fallback).toJson()).isInstanceOf(IllegalStateException.class);
    assertThat(optional.has("fallback_mod_id")).isFalse();
  }

  @Test void itemKeyModelRoutesKnownAndMissingKeysToTheExpectedModel() {
    var chosen=mock(net.minecraft.client.renderer.item.ItemModel.class);
    var fallback=mock(net.minecraft.client.renderer.item.ItemModel.class);
    var known=new ItemStack(Items.STICK);
    var unknown=new ItemStack(Items.APPLE);
    var model=new ItemKeyModel(ItemKey.simple(stack -> stack.is(Items.STICK)?"known":"unknown"),fallback,Map.of("known",chosen));
    var display=net.minecraft.world.item.ItemDisplayContext.GUI;
    model.update(null,known,null,display,null,null,1);
    model.update(null,unknown,null,display,null,null,2);
    verify(chosen).update(null,known,null,display,null,null,1);
    verify(fallback).update(null,unknown,null,display,null,null,2);
  }

  @Test void offhandSweepAttributeSwapsTheWeaponModifiers() {
    var attribute=Attributes.SWEEPING_DAMAGE_RATIO;
    var mainModifier=new AttributeModifier(Identifier.parse("test:main"),0.5,AttributeModifier.Operation.ADD_VALUE);
    var offModifier=new AttributeModifier(Identifier.parse("test:off"),0.2,AttributeModifier.Operation.ADD_VALUE);
    var main=new ItemStack(Items.STICK);
    var off=new ItemStack(Items.APPLE);
    main.set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(attribute,mainModifier,EquipmentSlotGroup.MAINHAND).build());
    off.set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(attribute,offModifier,EquipmentSlotGroup.MAINHAND).build());
    var entity=mock(LivingEntity.class);
    var instance=new AttributeInstance(attribute,ignored -> {});
    instance.addTransientModifier(mainModifier);
    when(entity.getMainHandItem()).thenReturn(main);
    when(entity.getAttribute(attribute)).thenReturn(instance);
    when(entity.getAttributeValue(attribute)).thenReturn(0.5);
    assertThat(CombatHelper.getAttribute(off,entity,attribute,InteractionHand.OFF_HAND)).isCloseTo(0.2f,within(0.0001f));
    assertThat(CombatHelper.getAttribute(main,entity,attribute,InteractionHand.MAIN_HAND)).isEqualTo(0.5f);
    assertThat(instance.getValue()).isEqualTo(0.5);
  }

  enum Variant { FIRST,SECOND }
  @Test void enumGenerationKeepsRegistryHolderIdentityWithoutResolvingIt() {
    var object=EnumObject.generate(Variant.values(),value -> new slimeknights.mantle.registration.object.ItemObject<>(BuiltInRegistries.ITEM,Items.STICK));
    assertThat(object.getHolder(Variant.FIRST)).isSameAs(Items.STICK.builtInRegistryHolder());
    assertThat(object.holders()).hasSize(2);
    assertThat(EnumDeferredRegister.prefix("test").apply(Variant.FIRST)).isEqualTo("test_first");
    assertThat(EnumDeferredRegister.suffix("test").apply(Variant.SECOND)).isEqualTo("second_test");
  }

  static class LegacyRecipe extends CustomRecipe implements IMultiRecipe<Recipe<?>> {
    private final Recipe<?> child;
    LegacyRecipe(Recipe<?> child) { super(); this.child=child; }
    @Override public List<Recipe<?>> getRecipes(RegistryAccess access) { return List.of(child); }
    @Override public boolean matches(CraftingInput input,net.minecraft.world.level.Level level) { return false; }
    @Override public ItemStack assemble(CraftingInput input) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return null; }
  }
  @Test void namedRecipeHelpersExpandLegacyRecipesAndKeepTheirSourceId() {
    var child=mock(CraftingRecipe.class);when(child.getType()).thenReturn(RecipeType.CRAFTING);
    var id=Identifier.parse("test:parent");
    var base=new RecipeHolder<>(net.minecraft.resources.ResourceKey.<Recipe<?>>create(net.minecraft.core.registries.Registries.RECIPE,id),new LegacyRecipe(child));
    var manager=mock(RecipeManager.class);
    when(manager.recipeMap()).thenReturn(RecipeMap.create(List.of(base)));
    var named=MultiNamedRecipe.getNamedRecipes(REGISTRIES,manager,RecipeType.CRAFTING,CraftingRecipe.class);
    assertThat(named).singleElement().satisfies(recipe -> {
      assertThat(recipe.recipe()).isSameAs(child);
      assertThat(recipe.sourceIds()).containsExactly(id);
      assertThat(recipe.id().toString()).isEqualTo("test:parent/mantle_multirecipe/0");
    });
    assertThat(MultiRecipeHolder.getVanillaRecipes(REGISTRIES,manager,RecipeType.CRAFTING)).singleElement().satisfies(holder -> assertThat(holder.value()).isSameAs(child));
  }

  @Test void legacyBookTransformerUsesEveryEntryAcrossMultipleContentsPages() {
    var book=new slimeknights.mantle.client.book.data.BookData();
    var section=mock(slimeknights.mantle.client.book.data.SectionData.class);
    section.name="chapter";
    section.pages=new java.util.ArrayList<>();
    for (int index=0;index<49;index++) {
      var page=mock(slimeknights.mantle.client.book.data.PageData.class);
      page.name="page"+index;
      page.extraData=Map.of();
      when(page.getTitle()).thenReturn("Page "+index);
      section.pages.add(page);
    }
    when(section.getTitle()).thenReturn("Chapter");
    book.sections=new java.util.ArrayList<>(List.of(section));
    slimeknights.mantle.client.book.transformer.BookTransformer.contentTableTransformer().transform(book);
    assertThat(section.pages).hasSize(52);
    for (int index=0;index<3;index++) {
      var contents=(slimeknights.mantle.client.book.data.content.ContentTableOfContents)section.pages.get(index).content;
      assertThat(contents.data).hasSize(index==2?1:24);
    }
    var last=(slimeknights.mantle.client.book.data.content.ContentTableOfContents)section.pages.get(2).content;
    assertThat(last.data[0].action).isEqualTo("mantle:go-to-page-rtn chapter.page48");
  }

  static class RecordTransfer implements slimeknights.mantle.fluid.transfer.IFluidContainerTransfer {
    static final slimeknights.mantle.data.loadable.record.RecordLoadable<RecordTransfer> LOADABLE=
      slimeknights.mantle.data.loadable.record.RecordLoadable.create(
        slimeknights.mantle.data.loadable.primitive.IntLoadable.FROM_ZERO.requiredField("value", transfer -> transfer.value),RecordTransfer::new);
    final int value;
    RecordTransfer(int value) { this.value=value; }
    @Override public slimeknights.mantle.data.loadable.record.RecordLoadable<RecordTransfer> getLoader() { return LOADABLE; }
    @Override public void addRepresentativeItems(java.util.function.Consumer<net.minecraft.world.item.Item> consumer) {}
    @Override public boolean matches(ItemStack stack,FluidStack fluid) { return false; }
    @Override public TransferResult transfer(ItemStack stack,FluidStack fluid,net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler,TransferDirection direction) { return null; }
  }
  @Test void transferRegistrySupportsNewRecordOnlyExtensionsForJsonAndNetwork() {
    var registry=slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.LOADER;
    registry.register(Identifier.parse("mantle_test:record_transfer"),RecordTransfer.LOADABLE);
    var json=JsonParser.parseString("{\"type\":\"mantle_test:record_transfer\",\"value\":37}").getAsJsonObject();
    var transfer=registry.deserialize(json,slimeknights.mantle.util.typed.TypedMap.EMPTY);
    assertThat(((RecordTransfer)transfer).value).isEqualTo(37);
    assertThat(registry.serialize(transfer)).isEqualTo(json);
    assertThat(transfer.serialize(null)).isEqualTo(json);
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),REGISTRIES);
    try {
      registry.encode(buffer,transfer);
      assertThat(((RecordTransfer)registry.decode(buffer)).value).isEqualTo(37);
      assertThat(buffer.readableBytes()).isZero();
    } finally { buffer.release(); }
  }

  @Test void nativeTransferRecordsRoundTripWithoutExpandingOutputTags() {
    var json=JsonParser.parseString("{\"type\":\"mantle:empty_item\",\"input\":\"minecraft:stick\",\"result\":{\"item\":\"minecraft:apple\"},\"fluid\":{\"fluid\":\"minecraft:water\",\"amount\":250}}");
    var registry=slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.LOADER;
    var context=ContextKey.lookupContext(REGISTRIES).build();
    var transfer=registry.convert(json,"transfer",context);
    assertThat(transfer.matches(new ItemStack(Items.STICK),FluidStack.EMPTY)).isTrue();
    var encoded=registry.serialize(transfer,context);
    assertThat(registry.convert(encoded,"transfer",context).getClass()).isEqualTo(transfer.getClass());
  }

  @Test void optionalOutputCodecsRoundTripEmptyValuesAndReadOfficialFieldNames() {
    var context=ContextKey.lookupContext(REGISTRIES).build();
    for (var codec:List.of(slimeknights.mantle.recipe.helper.ItemOutput.OPTIONAL_ITEM_CODEC,slimeknights.mantle.recipe.helper.ItemOutput.OPTIONAL_STACK_CODEC)) {
      var ops=REGISTRIES.createSerializationContext(JsonOps.INSTANCE);
      var encoded=codec.encodeStart(ops,slimeknights.mantle.recipe.helper.ItemOutput.EMPTY).getOrThrow();
      assertThat(codec.parse(ops,encoded).getOrThrow().isEmpty()).isTrue();
    }
    var item=slimeknights.mantle.recipe.helper.ItemOutput.REQUIRED_STACK_LOADABLE.convert(JsonParser.parseString("{\"item\":\"minecraft:apple\",\"count\":2}"),"item",context);
    assertThat(item.getCount()).isEqualTo(2);
    assertThat(item.getItem()).isSameAs(Items.APPLE);
    var fluid=slimeknights.mantle.recipe.helper.FluidOutput.REQUIRED_LOADABLE.convert(JsonParser.parseString("{\"fluid\":\"minecraft:water\",\"amount\":250}"),"fluid",context);
    assertThat(fluid.getAmount()).isEqualTo(250);
  }
  @Test void cookingTimeCodecReadsBothSupportedFieldNames() {
    var codec=slimeknights.mantle.recipe.cooking.CookingResultRecipe.cookingTimeField(200).codec();
    assertThat(codec.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"cooking_time\":73}")).getOrThrow()).isEqualTo(73);
    assertThat(codec.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"cookingtime\":91}")).getOrThrow()).isEqualTo(91);
    assertThat(codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow()).isEqualTo(200);
  }

  @Test void stringLoadableSerializesWithTheCallersContextWhileBooleansStayBoolean() {
    var prefix=new ContextKey<String>("prefix");
    var loadable=new slimeknights.mantle.data.loadable.primitive.StringLoadable<String>() {
      @Override public String parseString(String value,String key,slimeknights.mantle.util.typed.TypedMap context) { return value; }
      @Override public String getString(String value,slimeknights.mantle.util.typed.TypedMap context) { return context.get(prefix)+value; }
      @Override public String decode(net.minecraft.network.FriendlyByteBuf buffer,slimeknights.mantle.util.typed.TypedMap context) { return buffer.readUtf(); }
      @Override public void encode(net.minecraft.network.FriendlyByteBuf buffer,String value) { buffer.writeUtf(value); }
    };
    var context=slimeknights.mantle.util.typed.TypedMap.builder().put(prefix,"context:").build();
    assertThat(loadable.serialize("value",context).getAsString()).isEqualTo("context:value");
    assertThat(slimeknights.mantle.data.loadable.primitive.BooleanLoadable.INSTANCE.serialize(true,context).getAsJsonPrimitive().isBoolean()).isTrue();
  }

  @Test void pairedTagProvidersGenerateMatchingBlockAndItemTags(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
    var pack=new net.minecraft.data.PackOutput(directory);
    var lookup=java.util.concurrent.CompletableFuture.<net.minecraft.core.HolderLookup.Provider>completedFuture(REGISTRIES);
    var pair=slimeknights.mantle.registration.object.BlockItemTagKey.create(Identifier.parse("mantle_test:paired"));
    var blocks=new slimeknights.mantle.data.BlockTagProviderExtension(pack,lookup,"mantle_test") {
      @Override protected void addTags(net.minecraft.core.HolderLookup.Provider registry) { tag(pair).add(net.minecraft.world.level.block.Blocks.STONE); }
    };
    var items=new slimeknights.mantle.data.ItemTagProviderExtension(pack,lookup,blocks.contentsGetter(),"mantle_test") {
      @Override protected void addTags(net.minecraft.core.HolderLookup.Provider registry) { copy(pair); }
    };
    blocks.run(net.minecraft.data.CachedOutput.NO_CACHE).join();
    items.run(net.minecraft.data.CachedOutput.NO_CACHE).join();
    var blockJson=JsonParser.parseString(java.nio.file.Files.readString(directory.resolve("data/mantle_test/tags/block/paired.json")));
    var itemJson=JsonParser.parseString(java.nio.file.Files.readString(directory.resolve("data/mantle_test/tags/item/paired.json")));
    assertThat(itemJson).isEqualTo(blockJson);
    assertThat(itemJson.getAsJsonObject().getAsJsonArray("values").get(0).getAsString()).isEqualTo("minecraft:stone");
  }

  @Test void missingCookingOutputTagsProduceAnEmptyDisplayWithoutATemplateFailure() {
    var output=slimeknights.mantle.recipe.helper.ItemOutput.fromTag(net.minecraft.tags.ItemTags.create(Identifier.parse("mantle_test:missing_output")),1);
    var recipe=slimeknights.mantle.recipe.cooking.CookingRecipeBuilder.CookingType.SMELTING.create("",CookingBookCategory.MISC,Ingredient.of(Items.STICK),output,0,200,true);
    var display=(net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay)recipe.display().getFirst();
    assertThat(display.result()).isSameAs(net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE);
  }

  @Test void mixedLegacyAndNativeFluidArraysRoundTripThroughJson() {
    var context=ContextKey.lookupContext(REGISTRIES).build();
    var json=JsonParser.parseString("[{\"fluid\":\"minecraft:water\",\"amount\":250},{\"ingredient\":\"minecraft:lava\",\"amount\":500}]");
    var ingredient=FluidIngredient.LOADABLE.convert(json,"fluids",context);
    var encoded=FluidIngredient.LOADABLE.serialize(ingredient,context);
    var copy=FluidIngredient.LOADABLE.convert(encoded,"fluids",context);
    assertThat(copy.test(new FluidStack(Fluids.WATER,250))).isTrue();
    assertThat(copy.test(new FluidStack(Fluids.LAVA,500))).isTrue();
    assertThat(copy.test(new FluidStack(Fluids.LAVA,499))).isFalse();
  }
  @Test void compoundFluidAmountsFollowTheMatchingComponentPredicate() {
    var holder=HolderSet.direct(Fluids.WATER.builtInRegistryHolder());
    var healing=FluidIngredient.of(new SizedFluidIngredient(new PotionFluidIngredient(HolderSet.direct(Potions.HEALING),holder),250));
    var swift=FluidIngredient.of(new SizedFluidIngredient(new PotionFluidIngredient(HolderSet.direct(Potions.SWIFTNESS),holder),500));
    var compound=FluidIngredient.of(healing,swift);
    var stack=new FluidStack(Fluids.WATER,500);
    stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.SWIFTNESS));
    assertThat(compound.test(stack)).isTrue();
    assertThat(compound.getAmount(stack)).isEqualTo(500);
  }
}
