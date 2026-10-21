package slimeknights.mantle.item.burnable;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;

public class BurnableTallBlockItem extends DoubleHighBlockItem {
  private final int burnTime;
  public BurnableTallBlockItem(Block blockIn, Properties builder, int burnTime) {
    super(blockIn, builder);
    this.burnTime = burnTime;
  }

  @Override
  public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) { return burnTime; }
}
