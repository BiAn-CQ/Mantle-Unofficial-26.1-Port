package slimeknights.mantle.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;

import java.util.List;

/**
 * This interface is intended to be used on dynamic recipes to return a full list of valid recipes.
 * @param <T>  Recipe type for the return
 */
public interface IMultiRecipe<T> {
  /**
   * Gets a list of recipes for display in JEI
   * @return  List of recipes
   * @param access  Registry access instance
   */
  default List<T> getRecipes(RegistryAccess access) {
    return getRecipes((HolderLookup.Provider) access);
  }

  /**
   * Compatibility bridge for integrations not yet switched from the old
   * provider parameter.  New code should implement the RegistryAccess form.
   */
  @Deprecated
  default List<T> getRecipes(HolderLookup.Provider access) {
    return List.of();
  }
}
