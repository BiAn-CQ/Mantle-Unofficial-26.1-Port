package slimeknights.mantle.recipe.input;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
public record ItemHandlerSlotWrapper(ResourceHandler<ItemResource> parent, int index) implements SingleItemInput {
  @Override public ItemStack getItem() { return parent.getResource(index).toStack(parent.getAmountAsInt(index)); }
}
