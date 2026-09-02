package slimeknights.mantle.fluid.transfer;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer.IJsonSerializable;
import slimeknights.mantle.fluid.FluidTransferHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Interface for transferring fluid either to or from an item */
public interface IFluidContainerTransfer extends IJsonSerializable {
  /**
   * Adds every item an ingredient may accept without assuming it has a vanilla holder set.
   * Custom ingredients deliberately reject {@link Ingredient#getValues()}, so their own
   * representative item stream must be used instead.
   */
  static void addIngredientItems(Ingredient ingredient, Consumer<Item> consumer) {
    ICustomIngredient custom = ingredient.getCustomIngredient();
    (custom == null ? ingredient.getValues().stream() : custom.items())
      .map(Holder::value)
      .forEach(consumer);
  }

  /** Adds any items matched by this recipe for the sake of enabling transfer client side */
  void addRepresentativeItems(Consumer<Item> consumer);

  /**
   * Checks if this recipe uniquely matches the given item
   * @param stack  Stack to match
   * @param fluid  Current fluid the handler allows draining. Does not mean the handler may not accept other fluids
   *               On client side, this will always be empty. Return true if this stack
   * @return  True if this handler can transfer
   */
  boolean matches(ItemStack stack, FluidStack fluid);

  /**
   * Performs the actual transfer into or out of the handler
   * @param stack      Stack to transfer
   * @param fluid      Current fluid the handler allows draining. Does not mean the handler may not accept other fluids
   * @param handler    Handler either receiving or giving fluid
   * @param direction  Determines whether to try and fill or empty the container
   * @return  container after the transfer and the fluid transferred, null if the transfer failed
   */
  @Nullable
  TransferResult transfer(ItemStack stack, FluidStack fluid, ResourceHandler<FluidResource> handler, TransferDirection direction);

  /**
   * Result after transferring a fluid
   * @param stack    Item stack result, may be modified
   * @param fluid    Fluid, generally should not be modified
   * @param didFill  If true, the item stack was filled. If false, it was drained
   */
  record TransferResult(ItemStack stack, FluidStack fluid, boolean didFill) {
    /** Gets the sound for this result */
    public SoundEvent getSound() {
      return didFill ? FluidTransferHelper.getFillSound(fluid) : FluidTransferHelper.getEmptySound(fluid);
    }
  }

  /** Represents the direction to allow transfer */
  enum TransferDirection {
    /** Attempts to empty the item. If that fails, attempts to fill the item. */
    AUTO,
    /** Empties the item into the tank */
    EMPTY_ITEM,
    /** Fills the item from the tank */
    FILL_ITEM,
    /** Attempts to fill the item. If that fails, attempts to empty the item. */
    REVERSE;

    /** If true, may fill the item */
    public boolean canEmpty() {
      return this != FILL_ITEM;
    }

    /** If true, may empty the item */
    public boolean canFill() {
      return this != EMPTY_ITEM;
    }
  }
}
