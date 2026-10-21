package slimeknights.mantle.item.burnable;

import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;

public class BurnableSignItem extends SignItem {
  private final int burnTime;
  public BurnableSignItem(Properties propertiesIn, Block floorBlockIn, Block wallBlockIn, int burnTime) {
    super(floorBlockIn, wallBlockIn, propertiesIn);
    this.burnTime = burnTime;
  }

  @Override
  public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) { return burnTime; }
}
