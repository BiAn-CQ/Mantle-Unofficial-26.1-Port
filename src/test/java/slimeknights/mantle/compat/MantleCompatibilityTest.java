package slimeknights.mantle.compat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.effect.MobEffectInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.book.data.content.ContentTextLeftImage;
import slimeknights.mantle.client.book.data.content.ContentTextRightImage;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.data.gson.IdentifierSerializer;
import slimeknights.mantle.data.gson.ResourceLocationSerializer;
import slimeknights.mantle.data.loadable.common.GsonLoadable;
import slimeknights.mantle.recipe.container.IRecipeContainer;
import slimeknights.mantle.recipe.cooking.CookingRecipeBuilder;
import slimeknights.mantle.recipe.crafting.ShapedFallbackRecipe;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipeBuilder;
import slimeknights.mantle.block.fluid.MobEffectLiquidBlock;
import slimeknights.mantle.command.DumpLootModifiers;
import slimeknights.mantle.command.tags.ModifyTagCommand;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/** Behavioral regressions for the 1.21 features adapted to the current Minecraft APIs. */
class MantleCompatibilityTest {
  @BeforeAll
  static void bindPureTestComponents() {
    if (!Items.STICK.builtInRegistryHolder().areComponentsBound()) {
      Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  @Test
  void bookImageTextAcceptsModernAndLegacyFields() {
    new BookLoader();
    Gson gson = BookLoader.getGson();
    var left = gson.fromJson("{\"right_text\":[{\"text\":\"Beside\"}],\"bottom_text\":[{\"text\":\"Below\"}]}", ContentTextLeftImage.class);
    var right = gson.fromJson("{\"left_text\":[{\"text\":\"Beside\"}],\"bottom_text\":[{\"text\":\"Below\"}]}", ContentTextRightImage.class);
    assertThat(left.text1).hasSize(1);
    assertThat(left.text2).hasSize(1);
    assertThat(right.text1).hasSize(1);
    assertThat(right.text2).hasSize(1);
    var legacy = gson.fromJson("{\"text1\":[{\"text\":\"Retained\"}]}", ContentTextLeftImage.class);
    assertThat(legacy.text1).hasSize(1);
  }

  @Test
  void gsonLoadableRoundTripsNumbersAndArrays() {
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    try {
      var primitive = new GsonLoadable<>(new Gson(), Integer.class);
      primitive.encode(buffer, 7);
      assertThat(primitive.decode(buffer)).isEqualTo(7);
      var array = new GsonLoadable<>(new Gson(), int[].class);
      array.encode(buffer, new int[]{1, 2, 3});
      assertThat(array.decode(buffer)).containsExactly(1, 2, 3);
    } finally {
      buffer.release();
    }
  }

  @Test
  void emptyAndMissingNamespacesUseTheConfiguredDefault() {
    var json = new JsonPrimitive(":page");
    var expected = Identifier.fromNamespaceAndPath("mantle", "page");
    var direct = IdentifierSerializer.resourceLocation("mantle");
    var generic = ResourceLocationSerializer.resourceLocation("mantle");
    assertThat(direct.deserialize(new JsonPrimitive("page"), Identifier.class, null)).isEqualTo(expected);
    assertThat(direct.deserialize(json, Identifier.class, null)).isEqualTo(expected);
    assertThat(generic.deserialize(json, Identifier.class, null)).isEqualTo(expected);
  }





  @Test
  void recipeBuildersAcceptUnboundTags() {
    var tag = TagKey.create(Registries.ITEM, Identifier.parse("test:datagen_tag"));
    assertThatCode(() -> Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag))).doesNotThrowAnyException();
    assertThatCode(() -> CookingRecipeBuilder.builder(Items.STICK).requires(tag)).doesNotThrowAnyException();
    var builder = ShapedRetexturedRecipeBuilder.fromShaped(mock(ShapedRecipeBuilder.class));
    assertThatCode(() -> builder.setSource(tag)).doesNotThrowAnyException();
    assertThat(slimeknights.mantle.data.loadable.common.IngredientLoadable.DISALLOW_EMPTY.serialize(Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag))))
      .isEqualTo(new JsonPrimitive("#test:datagen_tag"));
  }

  @Test
  void recipeContainerScansItsSlots() {
    RecipeInput nativeInput = new RecipeInput() {
      @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
      @Override public int size() { return 2; }
    };
    IRecipeContainer localInput = new IRecipeContainer() {
      @Override public int getContainerSize() { return 2; }
      @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    };
    assertThat(nativeInput.isEmpty()).isTrue();
    assertThat(localInput.isEmpty()).isTrue();
  }

  @Test
  void shapedFallbackChecksCustomCraftingAlternatives() {
    var common = new Recipe.CommonInfo(true);
    var book = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");
    var pattern = ShapedRecipePattern.of(Map.of('S', Ingredient.of(Items.STICK)), List.of("S"));
    var result = new ItemStackTemplate(Items.STICK, 1, DataComponentPatch.EMPTY);
    var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
    var id = Identifier.parse("test:alternative");
    var key = ResourceKey.<Recipe<?>>create(Registries.RECIPE, id);
    var server = mock(ServerLevel.class);
    var manager = mock(RecipeManager.class);
    when(server.recipeAccess()).thenReturn(manager);

    var vanilla = new ShapedRecipe(common, book, pattern, result);
    when(manager.byKey(key)).thenReturn(Optional.of(new RecipeHolder<>(key, vanilla)));
    var fallback = new ShapedFallbackRecipe(common, book, pattern, result, List.of(id));
    assertThat(fallback.matches(input, server)).isFalse();

    ShapedRecipe custom = new ShapedRecipe(common, book, pattern, result) {
      @Override public boolean matches(CraftingInput grid, Level level) { return true; }
    };
    when(manager.byKey(key)).thenReturn(Optional.of(new RecipeHolder<>(key, custom)));
    var customFallback = new ShapedFallbackRecipe(common, book, pattern, result, List.of(id));
    assertThat(custom.matches(input, server)).isTrue();
    assertThat(customFallback.matches(input, server)).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void effectFluidBlockAppliesEffectsOnlyWhileImmersed() throws ReflectiveOperationException {
    Supplier<MobEffectInstance> supplier = mock(Supplier.class);
    var effect = mock(MobEffectInstance.class);
    when(supplier.get()).thenReturn(effect);
    // Block registration is frozen in the pure test loader; bypass construction,
    // supplying the same fields and dispatching the real BlockState contact hook.
    var block = mock(MobEffectLiquidBlock.class, CALLS_REAL_METHODS);
    var effectField = MobEffectLiquidBlock.class.getDeclaredField("effect");
    effectField.setAccessible(true);
    effectField.set(block, supplier);
    var fluidField = LiquidBlock.class.getDeclaredField("fluid");
    fluidField.setAccessible(true);
    fluidField.set(block, Fluids.WATER);
    var state = mock(BlockState.class, CALLS_REAL_METHODS);
    doReturn(block).when(state).getBlock();
    var entity = mock(LivingEntity.class);
    when(entity.getFluidTypeHeight(Fluids.WATER.getFluidType())).thenReturn(0.0);
    state.entityInside(mock(Level.class), BlockPos.ZERO, entity, InsideBlockEffectApplier.NOOP, false);
    verifyNoInteractions(supplier);
    verify(entity, never()).addEffect(any(MobEffectInstance.class));
    when(entity.getFluidTypeHeight(Fluids.WATER.getFluidType())).thenReturn(1.0);
    state.entityInside(mock(Level.class), BlockPos.ZERO, entity, InsideBlockEffectApplier.NOOP, false);
    verify(supplier).get();
    verify(entity).addEffect(effect);
  }

  @Test
  @SuppressWarnings("unchecked")
  void lootModifierDumpReadsNeoForgeResources() throws ReflectiveOperationException, java.io.IOException {
    var nativePath = Identifier.parse("neoforge:loot_modifiers/global_loot_modifiers.json");
    var obsoletePath = Identifier.parse("forge:loot_modifiers/global_loot_modifiers.json");
    var resource = mock(Resource.class);
    when(resource.openAsReader()).thenReturn(new java.io.BufferedReader(new StringReader("{\"entries\":[\"test:modifier\"]}")));
    var resources = mock(ResourceManager.class);
    when(resources.getResourceStack(nativePath)).thenReturn(List.of(resource));
    var server = mock(MinecraftServer.class);
    when(server.getResourceManager()).thenReturn(resources);
    var source = mock(CommandSourceStack.class);
    when(source.getServer()).thenReturn(server);
    CommandContext<CommandSourceStack> context = mock(CommandContext.class);
    when(context.getSource()).thenReturn(source);
    var run = DumpLootModifiers.class.getDeclaredMethod("run", CommandContext.class, boolean.class);
    run.setAccessible(true);
    assertThat(run.invoke(null, context, false)).isEqualTo(1);
    verify(resources).getResourceStack(nativePath);
    verify(resources, never()).getResourceStack(obsoletePath);
  }

  @Test
  void tagCommandErrorAcceptsRegistryIdentifiers() throws ReflectiveOperationException {
    var registry = Identifier.parse("minecraft:item");
    var tag = Identifier.parse("test:tag");
    var field = ModifyTagCommand.class.getDeclaredField("ERROR_WRITING_TAG");
    field.setAccessible(true);
    var error = (Dynamic2CommandExceptionType) field.get(null);
    var message = (Component) error.create(registry, tag).getRawMessage();
    assertThat(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, message).result()).isPresent();
    var safe = Component.translatableEscape("command.mantle.modify_tag.write_error", registry, tag);
    assertThat(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, safe).result()).isPresent();
  }

  @Test
  void lootModifierAcceptsOmittedFunctions() {
    var builder = slimeknights.mantle.loot.ReplaceItemLootModifier.builder(
      Ingredient.of(Items.STICK), slimeknights.mantle.recipe.helper.ItemOutput.fromStack(new ItemStack(Items.STICK)));
    var codec = slimeknights.mantle.loot.ReplaceItemLootModifier.CODEC.codec();
    var ops = net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,
      net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    JsonObject json = codec.encodeStart(ops, builder.build()).getOrThrow().getAsJsonObject();
    assertThat(codec.parse(ops, json).result()).isPresent();
    json.remove("functions");
    assertThat(codec.parse(ops, json).result()).isPresent();
  }

  @Test
  void indirectDamageDoesNotRequireAnAttacker() {
    var source = mock(net.minecraft.world.damagesource.DamageSource.class);
    when(source.getEntity()).thenReturn(null);
    when(source.getDirectEntity()).thenReturn(mock(net.minecraft.world.entity.Entity.class));
    when(source.isDirect()).thenReturn(false);
    assertThat(source.getEntity() != source.getDirectEntity()).isTrue();
    assertThat(slimeknights.mantle.data.predicate.damage.DamageSourcePredicate.IS_INDIRECT.matches(source)).isTrue();
  }





  @Test
  void tagEditingAndDumpCommandsUseNativeDirectories() {
    var source = new slimeknights.mantle.command.argument.RegistryTagSource<>(BuiltInRegistries.ITEM);
    assertThat(Registries.tagsDirPath(source.key())).isEqualTo("tags/item");
    assertThat(source.folder()).isEqualTo("tags/item");
  }
}
