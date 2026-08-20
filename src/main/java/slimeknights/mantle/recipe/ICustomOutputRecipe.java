package slimeknights.mantle.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.recipe.container.IRecipeContainer;

/**
 * Compatibility bridge for recipes with non-item outputs.
 */
public interface ICustomOutputRecipe<C extends IRecipeContainer> extends ICommonRecipe<C> {
  @Override
  @Deprecated
  default ItemStack getResultItem(RegistryAccess access) {
    return ItemStack.EMPTY;
  }

  @Override
  @Deprecated
  default ItemStack assemble(C input, RegistryAccess access) {
    return ItemStack.EMPTY;
  }
}
