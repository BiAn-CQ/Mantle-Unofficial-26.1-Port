package slimeknights.mantle.recipe.crafting;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

/** Builder for recipes that extend {@link net.minecraft.world.item.crafting.CraftingRecipe} */
@SuppressWarnings("unused") // API
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CraftingExtensionBuilder<T extends CraftingExtensionBuilder<T>> extends AbstractRecipeBuilder<T> implements RecipeBuilder {
  protected final ItemStack result;
  protected RecipeCategory category = RecipeCategory.MISC;
  protected boolean showNotification = true;
  public T showNotification(boolean show) { this.showNotification = show; return self(); }

  public Item getResult() {
    return result.getItem();
  }

  @Override public net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> defaultId() {
    return RecipeBuilder.getDefaultRecipeId(result);
  }

  /** Sets the category for this recipe. */
  public T category(RecipeCategory category) {
    this.category = category;
    return self();
  }

  /** Gets the crafting book category for this builder's category. */
  protected CraftingBookCategory getBookCategory() {
    return RecipeBuilder.determineCraftingBookCategory(category);
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, Loadables.ITEM.getKey(getResult()));
  }
}
