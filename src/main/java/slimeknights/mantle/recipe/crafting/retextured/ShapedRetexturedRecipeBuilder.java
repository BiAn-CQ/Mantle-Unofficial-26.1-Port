package slimeknights.mantle.recipe.crafting.retextured;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.crafting.ShapedExtensionBuilder;

/** Builder for {@link ShapedRetexturedRecipe} */
@SuppressWarnings("unused")
public class ShapedRetexturedRecipeBuilder extends ShapedExtensionBuilder<ShapedRetexturedRecipeBuilder> {
  private char textureKey = '\0';
  private boolean matchAll = false;

  protected ShapedRetexturedRecipeBuilder(ItemStack result) {
    super(result);
  }

  /** Creates a builder for the given stack */
  public static ShapedRetexturedRecipeBuilder shaped(ItemStack result) {
    return new ShapedRetexturedRecipeBuilder(result);
  }

  /** Creates a builder for the given item and count */
  public static ShapedRetexturedRecipeBuilder shaped(ItemLike result, int count) {
    return shaped(new ItemStack(result, count));
  }

  /** Creates a builder for the given item */
  public static ShapedRetexturedRecipeBuilder shaped(ItemLike result) {
    return shaped(result, 1);
  }

  /** Sets the texture source to a key from the texture map. */
  public ShapedRetexturedRecipeBuilder source(char textureKey) {
    this.textureKey = textureKey;
    return this;
  }

  /**
   * Sets the match first property on the recipe.
   * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    if (textureKey == '\0') {
      throw new IllegalStateException("No texture defined for texture recipe");
    }
    if (!this.key.containsKey(textureKey)) {
      throw new IllegalStateException("Texture references symbol '" + textureKey + "' but it's not defined in the key");
    }
    output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id),
      slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe.fromJsonData(new net.minecraft.world.item.crafting.Recipe.CommonInfo(showNotification), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(getBookCategory(), group), new net.minecraft.world.item.crafting.ShapedRecipePattern.Data(key, rows), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result), com.mojang.datafixers.util.Either.left(textureKey), matchAll),
      buildAdvancement(output, id, category.getFolderName())
    );
  }
}
