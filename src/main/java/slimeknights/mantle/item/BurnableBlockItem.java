package slimeknights.mantle.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class BurnableBlockItem extends BlockItem {
  private final int burnTime;
  public BurnableBlockItem(Block blockIn, Properties builder, int burnTime) {
    super(blockIn, builder);
    this.burnTime = burnTime;
  }
}
