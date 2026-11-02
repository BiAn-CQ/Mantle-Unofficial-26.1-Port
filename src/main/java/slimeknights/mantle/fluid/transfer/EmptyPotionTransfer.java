package slimeknights.mantle.fluid.transfer;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.TagPreference;
import slimeknights.mantle.util.DataComponentHelper;

/** Fluid transfer info that empties a fluid from a potion item, but empties water if it's the water potion */
public class EmptyPotionTransfer extends EmptyFluidContainerTransfer {
  private static final PotionContents WATER = new PotionContents(Potions.WATER);
  public static final Identifier ID = Mantle.getResource("empty_potion");
  /** Unique loader instance */
  public static final RecordLoadable<EmptyPotionTransfer> DESERIALIZER = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", t -> t.input),
    ItemOutput.Loadable.OPTIONAL_ITEM.emptyField("result", t -> t.result),
    IntLoadable.FROM_ONE.requiredField("amount", t -> t.fluid.getAmount()),
    EmptyPotionTransfer::new);

  public EmptyPotionTransfer(Ingredient input, ItemOutput filled, int amount) {
    super(input, filled, FluidOutput.fromFluid(Fluids.WATER, amount));
  }

  @Override
  public boolean matches(ItemStack stack, FluidStack fluid) {
    // to match, must either have water in the stack, or a potion fluid
    return super.matches(stack, fluid)
      && (TagPreference.getPreference(net.neoforged.neoforge.common.Tags.Fluids.POTION).isPresent() || stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).equals(WATER));
  }

  @Override
  protected FluidStack getFluid(ItemStack stack) {
    // water just returns water
    if (stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).equals(WATER)) {
      return fluid.copy();
    }
    // if it's not water, we need a potion fluid to return anything
    return TagPreference.getPreference(net.neoforged.neoforge.common.Tags.Fluids.POTION)
      .map(value -> {
        FluidStack result = new FluidStack(value, fluid.getAmount());
        return DataComponentHelper.copy(result, stack, DataComponents.POTION_CONTENTS);
      })
      .orElse(FluidStack.EMPTY);
  }

  @Override public RecordLoadable<? extends IFluidContainerTransfer> getLoader() { return DESERIALIZER; }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = new JsonObject();
    json.addProperty("type", ID.toString());
    DESERIALIZER.serialize(this, json);
    return json;
  }
  /* JEI */
  private ItemStack[] inputItemsForIngredient() { return super.inputItems().toArray(ItemStack[]::new); }
  private List<ItemStack> inputItems;
  private List<FluidStack> resultFluids;

  /** Ensures all display contents are generated */
  private void ensureGenerated() {
    if (inputItems == null) {
      ItemStack[] inputs = inputItemsForIngredient();
      int listSize = BuiltInRegistries.POTION.size() * inputs.length;
      List<ItemStack> inputItems = new ArrayList<>(listSize);
      List<FluidStack> resultFluids = new ArrayList<>(listSize);

      // start by adding water - need 1 copy per input
      FluidStack resultWater = this.fluid.get();
      PotionContents waterContents = new PotionContents(Potions.WATER);
      for (ItemStack input : inputs) {
        ItemStack copy = input.copy();
        copy.set(DataComponents.POTION_CONTENTS, waterContents);
        inputItems.add(copy);
        resultFluids.add(resultWater);
      }

      // if we have a potion fluid, add all other potions
      Optional<Fluid> potionFluidOptional = TagPreference.getPreference(Tags.Fluids.POTION);
      if (potionFluidOptional.isPresent()) {
        Fluid potionFluid = potionFluidOptional.get();
        Iterator<Holder<Potion>> iterator = java.util.stream.StreamSupport.stream(BuiltInRegistries.POTION.asHolderIdMap().spliterator(), false).iterator();
        while (iterator.hasNext()) {
          Holder<Potion> potion = iterator.next();
          if (potion.equals(Potions.WATER)) {
            continue;
          }
          PotionContents contents = new PotionContents(potion);
          FluidStack resultFluid = new FluidStack(potionFluid, this.fluid.getAmount());
          resultFluid.set(DataComponents.POTION_CONTENTS, contents);
          // add each input with the potion
          for (ItemStack stack : inputs) {
            ItemStack input = stack.copy();
            input.set(DataComponents.POTION_CONTENTS, contents);
            inputItems.add(input);
            // add a copy of the fluid with each input
            resultFluids.add(resultFluid);
          }
        }
      }
      this.inputItems = List.copyOf(inputItems);
      this.resultFluids = List.copyOf(resultFluids);
    }
  }

  @Override
  public List<ItemStack> inputItems() {
    ensureGenerated();
    return inputItems;
  }

  @Override
  public List<FluidStack> resultFluids() {
    ensureGenerated();
    return resultFluids;
  }

  @Override
  public List<SlotLink> linkedSlots() {
    return List.of(SlotLink.INPUT_ITEMS, SlotLink.RESULT_FLUIDS);
  }
}
