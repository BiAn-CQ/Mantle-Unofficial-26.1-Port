package slimeknights.mantle.recipe.crafting.retextured;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Canonical package for the native retextured crafting recipe. */
public class ShapedRetexturedRecipe extends slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe {
  public ShapedRetexturedRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result, Ingredient texture, boolean matchAll) {
    super(common, book, pattern, result, texture, matchAll);
  }
}
