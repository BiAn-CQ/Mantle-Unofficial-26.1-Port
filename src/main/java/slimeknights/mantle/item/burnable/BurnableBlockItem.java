package slimeknights.mantle.item.burnable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class BurnableBlockItem extends BlockItem {
  private final int burnTime;
  public BurnableBlockItem(Block blockIn, Properties builder, int burnTime) {
    super(blockIn, builder);
    this.burnTime = burnTime;
  }

  @Override
  public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) { return burnTime; }
}
