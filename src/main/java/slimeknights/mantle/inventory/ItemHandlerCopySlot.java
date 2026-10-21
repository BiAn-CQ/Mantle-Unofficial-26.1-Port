package slimeknights.mantle.inventory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
/** Native resource-handler slot with copy-safe stack replacement and empty-slot pickup. */
public class ItemHandlerCopySlot extends SmartItemHandlerSlot {
  public ItemHandlerCopySlot(ResourceHandler<ItemResource> handler, int index, int x, int y) { super(handler, index, x, y); }
}
