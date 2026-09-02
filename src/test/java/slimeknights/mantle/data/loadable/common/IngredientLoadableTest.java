package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngredientLoadableTest {
  @Test
  void acceptsNativeItemSyntax() {
    if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
      Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    if (!Items.DIRT.builtInRegistryHolder().areComponentsBound()) {
      Items.DIRT.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    var ingredient = IngredientLoadable.DISALLOW_EMPTY.convert(new JsonPrimitive("minecraft:stone"), "ingredient");

    assertThat(ingredient.test(new ItemStack(Items.STONE))).isTrue();
    assertThat(ingredient.test(new ItemStack(Items.DIRT))).isFalse();
  }

  @Test
  void rejectsLegacyObjectSyntax() {
    var legacy = JsonParser.parseString("{\"item\":\"minecraft:stone\"}");

    assertThatThrownBy(() -> IngredientLoadable.DISALLOW_EMPTY.convert(legacy, "ingredient"))
      .isInstanceOf(RuntimeException.class);
  }
}
