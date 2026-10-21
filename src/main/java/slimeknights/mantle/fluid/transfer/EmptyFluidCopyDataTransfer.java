package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.util.DataComponentHelper;

import java.util.List;

/** Empties a container while copying only the selected item components. */
public class EmptyFluidCopyDataTransfer extends EmptyFluidContainerTransfer {
  public static final Identifier ID = Mantle.getResource("empty_copy_data");
  public static final RecordLoadable<EmptyFluidCopyDataTransfer> LOADER = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", t -> t.input),
    ItemOutput.Loadable.OPTIONAL_ITEM.emptyField("result", t -> t.result),
    FluidOutput.Loadable.REQUIRED.requiredField("fluid", t -> t.fluid),
    Loadables.DATA_COMPONENT_TYPE.list(ArrayLoadable.COMPACT).requiredField("copy", t -> t.copyComponents),
    EmptyFluidCopyDataTransfer::new);

  private final List<DataComponentType<?>> copyComponents;

  public EmptyFluidCopyDataTransfer(Ingredient input, ItemOutput result, FluidOutput fluid, List<DataComponentType<?>> copyComponents) {
    super(input, result, fluid);
    this.copyComponents = List.copyOf(copyComponents);
  }

  @Override
  protected FluidStack getFluid(ItemStack stack) {
    return DataComponentHelper.copy(super.getFluid(stack).copy(), stack, copyComponents);
  }

  @Override public RecordLoadable<? extends IFluidContainerTransfer> getLoader() { return LOADER; }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = new JsonObject();
    LOADER.serialize(this, json, ContextKey.currentContext());
    json.addProperty("type", ID.toString());
    return json;
  }
}
