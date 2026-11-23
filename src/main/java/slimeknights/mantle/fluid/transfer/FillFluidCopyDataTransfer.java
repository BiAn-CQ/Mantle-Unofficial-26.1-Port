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
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.List;

/** Fills a container while copying the full fluid component patch. */
public class FillFluidCopyDataTransfer extends FillFluidContainerTransfer {
  public static final Identifier ID = Mantle.getResource("fill_copy_data");
  public static final RecordLoadable<FillFluidCopyDataTransfer> LOADER = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", t -> t.input),
    ItemOutput.Loadable.OPTIONAL_ITEM.emptyField("result", t -> t.result),
    FluidIngredient.LOADABLE.requiredField("fluid", t -> t.fluid),
    FillFluidCopyDataTransfer::new);

  public FillFluidCopyDataTransfer(Ingredient input, ItemOutput result, FluidIngredient fluid) {
    super(input, result, fluid);
  }

  public FillFluidCopyDataTransfer(Ingredient input, ItemOutput result, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient fluid) {
    this(input, result, FluidIngredient.of(fluid));
  }

  /** @deprecated all component changes are now copied. */
  @Deprecated
  public FillFluidCopyDataTransfer(Ingredient input, ItemOutput result, FluidIngredient fluid, List<DataComponentType<?>> copy) {
    this(input, result, fluid);
  }

  /** @deprecated all component changes are now copied. */
  @Deprecated
  public FillFluidCopyDataTransfer(Ingredient input, ItemOutput result, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient fluid, List<DataComponentType<?>> copy) {
    this(input, result, fluid);
  }

  @Override
  protected ItemStack getFilled(FluidStack drained) {
    ItemStack filled = super.getFilled(drained);
    if (!filled.isEmpty()) filled.applyComponents(drained.getComponentsPatch());
    return filled;
  }

  @Override public RecordLoadable<? extends IFluidContainerTransfer> getLoader() { return LOADER; }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = new JsonObject();
    LOADER.serialize(this, json, ContextKey.currentContext());
    json.addProperty("type", ID.toString());
    return json;
  }

  private List<ItemStack> resultItems;
  @Override public List<ItemStack> resultItems() {
    if (resultItems == null) resultItems = inputFluids().stream()
      .map(this::getFilled).toList();
    return resultItems;
  }
  @Override public List<SlotLink> linkedSlots() { return List.of(SlotLink.INPUT_FLUIDS, SlotLink.RESULT_ITEMS); }
}
