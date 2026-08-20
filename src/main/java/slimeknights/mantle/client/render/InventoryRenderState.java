package slimeknights.mantle.client.render;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Extracted render-thread state for Mantle inventory block entity renderers. */
public class InventoryRenderState extends BlockEntityRenderState {
  public BlockState blockState = Blocks.AIR.defaultBlockState();
  public List<RenderItem> placements = Collections.emptyList();
  public List<ItemStackRenderState> items = Collections.emptyList();
}
