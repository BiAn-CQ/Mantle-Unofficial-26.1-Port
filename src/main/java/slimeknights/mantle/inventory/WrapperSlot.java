package slimeknights.mantle.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Used to wrap the slots inside Modules/Subcontainers
 */
public class WrapperSlot extends Slot {

  public final Slot parent;

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
    return this.parent.getItem();
  }

  @Override
  public boolean hasItem() {
    return !this.getItem().isEmpty();
  }

  @Override
  public void setByPlayer(ItemStack stack) {
    this.parent.setByPlayer(stack);
  }

  @Override
  public void set(ItemStack stack) {
    this.parent.set(stack);
  }

  @Override
  public void setChanged() {
    this.parent.setChanged();
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
