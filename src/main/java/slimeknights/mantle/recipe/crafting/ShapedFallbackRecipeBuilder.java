package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builder for a shaped recipe with fallbacks */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fallback")
public class ShapedFallbackRecipeBuilder {
  private final ShapedRecipeBuilder base;
  private final List<Identifier> alternatives = new ArrayList<>();

  /**
   * Adds a single alternative to this recipe. Any matching alternative causes this recipe to fail
   * @param location  Alternative
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternative(Identifier location) {
    this.alternatives.add(location);
    return this;
  }

  /**
   * Adds a list of alternatives to this recipe. Any matching alternative causes this recipe to fail
   * @param locations  Alternative list
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternatives(Collection<Identifier> locations) {
    this.alternatives.addAll(locations);
    return this;
  }

  public void build(RecipeOutput output) {
    build(output, base.defaultId());
  }

  public void build(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    base.save(new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void includeRootAdvancement() {
        output.includeRootAdvancement();
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement) {
        ShapedRecipe shaped = (ShapedRecipe) recipe;
        ShapedFallbackRecipe fallback = new ShapedFallbackRecipe(
          new Recipe.CommonInfo(shaped.showNotification()),
          new CraftingRecipe.CraftingBookInfo(shaped.category(), shaped.group()),
          shaped.pattern,
          base.result,
          alternatives);
        output.accept(key, fallback, advancement);
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
        ShapedRecipe shaped = (ShapedRecipe) recipe;
        ShapedFallbackRecipe fallback = new ShapedFallbackRecipe(
          new Recipe.CommonInfo(shaped.showNotification()),
          new CraftingRecipe.CraftingBookInfo(shaped.category(), shaped.group()),
          shaped.pattern,
          base.result,
          alternatives);
        output.accept(key, fallback, advancement, conditions);
      }
    }, id);
  }
}
