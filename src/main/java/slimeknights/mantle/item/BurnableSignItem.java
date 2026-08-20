package slimeknights.mantle.item;

import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;

public class BurnableSignItem extends SignItem {
  private final int burnTime;
  public BurnableSignItem(Properties propertiesIn, Block floorBlockIn, Block wallBlockIn, int burnTime) {
    super(floorBlockIn, wallBlockIn, propertiesIn);
    this.burnTime = burnTime;
  }
}
