package slimeknights.mantle.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nonnull;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Standard implementation for {@link IRetexturedBlockEntity}, use alongside {@link RetexturedBlock} and {@link slimeknights.mantle.item.RetexturedBlockItem}
 */
public class DefaultRetexturedBlockEntity extends MantleBlockEntity implements IRetexturedBlockEntity {
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;
  public DefaultRetexturedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Nonnull
  @Override
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(texture);
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    if (tags.contains(TAG_TEXTURE)) {
      texture = RetexturedHelper.getBlock(tags.getStringOr(TAG_TEXTURE, ""));
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
    super.applyImplicitComponents(components);
    texture = components.getOrDefault(slimeknights.mantle.registration.MantleData.BLOCK_TEXTURE.get(), Blocks.AIR);
  }
  @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
    super.collectImplicitComponents(components);
    IRetexturedBlockEntity.collectImplicitComponents(this, components);
  }
  @Override public void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
    super.removeComponentsFromTag(output);
    output.discard(TAG_TEXTURE);
  }
}
