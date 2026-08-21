package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SizedIngredientTest {
  @Test
  void tagIngredientCanBeEnumeratedBeforeTagsAreBound() {
    Holder.Reference<Item> barrier = Items.BARRIER.builtInRegistryHolder();
    if (!barrier.areComponentsBound()) {
      barrier.bindComponents(DataComponentMap.EMPTY);
    }
    TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mantle", "unbound_test"));
    SizedIngredient ingredient = SizedIngredient.fromTag(tag, 2);

    assertThat(ingredient.getIngredient().isCustom()).isTrue();
    assertThatCode(ingredient::getMatchingStacks).doesNotThrowAnyException();
    assertThat(ingredient.getMatchingStacks()).isNotEmpty();
    assertThat(ingredient.getMatchingStacks()).allMatch(stack -> stack.getCount() == 2);
  }
}
