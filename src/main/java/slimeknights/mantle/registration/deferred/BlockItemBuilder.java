package slimeknights.mantle.registration.deferred;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.item.tooltip.FixedTooltipBlockItem;
import slimeknights.mantle.item.tooltip.OptionalTooltipBlockItem;
import java.util.function.Function;

/** Shared block-item factory choices. Each item receives fresh native ID-bound properties. */
public interface BlockItemBuilder<B> {
  Function<Block,BlockItem> BLOCK_ITEM = block -> new BlockItem(block, ItemDeferredRegister.bindItemProperties(new Item.Properties()));
  Function<Block,BlockItem> OPTIONAL_TOOLTIP_ITEM = block -> new OptionalTooltipBlockItem(block, ItemDeferredRegister.bindItemProperties(new Item.Properties()));
  Function<Block,BlockItem> FIXED_TOOLTIP_ITEM = block -> FixedTooltipBlockItem.fromBlock(block, ItemDeferredRegister.bindItemProperties(new Item.Properties()));
  B blockItem(Function<? super Block,? extends BlockItem> item);
  default B defaultBlockItem() { return blockItem(BLOCK_ITEM); }
  default B optionalBlockTooltip() { return blockItem(OPTIONAL_TOOLTIP_ITEM); }
  default B fixedBlockTooltip() { return blockItem(FIXED_TOOLTIP_ITEM); }
}
