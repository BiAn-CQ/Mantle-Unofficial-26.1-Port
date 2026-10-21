package slimeknights.mantle.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WrapperSlotTest {
  @Test
  void forwardsBothStacksToTheParentSetter() {
    Slot parent = mock(Slot.class);
    ItemStack replacement = mock(ItemStack.class);
    ItemStack previous = mock(ItemStack.class);
    new WrapperSlot(parent).setByPlayer(replacement, previous);
    verify(parent).setByPlayer(replacement, previous);
    verify(parent, never()).set(any());
  }

  @Test
  void preservesTheParentsSingleArgumentInsertOverride() {
    Slot parent = mock(Slot.class);
    ItemStack incoming = mock(ItemStack.class);
    ItemStack remainder = mock(ItemStack.class);
    when(parent.safeInsert(incoming)).thenReturn(remainder);
    assertThat(new WrapperSlot(parent).safeInsert(incoming)).isSameAs(remainder);
    verify(parent).safeInsert(incoming);
    verify(parent, never()).safeInsert(any(), anyInt());
  }

  @Test
  void preservesFakeSlotSemanticsWhenPositioned() {
    Slot parent = mock(Slot.class);
    when(parent.isFake()).thenReturn(true);
    var first = WrapperSlot.positioned(parent, 10, 20);
    var second = WrapperSlot.positioned(first, 30, 40);
    assertThat(second.parent).isSameAs(parent);
    assertThat(second.isFake()).isTrue();
  }
}
