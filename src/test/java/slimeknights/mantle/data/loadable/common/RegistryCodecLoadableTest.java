package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.mantle.data.loadable.StreamableCodec;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.mapping.MapLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistryCodecLoadableTest {
  private static final ResourceKey<Registry<String>> VALUES = ResourceKey.createRegistryKey(Identifier.parse("test:world_values"));
  private static final RegistryCodecLoadable<Holder<String>> HOLDER = new RegistryCodecLoadable<>(RegistryFixedCodec.create(VALUES), ByteBufCodecs.holderRegistry(VALUES));

  @BeforeAll
  static void bindItemComponents() {
    if (!Items.STICK.builtInRegistryHolder().areComponentsBound()) {
      Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  private record World(RegistryAccess access, Holder<String> value) {
    TypedMap context() {
      return ContextKey.lookupContext(access).build();
    }
  }

  private static World world() {
    var registry = new MappedRegistry<String>(VALUES, Lifecycle.stable());
    var value = Registry.registerForHolder(registry, ResourceKey.create(VALUES, Identifier.parse("test:entry")), "value");
    registry.freeze();
    return new World(new RegistryAccess.ImmutableRegistryAccess(List.of(registry)), value);
  }

  @Test
  void worldHolderRoundTripsWithLookupOrAccessContext() {
    World world = world();
    var json = new JsonPrimitive("test:entry");
    for (TypedMap context : List.of(world.context(), TypedMapBuilder.builder().put(ContextKey.REGISTRY_ACCESS, world.access()).build())) {
      assertThat(HOLDER.convert(json, "entry", context)).isEqualTo(world.value());
      assertThat(HOLDER.serialize(world.value(), context)).isEqualTo(json);
    }
    try (var ignored = IngredientLoadable.pushOps(RegistryOps.create(JsonOps.INSTANCE, world.access()))) {
      assertThat(HOLDER.convert(json, "entry")).isEqualTo(world.value());
      assertThat(HOLDER.serialize(world.value())).isEqualTo(json);
    }
  }

  @Test
  void requiredRegistryOpsRejectConditionOnlyContext() {
    var conditions = TypedMapBuilder.builder().put(ContextKey.CONDITION_CONTEXT,
      net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY).build();
    assertThatThrownBy(() -> ContextKey.createRegistryOps(conditions, ErrorFactory.JSON_SYNTAX_ERROR))
      .isInstanceOf(com.google.gson.JsonSyntaxException.class).hasMessageContaining("registry lookup");
    assertThat(ContextKey.createRegistryOps(world().context(), ErrorFactory.JSON_SYNTAX_ERROR))
      .isInstanceOf(RegistryOps.class);
  }

  @Test
  void defaultingRegistryRetainsItsOptionalNetworkFormat() {
    var loader = slimeknights.mantle.data.predicate.fluid.FluidPredicate.LOADER;
    var codec = new StreamableCodec<FriendlyByteBuf,slimeknights.mantle.data.predicate.IJsonPredicate<net.minecraft.world.level.material.Fluid>>(loader);
    for (var value : List.of(slimeknights.mantle.data.predicate.fluid.FluidPredicate.ANY, slimeknights.mantle.data.predicate.fluid.FluidPredicate.NONE)) {
      var old = new FriendlyByteBuf(Unpooled.buffer());
      var current = new FriendlyByteBuf(Unpooled.buffer());
      try {
        loader.encode(old, value);
        codec.encode(current, value);
        assertThat(io.netty.buffer.ByteBufUtil.getBytes(current)).containsExactly(io.netty.buffer.ByteBufUtil.getBytes(old));
        assertThat(codec.decode(current)).isSameAs(value);
        assertThat(loader.decode(old)).isSameAs(value);
      } finally {
        old.release();
        current.release();
      }
    }
  }

  private record Value(Holder<String> holder) {}
  private record Nested(List<Value> list, Map<String,Value> map, Value[] array) {}

  @Test
  void nestedRecordsCollectionsAndMappedValuesPreserveRegistryContext() {
    World world = world();
    Loadable<Value> value = HOLDER.flatXmap(Value::new, Value::holder);
    RecordLoadable<Nested> nested = RecordLoadable.create(
      value.list(1).requiredField("list", Nested::list),
      new MapLoadable<>(StringLoadable.DEFAULT, value, 1).requiredField("map", Nested::map),
      value.array(Value[]::new, false, 1).requiredField("array", Nested::array), Nested::new);
    var entry = new Value(world.value());
    var original = new Nested(List.of(entry), Map.of("key", entry), new Value[]{entry});
    var codec = new LoadableCodec<>(nested);
    var ops = RegistryOps.create(JsonOps.INSTANCE, world.access());
    JsonElement json = codec.encodeStart(ops, original).getOrThrow();
    Nested decoded = codec.parse(ops, json).getOrThrow();
    assertThat(decoded.list()).containsExactly(entry);
    assertThat(decoded.map()).containsEntry("key", entry);
    assertThat(decoded.array()).containsExactly(entry);
  }

  @Test
  void enchantedOutputRoundTripsThroughNativeAndLoadableCodecs() {
    var enchantments = new MappedRegistry<Enchantment>(Registries.ENCHANTMENT, Lifecycle.stable());
    var enchantment = Registry.registerForHolder(enchantments, ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("test:enchantment")), mock(Enchantment.class));
    enchantments.freeze();
    var registries = new ArrayList<Registry<?>>();
    BuiltInRegistries.REGISTRY.forEach(registries::add);
    registries.add(enchantments);
    var access = new RegistryAccess.ImmutableRegistryAccess(registries);
    var ops = RegistryOps.create(JsonOps.INSTANCE, access);
    var context = ContextKey.lookupContext(access).build();
    var values = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    values.set(enchantment, 2);
    var item = new ItemStack(Items.STICK);
    item.set(DataComponents.ENCHANTMENTS, values.toImmutable());
    var template = ItemStackTemplate.fromNonEmptyStack(item);
    JsonElement nativeJson = ItemStackTemplate.CODEC.encodeStart(ops, template).getOrThrow();
    ItemOutput decoded = ItemOutput.Loadable.REQUIRED_STACK.convert(nativeJson, "result", context);
    assertThat(decoded.serialize(true, context)).isEqualTo(nativeJson);
    JsonElement encoded = ItemOutput.REQUIRED_STACK_CODEC.encodeStart(ops, ItemOutput.fromStack(item)).getOrThrow();
    assertThat(encoded).isEqualTo(nativeJson);
    assertThat(ItemOutput.REQUIRED_STACK_CODEC.parse(ops, encoded).getOrThrow().get().get(DataComponents.ENCHANTMENTS)).isEqualTo(values.toImmutable());
  }

  private record ContextValue(Identifier id) implements GenericLoaderRegistry.IHaveLoader {
    private static final RecordLoadable<ContextValue> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), ContextValue::new);

    @Override
    public RecordLoadable<ContextValue> getLoader() {
      return LOADER;
    }
  }

  @Test
  void conditionalTrueBranchAndCodecSupplierRetainTheObjectContext() {
    var registry = new GenericLoaderRegistry<ContextValue>("test values", false);
    registry.register(Identifier.parse("test:value"), ContextValue.LOADER);
    var id = Identifier.parse("test:source");
    var context = TypedMapBuilder.builder().put(ContextKey.ID, id).build();
    JsonObject conditional = JsonParser.parseString("{\"type\":\"mantle:load_condition\",\"if_true\":{\"type\":\"test:value\"}}").getAsJsonObject();
    assertThat(registry.deserialize(conditional, context).id()).isEqualTo(id);
    var codec = new LoadableCodec<>(ContextValue.LOADER, () -> context);
    assertThat(codec.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow().id()).isEqualTo(id);
  }

  private interface WorldRecipe extends Recipe<RecipeInput> {
    Holder<String> value();
  }

  @Test
  void nativeRecipeSerializerPreservesWorldContextInBothDirections() {
    World world = world();
    RecordLoadable<WorldRecipe> loadable = RecordLoadable.create(HOLDER.requiredField("value", WorldRecipe::value), holder -> {
      var recipe = mock(WorldRecipe.class);
      when(recipe.value()).thenReturn(holder);
      return recipe;
    });
    var serializer = LoadableRecipeSerializer.of(loadable);
    var codec = serializer.codec().codec();
    var ops = RegistryOps.create(JsonOps.INSTANCE, world.access());
    var input = JsonParser.parseString("{\"value\":\"test:entry\"}");
    WorldRecipe recipe = codec.parse(ops, input).getOrThrow();
    assertThat(recipe.value()).isEqualTo(world.value());
    assertThat(codec.encodeStart(ops, recipe).getOrThrow()).isEqualTo(input);
  }

  private record Message(String value) {}

  @Test
  void recordNetworkFieldsReceiveTheEncodingContext() {
    Loadable<String> field = new Loadable<>() {
      @Override
      public String convert(JsonElement element, String key, TypedMap context) {
        return element.getAsString();
      }

      @Override
      public JsonElement serialize(String value) {
        return new JsonPrimitive(value);
      }

      @Override
      public String decode(FriendlyByteBuf buffer, TypedMap context) {
        return buffer.readUtf();
      }

      @Override
      public void encode(FriendlyByteBuf buffer, String value) {
        throw new UnsupportedOperationException("This field requires an encoding context");
      }

      @Override
      public void encode(FriendlyByteBuf buffer, String value, TypedMap context) {
        buffer.writeUtf(context.get(ContextKey.ID).getPath() + ':' + value);
      }
    };
    var record = RecordLoadable.create(field.requiredField("value", Message::value), Message::new);
    var context = TypedMapBuilder.builder().put(ContextKey.ID, Identifier.parse("test:source")).build();
    var codec = new StreamableCodec<FriendlyByteBuf,Message>(record, () -> context);
    var buffer = new FriendlyByteBuf(Unpooled.buffer());
    try {
      codec.encode(buffer, new Message("payload"));
      assertThat(codec.decode(buffer).value()).isEqualTo("source:payload");
    } finally {
      buffer.release();
    }
  }
}
