package slimeknights.mantle.block.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameableBlockEntityTest {
  private static NameableBlockEntity create() {
    return new NameableBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), Component.literal("Chest")) {
      @Override
      public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return null;
      }
    };
  }

  @Test
  void styledNameSurvivesSavingAndLoading() {
    var original = create();
    var name = Component.literal("Named chest").withStyle(ChatFormatting.RED).append(Component.literal("!"));
    original.setCustomName(name);
    var tag = new CompoundTag();
    original.saveSynced(tag);
    var loaded = create();
    loaded.load(tag);
    assertThat(loaded.getCustomName()).isEqualTo(name);
  }

  @Test
  void readsBothLegacyJsonAndPlainTextNames() {
    var entity = create();
    var tag = new CompoundTag();
    tag.putString("CustomName", "{\"text\":\"Old chest\",\"color\":\"red\"}");
    entity.load(tag);
    assertThat(entity.getCustomName()).isEqualTo(Component.literal("Old chest").withStyle(ChatFormatting.RED));
    tag.putString("CustomName", "Named chest");
    entity.load(tag);
    assertThat(entity.getCustomName()).isEqualTo(Component.literal("Named chest"));
  }

  @Test
  void missingNameClearsThePreviousValue() {
    var entity = create();
    entity.setCustomName(Component.literal("Old name"));
    entity.load(new CompoundTag());
    assertThat(entity.getCustomName()).isNull();
  }

  @Test
  void literalJsonTextIsNotReinterpretedAfterSaving() {
    var entity = create();
    var name = Component.literal("{\"text\":\"Literal name\"}");
    entity.setCustomName(name);
    var tag = new CompoundTag();
    entity.saveSynced(tag);
    var loaded = create();
    loaded.load(tag);
    assertThat(loaded.getCustomName()).isEqualTo(name);
  }

  @Test
  void nameTransfersThroughNativeItemComponents() {
    var entity = create();
    var name = Component.literal("Placed name").withStyle(ChatFormatting.BLUE);
    entity.applyComponents(DataComponentMap.EMPTY, DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, name).build());
    assertThat(entity.getCustomName()).isEqualTo(name);
    assertThat(entity.collectComponents().get(DataComponents.CUSTOM_NAME)).isEqualTo(name);
    entity.applyComponents(DataComponentMap.EMPTY, DataComponentPatch.EMPTY);
    assertThat(entity.getCustomName()).isNull();
  }
}
