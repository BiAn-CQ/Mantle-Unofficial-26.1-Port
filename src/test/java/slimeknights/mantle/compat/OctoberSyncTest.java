package slimeknights.mantle.compat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.model.FluidContainerModel;
import slimeknights.mantle.client.model.builder.FluidContainerModelBuilder;
import slimeknights.mantle.client.render.ChannelFluids;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.fluid.MantleFluids;
import static org.assertj.core.api.Assertions.*;

class OctoberSyncTest {
  @Test void bakingAnExplicitFluidDoesNotResolveComponentsOrModelsEarly() {
    var context = org.mockito.Mockito.mock(net.minecraft.client.renderer.item.ItemModel.BakingContext.class);
    var model = new FluidContainerModel.Unbaked(Identifier.parse("neoforge:item/bucket_drip"), java.util.Map.of(),
      java.util.Optional.of(new FluidContainerModel.FluidDefinition(Identifier.parse("minecraft:water"), DataComponentPatch.EMPTY)), false);
    assertThat(model.bake(context, new org.joml.Matrix4f())).isNotNull();
    org.mockito.Mockito.verifyNoInteractions(context);
  }

  @Test void virtualRegistryHoldersRoundTripUsingTheProvidersSerializationContext() {
    ResourceKey<Registry<String>> registry = ResourceKey.createRegistryKey(Identifier.parse("test:virtual_values"));
    ResourceKey<String> value = ResourceKey.create(registry, Identifier.parse("test:entry"));
    var provider = new RegistrySetBuilder().add(registry, context -> context.register(value, "virtual value")).build(RegistryAccess.EMPTY);
    var holder = provider.lookupOrThrow(registry).getOrThrow(value);
    var ops = ContextKey.optionalRegistryOps(ContextKey.lookupContext(provider).build());
    var codec = RegistryCodecs.homogeneousList(registry);
    var encoded = codec.encodeStart(ops, HolderSet.direct(holder)).getOrThrow();
    assertThat(encoded.toString()).contains("test:entry");
    assertThat(codec.parse(ops, encoded).getOrThrow().get(0).value()).isEqualTo("virtual value");
  }

  @Test void checkedMappingUsesJsonErrorsAndPreservesSerialization() {
    var loader = StringLoadable.DEFAULT.mapChecked((value, error) -> {
      try { return Integer.parseInt(value); }
      catch (NumberFormatException failure) { throw error.create("Expected an integer"); }
    }, Loadable.noErrors(Object::toString));
    assertThat(loader.convert(JsonParser.parseString("\"42\""), "number")).isEqualTo(42);
    assertThat(loader.serialize(42).getAsString()).isEqualTo("42");
    assertThatThrownBy(() -> loader.convert(JsonParser.parseString("\"invalid\""), "number"))
      .isInstanceOf(com.google.gson.JsonSyntaxException.class).hasMessageContaining("Expected an integer");
  }

  @Test void containerBuilderDefaultsToEmptyComponentsAndDecodesAsANativeModel() {
    var json = new FluidContainerModelBuilder(Identifier.parse("neoforge:item/bucket_drip")).fluid(Fluids.WATER).toJson();
    var model = FluidContainerModel.Unbaked.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    assertThat(model.fluid()).isPresent();
    assertThat(model.fluid().orElseThrow().name()).isEqualTo(Identifier.parse("minecraft:water"));
    assertThat(model.fluid().orElseThrow().components().isEmpty()).isTrue();
  }

  @Test void explicitContainerComponentsSurviveModelEncoding() {
    var patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("custom fluid")).build();
    var json = new FluidContainerModelBuilder(Identifier.parse("minecraft:item/generated")).fluid(Fluids.WATER).components(patch).flipGas(false).toJson();
    var ops = RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
    var model = FluidContainerModel.Unbaked.MAP_CODEC.codec().parse(ops, json).getOrThrow();
    assertThat(model.fluid().orElseThrow().components()).isEqualTo(patch);
    assertThat(model.flipGas()).isFalse();
  }

  @Test void channelsAcceptIndependentOptionalVerticalSegments() {
    var shape = JsonParser.parseString("{\"from\":{\"x\":0,\"y\":0,\"z\":0},\"to\":{\"x\":16,\"y\":8,\"z\":16}}");
    var json = new com.google.gson.JsonObject();
    var center = new com.google.gson.JsonObject(); center.add("still", shape); center.add("flowing", shape);
    var side = new com.google.gson.JsonObject();
    for (String name : java.util.List.of("still", "edge", "in", "out")) side.add(name, shape);
    json.add("center", center); json.add("side", side);
    var horizontal = ChannelFluids.LOADABLE.deserialize(json);
    assertThat(horizontal.down()).isNull(); assertThat(horizontal.up()).isNull();
    json.add("up", shape);
    var upward = ChannelFluids.LOADABLE.deserialize(json);
    assertThat(upward.down()).isNull(); assertThat(upward.up()).isNotNull();
    var decoded = ChannelFluids.LOADABLE.deserialize(ChannelFluids.LOADABLE.serialize(upward).getAsJsonObject());
    assertThat(decoded.up().getTo()).isEqualTo(upward.up().getTo());
  }

  @Test void commonFluidsStayOptionalInAStandaloneMantleEnvironment() {
    for (var fluid : java.util.List.of(MantleFluids.potion, MantleFluids.powderSnow, MantleFluids.honey,
        MantleFluids.beetrootSoup, MantleFluids.mushroomStew, MantleFluids.rabbitStew)) assertThat(fluid.isBound()).isFalse();
  }
}
