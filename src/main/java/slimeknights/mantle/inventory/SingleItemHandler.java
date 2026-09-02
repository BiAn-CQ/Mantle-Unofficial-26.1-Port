package slimeknights.mantle.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/**
 * Item handler containing exactly one item.
 */
public abstract class SingleItemHandler<T extends MantleBlockEntity> extends ItemStacksResourceHandler implements IndexModifier<ItemResource> {
  protected final T parent;
  private final int maxStackSize;

  protected SingleItemHandler(T parent, int maxStackSize) {
    super(1);
    this.parent = parent;
    this.maxStackSize = maxStackSize;
  }

  public ItemStack getStack() {
    return stacks.getFirst();
  }

  /**
   * Sets the stack in this duct
   * @param newStack  New stack
   */
  public void setStack(ItemStack newStack) {
    set(0, ItemResource.of(newStack), newStack.getCount());
  }

  /**
   * Checks if the given stack is valid for this slot
   * @param stack  Stack
   * @return  True if valid
   */
  protected abstract boolean isItemValid(ItemStack stack);


  @Override
  public boolean isValid(int index, ItemResource resource) {
    return index == 0 && !resource.isEmpty() && isItemValid(resource.toStack());
  }

  @Override
  protected int getCapacity(int index, ItemResource resource) {
    return resource.isEmpty() ? maxStackSize : Math.min(maxStackSize, resource.getMaxStackSize());
  }

  @Override
  protected void onContentsChanged(int index, ItemStack previousContents) {
    parent.setChangedFast();
    onStackChanged(previousContents, stacks.get(index));
  }

  /** Called after the stored stack changes through either direct mutation or a transaction. */
  protected void onStackChanged(ItemStack previousStack, ItemStack newStack) {
  }

  /**
   * Writes this module to NBT
   * @return  Module in NBT
   */
  public CompoundTag writeToNBT(HolderLookup.Provider provider) {
    return (CompoundTag)ItemStack.OPTIONAL_CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stacks.getFirst())
      .getOrThrow(IllegalStateException::new);
  }

  /**
   * Reads this module from NBT
   * @param nbt  NBT
   */
  public void readFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    stacks.set(0, ItemStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt)
      .getOrThrow(IllegalStateException::new));
  }
}
