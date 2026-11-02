package slimeknights.mantle.plugin.jei;

import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.ingredients.IIngredientType;
import slimeknights.mantle.recipe.ingredient.EntityIngredient.EntityInput;

public class MantleJEIConstants {
  /** Ingredient for an entity */
  public static final IIngredientType<EntityInput> ENTITY_TYPE = () -> EntityInput.class;


  public static final mezz.jei.api.recipe.types.IRecipeType<slimeknights.mantle.recipe.NamedRecipe<slimeknights.mantle.fluid.transfer.IDisplayFluidTransfer>> FLUID_TRANSFER =
    mezz.jei.api.recipe.types.IRecipeType.create("mantle", "fluid_transfer", slimeknights.mantle.recipe.NamedRecipe.<slimeknights.mantle.fluid.transfer.IDisplayFluidTransfer>recipeClass());
  private static boolean transferTabEnabled;
  public static void enableTransferTab() { transferTabEnabled = true; }
  public static boolean isTransferTabEnabled() { return transferTabEnabled; }
}
