package slimeknights.mantle.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.codec.LoadableMapCodec;
import slimeknights.mantle.data.loadable.*;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.registry.DatapackRegistryLoadable;
import slimeknights.mantle.data.predicate.entity.EntityPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.cooking.CookingRecipeBuilder.CookingType;
import slimeknights.mantle.recipe.cooking.SmeltingResultRecipe;
import slimeknights.mantle.recipe.data.ItemNameOutput;
import slimeknights.mantle.recipe.data.mock.MockRecipe;
import slimeknights.mantle.recipe.data.mock.MockRecipeConsumer;
import slimeknights.mantle.recipe.data.mock.RecipeModification;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.recipe.ingredient.PotionIngredient;
import slimeknights.mantle.util.CombatHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FullSyncTest {
  private static final RegistryAccess BUILT_INS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
  private static final ContextKey<Integer> OFFSET = new ContextKey<>("offset");

  @BeforeAll static void bindComponents() {
    for (var item : List.of(Items.POTION,Items.SPLASH_POTION,Items.STICK,Items.APPLE)) {
      if (!item.builtInRegistryHolder().areComponentsBound()) { item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY); }
    }
  }

  @Test void nativePotionSyntaxMatchesEveryListedPotionAndItem() {
    var json=JsonParser.parseString("{\"items\":[\"minecraft:potion\",\"minecraft:splash_potion\"],\"potion\":[\"minecraft:healing\",\"minecraft:swiftness\"]}");
    var potion=PotionIngredient.MAP_CODEC.codec().parse(BUILT_INS.createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    for (var item:List.of(Items.POTION,Items.SPLASH_POTION)) {
      for (var holder:List.of(Potions.HEALING,Potions.SWIFTNESS)) {
        var stack=new ItemStack(item);stack.set(DataComponents.POTION_CONTENTS,new PotionContents(holder));
        assertThat(potion.test(stack)).isTrue();
      }
    }
    assertThat(potion.test(new ItemStack(Items.STICK))).isFalse();
  }

  @Test void legacyPotionSyntaxStillLoads() {
    var json=JsonParser.parseString("{\"item\":\"minecraft:potion\",\"potion\":\"minecraft:healing\"}");
    var potion=PotionIngredient.MAP_CODEC.codec().parse(BUILT_INS.createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    assertThat(potion.items()).containsExactly(Items.POTION.builtInRegistryHolder());
    assertThat(potion.potion().stream()).containsExactly(Potions.HEALING);
  }

  @Test void potionNetworkRetainsMultiplePotions() {
    var original=new PotionIngredient(HolderSet.direct(Potions.HEALING,Potions.SWIFTNESS),HolderSet.direct(Items.POTION.builtInRegistryHolder()));
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),BUILT_INS);
    try {
      PotionIngredient.STREAM_CODEC.encode(buffer,original);
      var result=PotionIngredient.STREAM_CODEC.decode(buffer);
      assertThat(result.potion().stream()).containsExactly(Potions.HEALING,Potions.SWIFTNESS);
      assertThat(buffer.readableBytes()).isZero();
    } finally { buffer.release(); }
  }

  @Test void entityIngredientsAcceptNativeIdsAndRetainLegacyObjects() {
    var nativeIngredient=EntityIngredient.LOADABLE.convert(JsonParser.parseString("[\"minecraft:pig\",\"minecraft:cow\"]"),"entities");
    var legacyIngredient=EntityIngredient.LOADABLE.convert(JsonParser.parseString("{\"type\":\"minecraft:pig\"}"),"entities");
    assertThat(nativeIngredient.test(EntityType.PIG)).isTrue();
    assertThat(nativeIngredient.test(EntityType.COW)).isTrue();
    assertThat(nativeIngredient.test(EntityType.SHEEP)).isFalse();
    assertThat(legacyIngredient.test(EntityType.PIG)).isTrue();
  }

  @Test void entityIngredientNetworkRetainsHolderIdentity() {
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),BUILT_INS);
    try {
      EntityIngredient.LOADABLE.encode(buffer,EntityIngredient.of(EntityType.PIG,EntityType.COW));
      assertThat(EntityIngredient.LOADABLE.decode(buffer).getTypes()).containsExactlyInAnyOrder(EntityType.PIG,EntityType.COW);
    } finally { buffer.release(); }
  }

  @Test void dynamicRegistryLoadableUsesWorldLookup() {
    ResourceKey<Registry<String>> key=ResourceKey.createRegistryKey(Identifier.parse("test:full_sync"));
    var registry=new MappedRegistry<String>(key,Lifecycle.stable());
    var holder=Registry.registerForHolder(registry,Identifier.parse("test:entry"),"entry");registry.freeze();
    var access=new RegistryAccess.ImmutableRegistryAccess(List.of(registry));
    var loader=new DatapackRegistryLoadable<>(key);
    var context=ContextKey.lookupContext(access).build();
    assertThat(loader.convert(new JsonPrimitive("test:entry"),"value",context)).isSameAs(holder);
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),access);
    try { loader.encode(buffer,holder,context);assertThat(loader.decode(buffer,context)).isSameAs(holder); }
    finally { buffer.release(); }
  }

  @Test void canonicalBuiltinLoadablesExposeHolders() {
    assertThat(Loadables.ATTRIBUTE.convert(new JsonPrimitive("minecraft:attack_damage"),"attribute")).isEqualTo(Attributes.ATTACK_DAMAGE);
    assertThat(Loadables.ATTRIBUTE_VALUE.convert(new JsonPrimitive("minecraft:attack_damage"),"attribute")).isSameAs(Attributes.ATTACK_DAMAGE.value());
  }

  private static final Loadable<Integer> CONTEXT_VALUE = new Loadable<>() {
    @Override public Integer convert(JsonElement json,String key,TypedMap context) { return json.getAsInt()-context.getOrDefault(OFFSET,0); }
    @Override public JsonElement serialize(Integer value,TypedMap context) { return new JsonPrimitive(value+context.getOrDefault(OFFSET,0)); }
    @Override public Integer decode(FriendlyByteBuf buffer,TypedMap context) { return buffer.readVarInt()-context.getOrDefault(OFFSET,0); }
    @Override public void encode(FriendlyByteBuf buffer,Integer value,TypedMap context) { buffer.writeVarInt(value+context.getOrDefault(OFFSET,0)); }
  };
  private record NumberValue(int value) {}
  private static final RecordLoadable<NumberValue> NUMBER=RecordLoadable.create(CONTEXT_VALUE.requiredField("value",NumberValue::value),NumberValue::new);

  @Test void compositeLoadableForwardsJsonAndNetworkContext() {
    Loadable<Integer> loadable=CompositeLoadable.of(CONTEXT_VALUE,CONTEXT_VALUE,CONTEXT_VALUE);
    var context=TypedMap.builder().put(OFFSET,7).build();
    assertThat(loadable.serialize(3,context)).isEqualTo(new JsonPrimitive(10));
    assertThat(loadable.convert(new JsonPrimitive(10),"n",context)).isEqualTo(3);
    var buffer=new FriendlyByteBuf(Unpooled.buffer());
    try { loadable.encode(buffer,3,context);assertThat(loadable.decode(buffer,context)).isEqualTo(3); }
    finally { buffer.release(); }
  }

  @Test void loadableMapCodecRoundTripsNormalAndCompressedMaps() {
    var codec=new LoadableMapCodec<>(NUMBER,builder -> builder.put(OFFSET,7)).codec();
    for (var ops:List.of(JsonOps.INSTANCE,JsonOps.COMPRESSED)) {
      var encoded=codec.encodeStart(ops,new NumberValue(3)).getOrThrow();
      assertThat(codec.parse(ops,encoded).getOrThrow()).isEqualTo(new NumberValue(3));
    }
  }

  @Test void legacyRecordAdapterPreservesSerializationContext() {
    var json=new JsonObject();
    LegacyLoadable.message(NUMBER,"legacy").serializeInto(new NumberValue(3),json,TypedMap.builder().put(OFFSET,7).build());
    assertThat(json.get("value").getAsInt()).isEqualTo(10);
  }

  @Test void livingFallbackSerializesThroughTheEntityRegistry() {
    var fallback=LivingEntityPredicate.fallback(EntityPredicate.ANY);
    assertThat(LivingEntityPredicate.LOADER.serialize(fallback,TypedMap.EMPTY).isJsonNull()).isTrue();
    var entity=mock(Entity.class);
    assertThat(LivingEntityPredicate.asEntity(LivingEntityPredicate.ANY).matches(entity)).isFalse();
    assertThat(LivingEntityPredicate.asEntity(LivingEntityPredicate.ANY).matches(mock(LivingEntity.class))).isTrue();
  }

  @Test void cookingUsesVanillaUnlessItemOutputIsForced() {
    var ingredient=Ingredient.of(Items.STICK);
    var output=ItemOutput.fromItem(Items.APPLE);
    assertThat(CookingType.SMELTING.create("",CookingBookCategory.MISC,ingredient,output,1,200)).isExactlyInstanceOf(SmeltingRecipe.class);
    assertThat(CookingType.SMELTING.create("",CookingBookCategory.MISC,ingredient,output,1,200,true)).isExactlyInstanceOf(SmeltingResultRecipe.class);
    assertThat(CookingType.BLASTING.getTime(200)).isEqualTo(100);
    assertThat(CookingType.CAMPFIRE.getTime(200)).isEqualTo(600);
  }

  @Test void forcedCookingCanKeepAnUninstalledItemName() {
    var output=ItemNameOutput.fromName(Identifier.parse("absent:output"));
    assertThatCode(() -> CookingType.SMELTING.create("",CookingBookCategory.MISC,Ingredient.of(Items.STICK),output,1,200,true)).doesNotThrowAnyException();
  }

  @Test void itemNameOutputsSerializeComponentPatchesWithContext() {
    var patch=DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME,Component.literal("Optional item")).build();
    var output=ItemNameOutput.fromName(Identifier.parse("absent:output"),2,patch);
    var json=output.serialize(true,ContextKey.lookupContext(BUILT_INS).build()).getAsJsonObject();
    assertThat(json.get("id").getAsString()).isEqualTo("absent:output");
    assertThat(json.get("count").getAsInt()).isEqualTo(2);
    assertThat(json.getAsJsonObject("components").has("minecraft:custom_name")).isTrue();
    assertThat(output.getComponents()).isEqualTo(patch);
  }

  @Test void recipeModificationsTraverseNestedArrays() {
    var recipe=JsonParser.parseString("{\"children\":[{},{}]}").getAsJsonObject();
    RecipeModification.PathBuilder.of("children").index(1).set("item","absent:output").apply(recipe,new MockRecipe.Serializer(BUILT_INS));
    assertThat(recipe.getAsJsonArray("children").get(1).getAsJsonObject().get("item").getAsString()).isEqualTo("absent:output");
    assertThat(recipe.getAsJsonArray("children").get(0).getAsJsonObject().size()).isZero();
  }

  @Test void mockConsumerMergesConditionsAndForwardsAdvancements() {
    var recipes=new ArrayList<MockRecipe>();
    var advancements=new ArrayList<net.minecraft.advancements.AdvancementHolder>();
    MockRecipeConsumer consumer=new MockRecipeConsumer() {
      @Override public void accept(MockRecipe recipe) { recipes.add(recipe); }
      @Override public void advancement(net.minecraft.advancements.AdvancementHolder advancement, net.neoforged.neoforge.common.conditions.ICondition... conditions) { advancements.add(advancement); assertThat(conditions).hasSize(2); }
    };
    var first=mock(net.neoforged.neoforge.common.conditions.ICondition.class);
    var second=mock(net.neoforged.neoforge.common.conditions.ICondition.class);
    var advancement=mock(net.minecraft.advancements.AdvancementHolder.class);
    consumer.withConditions(first).accept(mock(MockRecipe.class),second);
    consumer.withConditions(first).advancement(advancement,second);
    assertThat(((MockRecipeConsumer.WithConditions)recipes.getFirst()).conditions()).containsExactly(first,second);
    assertThat(advancements).containsExactly(advancement);
  }

  @Test void namedRecipesKeepMultipleSourcesAndNativeHolderIds() {
    var id=Identifier.parse("test:display");
    var sources=List.of(Identifier.parse("test:a"),Identifier.parse("test:b"));
    assertThat(NamedRecipe.of(id,sources,"value").sourceIds()).containsExactlyElementsOf(sources);
    var recipe=mock(Recipe.class);
    var holder=new RecipeHolder<>(ResourceKey.<Recipe<?>>create(net.minecraft.core.registries.Registries.RECIPE,id),recipe);
    assertThat(NamedRecipe.of(holder).id()).isEqualTo(id);
  }
}
