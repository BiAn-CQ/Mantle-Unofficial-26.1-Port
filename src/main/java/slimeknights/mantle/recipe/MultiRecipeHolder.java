package slimeknights.mantle.recipe;


import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface to use on vanilla recipes, or any recipes that are stored as {@link net.minecraft.world.item.crafting.RecipeHolder} in JEI, to allow a single recipe to expose multiple to JEI.
 * To prevent the recipe itself from being exposed to JEI, it needs to return true from {@link Recipe#isSpecial()} and not be the same type as any subtype interpreter.
 * For custom recipe types, its typically better to use {@link MultiNamedRecipe} as it allows using a custom interface separate from the vanilla recipe.
 * @see MultiNamedRecipe
 */
public interface MultiRecipeHolder<T extends Recipe<?>> {
  /**
   * Gets a list of recipes for display in JEI.
   * @param id        ID of the base recipe. IDs of returned recipes should be derived from this ID while remaining unique for the sake of bookmarks.
   * @param provider  Registry access.
   * @return  List of recipes to display.
   */
  List<RecipeHolder<T>> getRecipes(Identifier id, HolderLookup.Provider provider);


  /**
   * Gets a list of all dynamic recipes for the given vanilla recipe type.
   */
  @SuppressWarnings("unchecked") // might want it later for other recipe types
  static <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getVanillaRecipes(HolderLookup.Provider provider, RecipeManager manager, RecipeType<T> type) {
    return manager.recipeMap().byType(type).stream().filter(r -> r.value().isSpecial())
      .flatMap(base -> {
        if (base.value() instanceof MultiRecipeHolder<?> multi) {
          Identifier id = base.id().identifier();
          try {
            return multi.getRecipes(id, provider).stream()
              .filter(r -> r.value().getType() == type)
              .map(r -> (RecipeHolder<T>) r);
          } catch (Exception e) {
            Mantle.logger.error("Failed to fetch JEI recipes for multi recipe {} ({})", id, multi, e);
          }
        }
        if (base.value() instanceof IMultiRecipe<?> legacy) {
          List<?> expanded = provider instanceof net.minecraft.core.RegistryAccess access ? legacy.getRecipes(access) : legacy.getRecipes(provider);
          var result = new java.util.ArrayList<RecipeHolder<T>>();
          for (int index = 0; index < expanded.size(); index++) {
            Object child = expanded.get(index);
            if (child instanceof RecipeHolder<?> holder && holder.value().getType() == type) result.add((RecipeHolder<T>)holder);
            else if (child instanceof Recipe<?> recipe && recipe.getType() == type) {
              var key = net.minecraft.resources.ResourceKey.<Recipe<?>>create(net.minecraft.core.registries.Registries.RECIPE, base.id().identifier().withSuffix("/mantle_multirecipe/" + index));
              result.add(new RecipeHolder<>(key, (T)recipe));
            }
          }
          return result.stream();
        }
        return Stream.empty();
      });
  }
}
