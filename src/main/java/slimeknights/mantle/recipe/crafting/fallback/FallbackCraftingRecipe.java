package slimeknights.mantle.recipe.crafting.fallback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

public interface FallbackCraftingRecipe {
  List<CraftingRecipe> getAlternatives(Level level);
  default boolean matchesAlternatives(CraftingInput input, Level level) {
    return getAlternatives(level).stream().noneMatch(recipe -> recipe.matches(input, level));
  }
  static List<CraftingRecipe> computeAlternatives(Level level, List<Identifier> ids) {
    if (!(level instanceof ServerLevel server)) { return List.of(); }
    var manager = server.recipeAccess();
    return ids.stream().map(id -> ResourceKey.<Recipe<?>>create(Registries.RECIPE, id)).map(manager::byKey)
      .flatMap(java.util.Optional::stream).map(RecipeHolder::value).filter(CraftingRecipe.class::isInstance).map(CraftingRecipe.class::cast).toList();
  }
}
