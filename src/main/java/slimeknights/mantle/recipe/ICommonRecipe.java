package slimeknights.mantle.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import slimeknights.mantle.recipe.container.IRecipeContainer;

/**
 * Compatibility bridge for recipes migrating from the pre-26.1 Mantle recipe API.
 */
public interface ICommonRecipe<C extends IRecipeContainer> extends Recipe<C> {
  @Override
  default ItemStack assemble(C input) {
    return getResultItem(RegistryAccess.EMPTY).copy();
  }

  default ItemStack assemble(C input, RegistryAccess access) {
    return assemble(input, (HolderLookup.Provider) access);
  }

  /**
   * Compatibility overload for integrations that still pass the pre-26.1
   * registry provider type.  The 26.1 contract is {@link RegistryAccess}.
   */
  @Deprecated
  default ItemStack assemble(C input, HolderLookup.Provider access) {
    return getResultItem(access).copy();
  }

  default ItemStack getResultItem(RegistryAccess access) {
    return getResultItem((HolderLookup.Provider) access);
  }

  /** @deprecated use {@link #getResultItem(RegistryAccess)} */
  @Deprecated
  default ItemStack getResultItem(HolderLookup.Provider access) {
    return ItemStack.EMPTY;
  }

  @Deprecated
  default boolean canCraftInDimensions(int width, int height) {
    return true;
  }

  @Override
  default boolean isSpecial() {
    return true;
  }

  @Override
  default boolean showNotification() {
    return false;
  }

  @Override
  default String group() {
    return "";
  }

  @Override
  default PlacementInfo placementInfo() {
    return PlacementInfo.NOT_PLACEABLE;
  }

  @Override
  default RecipeBookCategory recipeBookCategory() {
    return new RecipeBookCategory();
  }
}
