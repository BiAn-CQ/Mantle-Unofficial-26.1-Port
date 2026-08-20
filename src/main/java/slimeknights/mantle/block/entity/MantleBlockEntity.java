package slimeknights.mantle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

public class MantleBlockEntity extends BlockEntity {

  /** Registry context used by legacy CompoundTag hooks while loading 26.1 value IO. */
  protected HolderLookup.Provider registries = RegistryAccess.EMPTY;

  public MantleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public boolean isClient() {
    return this.getLevel() != null && this.getLevel().isClientSide();
  }

  /**
   * Marks the chunk dirty without performing comparator updates (twice!!) or block state checks
   * Used since most of our markDirty calls only adjust TE data
   */
  @SuppressWarnings("deprecation")
  public void setChangedFast() {
    if (level != null) {
      if (level.hasChunkAt(worldPosition)) {
        level.getChunkAt(worldPosition).markUnsaved();
      }
    }
  }
  
  
  /* Syncing */

  /**
   * If true, this TE syncs when {@link net.minecraft.world.level.Level#blockUpdated(BlockPos, Block) is called
   * Syncs data from {@link #saveSynced(CompoundTag)}
   */
  protected boolean shouldSyncOnUpdate() {
    return false;
  }

  @Override
  @Nullable
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    // number is just used for vanilla, -1 ensures it skips all instanceof checks as its not a vanilla TE
    return shouldSyncOnUpdate() ? ClientboundBlockEntityDataPacket.create(this) : null;
  }

  /**
   * Write to NBT that is synced to the client in {@link #getUpdateTag()} and in {@link #saveAdditional(CompoundTag)}
   * @param nbt  NBT
   */
  protected void saveSynced(CompoundTag nbt) {}

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag nbt = new CompoundTag();
    saveSynced(nbt, registries);
    return nbt;
  }

  public void load(CompoundTag nbt) {}

  @Override
  @SuppressWarnings("deprecation")
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    this.registries = input.lookup();
    load(input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new));
  }

  public void saveAdditional(CompoundTag nbt) {
    saveSynced(nbt, registries());
  }

  /** Provider-aware compatibility hook for older Mantle/Tinkers block entities. */
  protected void saveSynced(CompoundTag nbt, HolderLookup.Provider registries) {
    saveSynced(nbt);
  }

  /** Current 26.1 hook retained for block entities that do not need registry context. */
  protected HolderLookup.Provider registries() {
    return level != null ? level.registryAccess() : registries;
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag nbt = new CompoundTag();
    saveAdditional(nbt);
    output.store(nbt);
  }
}
