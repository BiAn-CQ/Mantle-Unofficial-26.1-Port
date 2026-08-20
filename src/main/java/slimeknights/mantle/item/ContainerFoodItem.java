package slimeknights.mantle.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.function.Supplier;

/**
 * Food item with a container that is returned when the item is consumed. Supports eating stackable items with containers.
 * Technically also works for items with no container.
 */
@SuppressWarnings("unused") // API
public class ContainerFoodItem extends Item {
  private final ItemUseAnimation useAnim;
  public ContainerFoodItem(Properties props, ItemUseAnimation useAnim) {
    super(props);
    this.useAnim = useAnim;
  }

  public ContainerFoodItem(Properties props) {
    this(props, ItemUseAnimation.DRINK);
  }

  @Override
  public ItemUseAnimation getUseAnimation(ItemStack pStack) {
    return useAnim;
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    ItemStackTemplate remainder = stack.getCraftingRemainder();
    ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
    ItemStack result = super.finishUsingItem(stack, level, living);
    Player player = living instanceof Player p ? p : null;
    if (!container.isEmpty() && (player == null || !player.getAbilities().instabuild)) {
      container = container.copy();
      if (result.isEmpty()) {
        return container;
      }
      if (player != null) {
        if (!player.getInventory().add(container)) {
          player.drop(container, false);
        }
      }
    }
    return result;
  }

  /** Fluid containing variant of {@link ContainerFoodItem} */
  public static class FluidContainerFoodItem extends ContainerFoodItem {
    private final Supplier<FluidStack> fluid;
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) {
      super(props);
      this.fluid = fluid;
    }

    public ResourceHandler<FluidResource> createFluidHandler(ItemAccess access) {
      return new ConstantFluidContainerWrapper(fluid.get(), access.oneByOne());
    }
  }
}
