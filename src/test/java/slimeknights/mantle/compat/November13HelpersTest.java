package slimeknights.mantle.compat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.model.FluidContainerModel;
import slimeknights.mantle.client.model.builder.FluidContainerModelBuilder;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidModel;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.mantle.util.html.HtmlString;
import static org.assertj.core.api.Assertions.*;

class November13HelpersTest {
  @Test void stringLengthBoundsApplyToParsingAndSerialization() {
    var loader = StringLoadable.length(2, 4);
    assertThat(loader.parseString("ab", "name")).isEqualTo("ab");
    assertThat(loader.getString("abcd")).isEqualTo("abcd");
    assertThatThrownBy(() -> loader.parseString("a", "name")).hasMessageContaining("shorter than 2");
    assertThatThrownBy(() -> loader.getString("abcde")).hasMessageContaining("longer than 4");
    assertThatThrownBy(() -> StringLoadable.NON_EMPTY.parseString("", "name")).hasMessageContaining("empty string");
    assertThat(StringLoadable.DEFAULT.parseString("", "name")).isEmpty();
  }

  @Test void htmlEscapesAccentsDegreeSignsAndReservedCharacters() {
    assertThat(HtmlString.escapeString("é 30° <&>\"")).isEqualTo("&eacute; 30&deg; &lt;&amp;&gt;&quot;");
  }

  private enum Kind { ONE }
  private static class ExtendedEnum extends EnumObject<Kind,String> {
    ExtendedEnum(EnumObject<Kind,String> source) { super(source); }
  }
  @Test void enumExtensionKeepsUnresolvedSuppliers() {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var source = new EnumObject.Builder<Kind,String>(Kind.class).put(Kind.ONE, () -> { calls.incrementAndGet(); return "one"; }).build();
    var copy = new ExtendedEnum(source);
    assertThat(calls.get()).isZero();
    assertThat(copy.getSupplier(Kind.ONE)).isSameAs(source.getSupplier(Kind.ONE));
    assertThat(copy.get(Kind.ONE)).isEqualTo("one");
  }

  @Test void registryHolderSetRetainsActualHoldersAndOrdering() {
    var set = RegistryHelper.holderSet(BuiltInRegistries.ITEM, Items.STONE, Items.DIRT);
    assertThat(set.get(0)).isSameAs(Items.STONE.builtInRegistryHolder());
    assertThat(set.get(1)).isSameAs(Items.DIRT.builtInRegistryHolder());
  }

  @Test void optionalAbsentFluidSkipsAbsentComponentTypes() {
    var json = JsonParser.parseString("{\"name\":\"missing:fluid\",\"optional\":true,\"components\":{\"missing:component\":1}}");
    var fluid = FluidContainerModel.FluidDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    assertThat(fluid.optional()).isTrue();
    assertThat(fluid.components().isEmpty()).isTrue();
  }

  @Test void requiredAbsentFluidIsReported() {
    assertThat(FluidContainerModel.FluidDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("\"missing:fluid\"")).error()).isPresent();
  }

  @Test void builderAndLoaderAgreeOnTextureOverrideField() {
    var json = new FluidContainerModelBuilder(Identifier.parse("minecraft:item/generated"))
      .textureOverride(Identifier.parse("example:buckets")).toJson();
    var definition = FluidContainerModel.Unbaked.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    assertThat(definition.fluid()).isEmpty();
    assertThat(definition.textureOverrides()).contains(Identifier.parse("example:buckets"));
  }

  @Test void optionalBucketBuilderRetainsTheOptionalFlag() {
    var json = new FluidContainerModelBuilder(Identifier.parse("minecraft:item/generated"))
      .fluid(Identifier.parse("missing:fluid")).fluidOptional().toJson();
    var definition = FluidContainerModel.Unbaked.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    assertThat(definition.fluid().orElseThrow().optional()).isTrue();
  }

  @Test void fluidModelBoundsComeFromItsGeometryAndIgnoreLaterListChanges() {
    var cubes = new ArrayList<FluidCuboid>();
    cubes.add(FluidCuboid.builder().from(2, 3, 4).to(8, 7, 10).build());
    cubes.add(FluidCuboid.builder().from(4, 10, 6).to(12, 14, 11).build());
    var model = FluidModel.of(cubes);
    cubes.clear();
    assertThat(model.getFluids()).hasSize(2);
    assertThat(model.getMin().y()).isEqualTo(3f);
    assertThat(model.getMax().y()).isEqualTo(14f);
    assertThat(model.getMinScaled().y()).isEqualTo(3 / 16f);
    assertThat(model.getMaxScaled().y()).isEqualTo(14 / 16f);
    assertThat(FluidModel.of(List.of())).isSameAs(FluidModel.EMPTY);
  }
  @Test void explicitFluidComponentsKeepTheCallersRegistryContext() {
    var ops = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
    var patch = net.minecraft.core.component.DataComponentPatch.builder().set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
      new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.HEALING)).build();
    var original = new FluidContainerModel.FluidDefinition(Identifier.parse("minecraft:water"), patch);
    var json = FluidContainerModel.FluidDefinition.CODEC.encodeStart(ops, original).getOrThrow();
    var copy = FluidContainerModel.FluidDefinition.CODEC.parse(ops, json).getOrThrow();
    assertThat(copy.components()).isEqualTo(patch);
  }

  @org.junit.jupiter.api.io.TempDir java.nio.file.Path dataOutput;

  @Test void lootInjectionDatagenUsesRegistryOpsAndNativeConditions() throws Exception {
    var lookup = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var provider = new slimeknights.mantle.loot.AbstractLootTableInjectionProvider(new net.minecraft.data.PackOutput(dataOutput), "test", java.util.concurrent.CompletableFuture.completedFuture(lookup)) {
      @Override public String getName() { return "loot injector regression"; }
      @Override protected void addTables(net.minecraft.core.HolderLookup.Provider registries) {
        inject("example", "minecraft:chests/simple_dungeon", net.neoforged.neoforge.common.conditions.NeverCondition.INSTANCE)
          .addToPool("main", net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(Items.STONE).build());
      }
    };
    provider.run(net.minecraft.data.CachedOutput.NO_CACHE).join();
    var json = JsonParser.parseString(java.nio.file.Files.readString(dataOutput.resolve("data/test/mantle/loot_injectors/example.json"))).getAsJsonObject();
    assertThat(json.has("neoforge:conditions")).isTrue();
    assertThat(json.has("conditions")).isFalse();
    var copy = slimeknights.mantle.loot.LootTableInjection.CODEC.parse(lookup.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
    assertThat(copy.pools()).hasSize(1);
  }

}
