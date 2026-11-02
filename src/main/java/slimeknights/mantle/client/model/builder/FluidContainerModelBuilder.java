package slimeknights.mantle.client.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.client.model.FluidContainerModel;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.RegistryHelper;

/** Builds the native 26.1 item-model definition for a component-bearing fluid container. */
public class FluidContainerModelBuilder {
  private final Identifier parent;
  private Identifier fluid;
  private DataComponentPatch components = DataComponentPatch.EMPTY;
  private boolean flipGas = true;

  public FluidContainerModelBuilder(Identifier parent) { this.parent = parent; }
  public FluidContainerModelBuilder fluid(Fluid fluid) { return fluid(Loadables.FLUID.getKey(fluid)); }
  public FluidContainerModelBuilder fluid(Identifier fluid) { this.fluid = fluid; return this; }
  public FluidContainerModelBuilder components(DataComponentPatch components) { this.components = java.util.Objects.requireNonNull(components); return this; }
  public FluidContainerModelBuilder flipGas(boolean flipGas) { this.flipGas = flipGas; return this; }
  public JsonObject toJson() { return toJson(new JsonObject()); }
  public JsonObject toJson(JsonObject json) {
    return toJson(json, RegistryHelper.BUILTIN_REGISTRY_ACCESS);
  }

  /** Serializes component values using the data generator's registry lookup. */
  public JsonObject toJson(JsonObject json, HolderLookup.Provider lookup) {
    if (fluid == null) throw new IllegalStateException("Must set fluid");
    json.addProperty("type", FluidContainerModel.ID.toString());
    json.addProperty("parent", parent.toString());
    if (components.isEmpty()) json.addProperty("fluid", fluid.toString());
    else {
      JsonObject value = new JsonObject();
      value.addProperty("name", fluid.toString());
      value.add("components", Loadables.DATA_COMPONENTS.serialize(components, ContextKey.lookupContext(lookup).build()));
      json.add("fluid", value);
    }
    json.addProperty("flip_gas", flipGas);
    return json;
  }
}
