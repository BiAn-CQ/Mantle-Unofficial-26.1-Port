package slimeknights.mantle.fluid.tooltip;

import com.google.gson.JsonParser;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FluidUnitListTest {
  @Test
  void predicateBasedUnitsRoundTripAndMatchTheSelectedFluids() {
    var json = JsonParser.parseString("{\"fluid\":{\"type\":\"mantle:set\",\"fluids\":[\"minecraft:water\"]},\"units\":[{\"key\":\"test.bucket\",\"needed\":1000}]}");
    var loaded = FluidUnitList.LOADABLE.convert(json, "units");
    assertThat(loaded.matches(Fluids.WATER)).isTrue();
    assertThat(loaded.matches(Fluids.LAVA)).isFalse();
    String encoded = FluidTooltipHandler.GSON.toJson(loaded);
    var decoded = FluidTooltipHandler.GSON.fromJson(encoded, FluidUnitList.class);
    assertThat(decoded.matches(Fluids.WATER)).isTrue();
    assertThat(decoded.matches(Fluids.LAVA)).isFalse();
    assertThat(decoded.units().getFirst().needed()).isEqualTo(1000);
  }

  @Test
  void legacyTagInputIsConvertedToAPredicate() {
    var legacy = JsonParser.parseString("{\"tag\":\"test:fluid\",\"units\":[{\"key\":\"test.bucket\",\"needed\":1000}]}");
    var loaded = FluidTooltipHandler.GSON.fromJson(legacy, FluidUnitList.class);
    var json = FluidUnitList.LOADABLE.serialize(loaded).getAsJsonObject();
    assertThat(json.has("tag")).isFalse();
    assertThat(json.getAsJsonObject("fluid").get("tag").getAsString()).isEqualTo("test:fluid");
  }
}
