package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** @deprecated use {@link InventoryBlockEntityRenderer} for the new render item registry. */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal") // Compatibility renderer intentionally reads the legacy block registry.
public class InventoryTileEntityRenderer<T extends BlockEntity & Container> implements BlockEntityRenderer<T,InventoryRenderState> {
  private final ItemModelResolver itemModelResolver;

  public InventoryTileEntityRenderer(BlockEntityRendererProvider.Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public InventoryRenderState createRenderState() {
    return new InventoryRenderState();
  }

  @Override
  public void extractRenderState(T inventory, InventoryRenderState renderState, float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(inventory, renderState, partialTicks, cameraPosition, breakProgress);
    // first, find the model for item display locations
    BlockState state = inventory.getBlockState();
    List<RenderItem> placements = RenderItem.REGISTRY.get(state.getBlock(), List.of());
    List<ItemStackRenderState> items = new ArrayList<>(placements.size());
    int seed = (int)inventory.getBlockPos().asLong();
    for (int slot = 0; slot < placements.size(); slot++) {
      ItemStackRenderState itemState = new ItemStackRenderState();
      if (slot < inventory.getContainerSize() && !placements.get(slot).isHidden()) {
        this.itemModelResolver.updateForTopItem(itemState, inventory.getItem(slot), placements.get(slot).getTransform(),
                                               inventory.getLevel(), null, seed + slot);
      }
      items.add(itemState);
    }
    renderState.blockState = state;
    renderState.placements = placements;
    renderState.items = items;
  }

  @Override
  public void submit(InventoryRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
      // if the block is rotatable, rotate item display
    boolean isRotated = RenderingHelper.applyRotation(matrices, state.blockState);
      // render items
    for (int slot = 0; slot < state.items.size(); slot++) {
      RenderingHelper.submitItem(matrices, submitNodeCollector, state.items.get(slot), state.placements.get(slot), state.lightCoords);
    }
      // pop back rotation
    if (isRotated) {
      matrices.popPose();
    }
  }

  @Override
  public boolean shouldRenderOffScreen() {
    return true;
  }
}
