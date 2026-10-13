package slimeknights.mantle.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This interface is intended to be used on dynamic recipes to return a full list of valid recipes.
 * For vanilla JEI categories, the source recipe must be special and must not have a category extension.
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

  /** Expands special multi-recipes for a vanilla recipe type. */
  static <I extends RecipeInput, R extends Recipe<I>, C> Stream<C> getMultiRecipes(RegistryAccess access, RecipeMap recipes, RecipeType<R> type, Class<C> clazz) {
    return recipes.byType(type).stream().map(RecipeHolder::value).filter(Recipe::isSpecial)
      .flatMap(recipe -> recipe instanceof IMultiRecipe<?> multi ? multi.getRecipes(access).stream() : Stream.empty())
      .filter(clazz::isInstance).map(clazz::cast);
  }

  /** Server-side overload; clients use the recipe map received from NeoForge. */
  static <I extends RecipeInput, R extends Recipe<I>, C> Stream<C> getMultiRecipes(RegistryAccess access, RecipeManager manager, RecipeType<R> type, Class<C> clazz) {
    return getMultiRecipes(access, manager.recipeMap(), type, clazz);
  }

  /**
   * Expands vanilla recipes into the holders used by JEI 26.1.
   * Existing holders retain their keys; raw recipes receive stable keys under their source recipe.
   */
  @SuppressWarnings("unchecked")
  static <I extends RecipeInput, R extends Recipe<I>> Stream<RecipeHolder<R>> getMultiRecipeHolders(RegistryAccess access, RecipeMap recipes, RecipeType<R> type) {
    return recipes.byType(type).stream().filter(holder -> holder.value().isSpecial()).flatMap(holder -> {
      if (!(holder.value() instanceof IMultiRecipe<?> multi)) {
        return Stream.empty();
      }
      List<?> expanded = multi.getRecipes(access);
      List<RecipeHolder<R>> result = new ArrayList<>();
      for (int index = 0; index < expanded.size(); index++) {
        Object entry = expanded.get(index);
        if (entry instanceof RecipeHolder<?> child && child.value().getType() == type) {
          result.add((RecipeHolder<R>) child);
        } else if (entry instanceof Recipe<?> child && child.getType() == type) {
          ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, holder.id().identifier().withSuffix("/mantle_multirecipe/" + index));
          result.add(new RecipeHolder<>(key, (R) child));
        }
      }
      return result.stream();
    });
  }
}
