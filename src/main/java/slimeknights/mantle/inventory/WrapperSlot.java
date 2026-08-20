package slimeknights.mantle.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

/**
 * Used to wrap the slots inside Modules/Subcontainers
 */
@SuppressWarnings("removal") // Legacy item handler API retained for TConstruct 26.1 compatibility.
public class WrapperSlot extends Slot {

  public final Slot parent;
  /** Client mirror used when a legacy slot is backed by a read-only transfer view. */
  private ItemStack mirroredStack = ItemStack.EMPTY;
  private boolean hasMirroredStack;

  public WrapperSlot(Slot slot) {
    this(slot, slot.x, slot.y);
  }

  /**
   * Wraps a slot while presenting a different client-side position.  Slot
   * coordinates became final in 26.1, so screens that move a slot while a
   * scrollbar changes page must replace the menu entry with this wrapper
   * instead of mutating the original slot.
   */
  public WrapperSlot(Slot slot, int x, int y) {
    super(slot.container, slot.getSlotIndex(), x, y);
    this.parent = slot instanceof WrapperSlot wrapper ? wrapper.rootParent() : slot;
    this.index = slot.index;
  }

  /** Gets the underlying non-positioned slot. */
  private Slot rootParent() {
    return this.parent instanceof WrapperSlot wrapper ? wrapper.rootParent() : this.parent;
  }

  /** Creates a positioned wrapper without accumulating wrapper chains. */
  public static WrapperSlot positioned(Slot slot, int x, int y) {
    return new WrapperSlot(slot, x, y);
  }

  /**
   * NeoForge 26.1's container synchronization calls {@link Slot#set(ItemStack)}
   * on every client slot.  Some legacy capability views are deliberately
   * exposed as read-only {@link net.neoforged.neoforge.items.IItemHandler}s,
   * while {@link SlotItemHandler#set(ItemStack)} still casts them to
   * {@link IItemHandlerModifiable}.  Keep the synchronized stack locally for
   * those views so a wrapper remains safe for both old and native handlers.
   */
  private boolean usesReadOnlyHandler() {
    return this.parent instanceof SlotItemHandler handler
      && !(handler.getItemHandler() instanceof IItemHandlerModifiable);
  }

  @Override
  public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    this.parent.onQuickCraft(oldStack, newStack);
  }

  @Override
  public void onTake(Player playerIn, ItemStack stack) {
    this.parent.onTake(playerIn, stack);
  }

  @Override
  public boolean mayPlace(ItemStack stack) {
    return this.parent.mayPlace(stack);
  }

  @Override
  public ItemStack getItem() {
    return this.usesReadOnlyHandler() && this.hasMirroredStack ? this.mirroredStack : this.parent.getItem();
  }

  @Override
  public boolean hasItem() {
    return !this.getItem().isEmpty();
  }

  @Override
  public void setByPlayer(ItemStack stack) {
    if (this.usesReadOnlyHandler()) {
      this.set(stack);
    } else {
      this.parent.setByPlayer(stack);
    }
  }

  @Override
  public void set(ItemStack stack) {
    if (this.usesReadOnlyHandler()) {
      this.mirroredStack = stack.copy();
      this.hasMirroredStack = true;
    } else {
      this.parent.set(stack);
    }
  }

  @Override
  public void setChanged() {
    if (!this.usesReadOnlyHandler()) {
      this.parent.setChanged();
    }
  }

  @Override
  public int getMaxStackSize() {
    return this.parent.getMaxStackSize();
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return this.parent.getMaxStackSize(stack);
  }

  @Override
  public Identifier getNoItemIcon() {
    return this.parent.getNoItemIcon();
  }

  @Override
  public ItemStack remove(int amount) {
    if (this.usesReadOnlyHandler()) {
      ItemStack current = this.getItem();
      if (current.isEmpty()) {
        return ItemStack.EMPTY;
      }
      ItemStack removed = current.split(amount);
      this.mirroredStack = current;
      return removed;
    }
    return this.parent.remove(amount);
  }

  @Override
  public boolean mayPickup(Player playerIn) {
    return this.parent.mayPickup(playerIn);
  }

  @Override
  public boolean isActive() {
    return this.parent.isActive();
  }

  @Override
  public Slot setBackground(Identifier background) {
    return this.parent.setBackground(background);
  }

  @Override
  public Optional<ItemStack> tryRemove(int pCount, int pDecrement, Player pPlayer) {
    return this.parent.tryRemove(pCount, pDecrement, pPlayer);
  }

  @Override
  public ItemStack safeTake(int pCount, int pDecrement, Player pPlayer) {
    return this.parent.safeTake(pCount, pDecrement, pPlayer);
  }

  @Override
  public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
    return this.parent.safeInsert(pStack, pIncrement);
  }

  @Override
  public boolean allowModification(Player pPlayer) {
    return this.parent.allowModification(pPlayer);
  }

  @Override
  public boolean isHighlightable() {
    return this.parent.isHighlightable();
  }
}
