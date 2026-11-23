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
  private boolean optional;
  private Identifier textureOverrides;

  public FluidContainerModelBuilder(Identifier parent) { this.parent = parent; }
  public FluidContainerModelBuilder fluid(Fluid fluid) { this.fluid = fluid == net.minecraft.world.level.material.Fluids.EMPTY ? null : Loadables.FLUID.getKey(fluid); return this; }
  public FluidContainerModelBuilder fluid(Identifier fluid) { this.fluid = fluid; return this; }
  public FluidContainerModelBuilder components(DataComponentPatch components) { this.components = java.util.Objects.requireNonNull(components); return this; }
  public FluidContainerModelBuilder flipGas(boolean flipGas) { this.flipGas = flipGas; return this; }
  public FluidContainerModelBuilder fluidOptional() { this.optional = true; return this; }
  public FluidContainerModelBuilder textureOverride(Identifier textureOverrides) { this.textureOverrides = textureOverrides; return this; }
  public JsonObject toJson() { return toJson(new JsonObject()); }
  public JsonObject toJson(JsonObject json) {
    return toJson(json, RegistryHelper.BUILTIN_REGISTRY_ACCESS);
  }

  /** Serializes component values using the data generator's registry lookup. */
  public JsonObject toJson(JsonObject json, HolderLookup.Provider lookup) {
    json.addProperty("type", FluidContainerModel.ID.toString());
    json.addProperty("parent", parent.toString());
    if (fluid == null) {
      if (textureOverrides != null) json.addProperty("texture_overrides", textureOverrides.toString());
    } else if (components.isEmpty() && !optional) json.addProperty("fluid", fluid.toString());
    else {
      JsonObject value = new JsonObject();
      value.addProperty("name", fluid.toString());
      if (optional) value.addProperty("optional", true);
      value.add("components", Loadables.DATA_COMPONENTS.serialize(components, ContextKey.lookupContext(lookup).build()));
      json.add("fluid", value);
    }
    json.addProperty("flip_gas", flipGas);
    return json;
  }
}
