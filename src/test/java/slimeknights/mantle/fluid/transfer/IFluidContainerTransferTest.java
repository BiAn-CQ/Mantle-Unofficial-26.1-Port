package slimeknights.mantle.fluid.transfer;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IFluidContainerTransferTest {
  @Test
  void representativeItemsSupportCustomIngredients() {
    Item item = mock(Item.class);
    @SuppressWarnings("unchecked")
    Holder<Item> holder = mock(Holder.class);
    ICustomIngredient custom = mock(ICustomIngredient.class);
    when(holder.value()).thenReturn(item);
    when(custom.items()).thenReturn(Stream.of(holder));

    List<Item> items = new ArrayList<>();
    IFluidContainerTransfer.addIngredientItems(new Ingredient(custom), items::add);

    assertThat(items).containsExactly(item);
  }
}
