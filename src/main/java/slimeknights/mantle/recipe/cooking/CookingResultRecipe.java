package slimeknights.mantle.recipe.cooking;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import slimeknights.mantle.recipe.helper.ItemOutput;

/** Simplifies the serializers for result recipes */
public interface CookingResultRecipe {
  /** Gets the recipe result */
  ItemOutput getResult();

  static ItemStackTemplate template(ItemOutput output) {
    ItemStack stack = output.get();
    if (stack.isEmpty()) {
      throw new IllegalStateException("Cooking recipe result resolved to an empty item stack");
    }
    return ItemStackTemplate.fromNonEmptyStack(stack);
  }
}
