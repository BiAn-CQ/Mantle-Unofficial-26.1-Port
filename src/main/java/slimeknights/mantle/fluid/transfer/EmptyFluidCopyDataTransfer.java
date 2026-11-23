package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;

import java.util.List;

/** Empties a container while copying the full item component patch. */
public class EmptyFluidCopyDataTransfer extends EmptyFluidContainerTransfer {
  public static final Identifier ID = Mantle.getResource("empty_copy_data");
  public static final RecordLoadable<EmptyFluidCopyDataTransfer> LOADER = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", t -> t.input),
    ItemOutput.Loadable.OPTIONAL_ITEM.emptyField("result", t -> t.result),
    FluidOutput.Loadable.REQUIRED.requiredField("fluid", t -> t.fluid),
    EmptyFluidCopyDataTransfer::new);

  public EmptyFluidCopyDataTransfer(Ingredient input, ItemOutput result, FluidOutput fluid) {
    super(input, result, fluid);
  }

  /** @deprecated all component changes are now copied. */
  @Deprecated
  public EmptyFluidCopyDataTransfer(Ingredient input, ItemOutput result, FluidOutput fluid, List<DataComponentType<?>> copy) {
    this(input, result, fluid);
  }

  @Override
  protected FluidStack getFluid(ItemStack stack) {
    FluidStack copy = fluid.copy();
    if (!copy.isEmpty()) copy.applyComponents(stack.getComponentsPatch());
    return copy;
  }

  @Override public RecordLoadable<? extends IFluidContainerTransfer> getLoader() { return LOADER; }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = new JsonObject();
    LOADER.serialize(this, json, ContextKey.currentContext());
    json.addProperty("type", ID.toString());
    return json;
  }

  private List<FluidStack> resultFluids;
  @Override public List<FluidStack> resultFluids() {
    if (resultFluids == null) resultFluids = inputItems().stream()
      .map(this::getFluid).toList();
    return resultFluids;
  }
  @Override public List<SlotLink> linkedSlots() { return List.of(SlotLink.INPUT_ITEMS, SlotLink.RESULT_FLUIDS); }
}
