package slimeknights.mantle.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaucetFluidTest {
  @Test
  void replacingStartInvalidatesOnlyTheStartCache() {
    FluidCuboid original = FluidCuboid.builder().from(0, 0, 0).to(16, 16, 16).build();
    Vector3f originalStart = original.getFromScaled();
    Vector3f originalEnd = original.getToScaled();
    FluidCuboid changed = original.withFrom(new Vector3f(8, 4, 2));
    assertThat(changed.getFromScaled()).isEqualTo(new Vector3f(0.5f, 0.25f, 0.125f)).isNotSameAs(originalStart);
    assertThat(changed.getToScaled()).isSameAs(originalEnd);
    assertThat(original.getFromScaled()).isEqualTo(new Vector3f());
  }

  @Test
  void replacingEndInvalidatesOnlyTheEndCache() {
    FluidCuboid original = FluidCuboid.builder().from(4, 4, 4).to(16, 16, 16).build();
    Vector3f originalStart = original.getFromScaled();
    Vector3f originalEnd = original.getToScaled();
    FluidCuboid changed = original.withTo(new Vector3f(8, 12, 16));
    assertThat(changed.getToScaled()).isEqualTo(new Vector3f(0.5f, 0.75f, 1)).isNotSameAs(originalEnd);
    assertThat(changed.getFromScaled()).isSameAs(originalStart);
    assertThat(original.getToScaled()).isEqualTo(new Vector3f(1, 1, 1));
  }

  @Test
  void emptyFaucetListsRoundTripWithoutChangingOtherCuboidLists() {
    FaucetFluid parsed = FaucetFluid.LOADABLE.convert(JsonParser.parseString("{\"side\":[],\"center\":[],\"continue\":true}"), "faucet");
    assertThat(parsed.side()).isEmpty();
    assertThat(parsed.center()).isEmpty();
    assertThat(parsed.isContinued()).isTrue();
    assertThat(FaucetFluid.LOADABLE.convert(FaucetFluid.LOADABLE.serialize(parsed), "faucet")).isEqualTo(parsed);
    assertThrows(JsonSyntaxException.class, () -> FluidCuboid.LIST_LOADABLE.convert(JsonParser.parseString("[]"), "cuboids"));
  }

  @Test
  void numericBottomDisablesRenderingAtAndAboveTheBlock() {
    FluidCuboid cube = FluidCuboid.builder().build();
    FaucetFluid defaults = new FaucetFluid(List.of(cube), List.of(cube), false);
    for (int bottom : List.of(16, 20)) {
      JsonObject json = new JsonObject();
      json.addProperty("bottom", bottom);
      json.addProperty("continue", true);
      FaucetFluid fluid = FaucetFluid.fromJson(json, defaults);
      assertThat(fluid.getFluids(Direction.DOWN)).isEmpty();
      assertThat(fluid.getFluids(Direction.NORTH)).isEmpty();
      assertThat(fluid.isContinued()).isTrue();
    }
  }

  @Test
  void inheritedBottomRecomputesTheScaledBoundsAfterTheCacheWasUsed() {
    FluidCuboid cube = FluidCuboid.builder().build();
    cube.getFromScaled();
    cube.getToScaled();
    FaucetFluid defaults = new FaucetFluid(List.of(cube), List.of(cube), false);
    FaucetFluid fluid = FaucetFluid.fromJson(JsonParser.parseString("{\"bottom\":8}").getAsJsonObject(), defaults);
    assertThat(fluid.side().getFirst().getFromScaled().y).isEqualTo(0.5f);
    assertThat(fluid.center().getFirst().getFromScaled().y).isEqualTo(0.5f);
    assertThat(cube.getFromScaled().y).isZero();
  }

  @Test
  void extendedLoaderUsesItsOwnDefaultAndDeserializer() {
    Identifier defaultName = Identifier.fromNamespaceAndPath("mantle", "extended_default");
    ExtendedLoader loader = new ExtendedLoader(defaultName);
    JsonObject defaults = JsonParser.parseString("{\"side\":[],\"center\":[],\"continue\":false}").getAsJsonObject();
    RecordLoadable<FaucetFluid> deserializer = loader.load(Map.of(defaultName, new Dynamic<>(JsonOps.INSTANCE, defaults)));
    assertThat(deserializer.convert(new JsonObject(), "extended").isContinued()).isTrue();
  }

  private static class ExtendedLoader extends FaucetFluid.Loader {
    ExtendedLoader(Identifier defaultName) {
      super("Extended fluids", "mantle/model/extended_fluids", defaultName);
    }

    RecordLoadable<FaucetFluid> load(Map<Identifier, Dynamic<?>> jsons) {
      return prepareLoader(jsons);
    }

    @Override
    public FaucetFluid fromJson(JsonObject json) {
      FaucetFluid defaults = getDefaultInstance();
      return new FaucetFluid(defaults.side(), defaults.center(), true);
    }
  }
}
