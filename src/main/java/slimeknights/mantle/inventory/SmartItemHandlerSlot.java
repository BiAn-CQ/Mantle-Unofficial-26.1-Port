package slimeknights.mantle.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Resource handler slot that also respects the item's own stack limit. */
public class SmartItemHandlerSlot extends ResourceHandlerSlot {
	public SmartItemHandlerSlot(ResourceHandler<ItemResource> itemHandler, int index, int xPosition, int yPosition) {
		super(itemHandler, modifier(itemHandler), index, xPosition, yPosition);
	}

	@SuppressWarnings("unchecked")
	private static IndexModifier<ItemResource> modifier(ResourceHandler<ItemResource> handler) {
		if (handler instanceof IndexModifier<?> modifier) {
			return (IndexModifier<ItemResource>) modifier;
		}
		return (slot, resource, amount) -> replace(handler, slot, resource, amount);
	}

	private static void replace(ResourceHandler<ItemResource> handler, int index, ItemResource resource, int amount) {
		try (Transaction transaction = Transaction.openRoot()) {
			ItemResource current = handler.getResource(index);
			int currentAmount = handler.getAmountAsInt(index);
			if (currentAmount > 0 && handler.extract(index, current, currentAmount, transaction) != currentAmount) {
				throw new IllegalStateException("Unable to replace item resource at index " + index);
			}
			if (amount > 0 && handler.insert(index, resource, amount, transaction) != amount) {
				throw new IllegalStateException("Unable to set item resource at index " + index);
			}
			transaction.commit();
		}
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return getItem().isEmpty() || super.mayPickup(playerIn);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return Math.min(stack.getMaxStackSize(), getResourceHandler().getCapacityAsInt(getSlotIndex(), ItemResource.of(stack)));
	}
}
