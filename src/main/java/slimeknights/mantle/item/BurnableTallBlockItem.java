package slimeknights.mantle.item;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;

public class BurnableTallBlockItem extends DoubleHighBlockItem {
  private final int burnTime;
  public BurnableTallBlockItem(Block blockIn, Properties builder, int burnTime) {
    super(blockIn, builder);
    this.burnTime = burnTime;
  }
}
