package slimeknights.mantle.fluid.tooltip;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a list of tooltip unit types for a fluid
 */
@SuppressWarnings("ClassCanBeRecord") // needed in GSON
public class FluidUnitList {
  private static final Loadable<List<FluidUnit>> UNITS = FluidUnit.LOADABLE.list(1);
  public static final RecordLoadable<FluidUnitList> LOADABLE = new RecordLoadable<>() {
    @Override
    public FluidUnitList deserialize(JsonObject json, TypedMap context) {
      IJsonPredicate<Fluid> fluid = json.has("fluid") ? FluidPredicate.LOADER.getIfPresent(json, "fluid", context)
        : json.has("tag") ? FluidPredicate.tag(Loadables.FLUID_TAG.getIfPresent(json, "tag", context)) : FluidPredicate.NONE;
      return new FluidUnitList(fluid, UNITS.getIfPresent(json, "units", context));
    }

    @Override
    public void serialize(FluidUnitList value, JsonObject json) {
      serialize(value, json, TypedMap.EMPTY);
    }

    @Override
    public void serialize(FluidUnitList value, JsonObject json, TypedMap context) {
      if (value.fluid != FluidPredicate.NONE) {
        json.add("fluid", FluidPredicate.LOADER.serialize(value.fluid, context));
      }
      json.add("units", UNITS.serialize(value.units, context));
    }

    @Override
    public FluidUnitList decode(FriendlyByteBuf buffer, TypedMap context) {
      return new FluidUnitList(FluidPredicate.LOADER.decode(buffer, context), UNITS.decode(buffer, context));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, FluidUnitList value) {
      encode(buffer, value, TypedMap.EMPTY);
    }

    @Override
    public void encode(FriendlyByteBuf buffer, FluidUnitList value, TypedMap context) {
      FluidPredicate.LOADER.encode(buffer, value.fluid, context);
      UNITS.encode(buffer, value.units, context);
    }
  };

  private final IJsonPredicate<Fluid> fluid;
  private final List<FluidUnit> units;

  public FluidUnitList(@Nullable TagKey<Fluid> tag, List<FluidUnit> units) {
    this(tag == null ? FluidPredicate.NONE : FluidPredicate.tag(tag), units);
  }

  public FluidUnitList(IJsonPredicate<Fluid> fluid, List<FluidUnit> units) {
    this.fluid = fluid;
    this.units = List.copyOf(units);
  }

  public IJsonPredicate<Fluid> fluid() {
    return fluid;
  }

  public List<FluidUnit> units() {
    return units;
  }

  /**
   * Checks if this matches the given fluid
   */
  public boolean matches(Fluid fluid) {
    return this.fluid.matches(fluid);
  }

  /**
   * Applies the text of all child units
   */
  public int getText(List<Component> tooltip, int amount) {
    if (units != null) {
      for (FluidUnit unit : units) {
        amount = unit.getText(tooltip, amount);
      }
    }
    return amount;
  }
}
