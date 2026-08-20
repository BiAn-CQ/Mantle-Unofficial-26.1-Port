package slimeknights.mantle.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/**
 * Helpers to aid in reading and writing of NBT
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TagHelper {
  /* BlockPos */

  /** @deprecated use {@link net.minecraft.nbt.NbtUtils#writeBlockPos(BlockPos)} */
  @Deprecated(forRemoval = true)
  public static CompoundTag writePos(BlockPos pos) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("X", pos.getX());
    tag.putInt("Y", pos.getY());
    tag.putInt("Z", pos.getZ());
    return tag;
  }

  /** @deprecated use {@link net.minecraft.nbt.NbtUtils#readBlockPos(CompoundTag)} */
  @Nullable
  @Deprecated(forRemoval = true)
  public static BlockPos readPos(CompoundTag tag) {
    if (tag.contains("X") && tag.contains("Y") && tag.contains("Z")) {
      return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
    }
    return null;
  }
}
