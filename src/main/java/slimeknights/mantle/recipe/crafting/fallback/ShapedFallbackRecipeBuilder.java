package slimeknights.mantle.recipe.crafting.fallback;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.crafting.ShapedExtensionBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a shaped recipe with fallbacks. If any fallback matches then the base recipe will not.
 * @see ShapelessFallbackRecipe
 */
@SuppressWarnings("unused")  // API
public class ShapedFallbackRecipeBuilder extends ShapedExtensionBuilder<ShapedFallbackRecipeBuilder> {
  private final List<Identifier> alternatives = new ArrayList<>();

  protected ShapedFallbackRecipeBuilder(ItemStack result) {
    super(result);
  }

  /** Creates a builder for the given stack */
  public static ShapedFallbackRecipeBuilder shaped(ItemStack result) {
    return new ShapedFallbackRecipeBuilder(result);
  }

  /** Creates a builder for the given item and count */
  public static ShapedFallbackRecipeBuilder shaped(ItemLike result, int count) {
    return shaped(new ItemStack(result, count));
  }

  /** Creates a builder for the given item */
  public static ShapedFallbackRecipeBuilder shaped(ItemLike result) {
    return shaped(result, 1);
  }

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

  @Override
  public void save(RecipeOutput output, Identifier id) {
    output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id),
      new ShapedFallbackRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(showNotification), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(getBookCategory(), group), getPattern(), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result), alternatives),
      buildAdvancement(output, id, category.getFolderName())
    );
  }
}
