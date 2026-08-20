package slimeknights.mantle.item;

import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.level.block.Block;

public class BurnableHangingSignItem extends HangingSignItem {
  private final int burnTime;
  public BurnableHangingSignItem(Properties propertiesIn, Block hangingBlock, Block wallBlock, int burnTime) {
    super(hangingBlock, wallBlock, propertiesIn);
    this.burnTime = burnTime;
  }
}
