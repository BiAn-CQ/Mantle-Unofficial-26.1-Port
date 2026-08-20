package slimeknights.mantle.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.cooking.BlastingResultRecipe;
import slimeknights.mantle.recipe.cooking.CampfireResultRecipe;
import slimeknights.mantle.recipe.cooking.SmeltingResultRecipe;
import slimeknights.mantle.recipe.cooking.SmokingResultRecipe;
import slimeknights.mantle.recipe.crafting.ShapedFallbackRecipe;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

/** Handles any custom recipes added by Mantle */
public class MantleRecipes {
  private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Mantle.modId);

  private MantleRecipes() {}

  /** Registers this to the bus */
  public static void init(IEventBus bus) {
    RECIPES.register(bus);
  }

  // crafting
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedFallbackRecipe>> CRAFTING_SHAPED_FALLBACK = RECIPES.register("crafting_shaped_fallback", () -> ShapedFallbackRecipe.SERIALIZER);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRetexturedRecipe>> CRAFTING_SHAPED_RETEXTURED = RECIPES.register("crafting_shaped_retextured", () -> ShapedRetexturedRecipe.SERIALIZER);
  // cooking
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmeltingResultRecipe>> SMELTING = RECIPES.register("smelting", () -> SmeltingResultRecipe.SERIALIZER);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastingResultRecipe>> BLASTING = RECIPES.register("blasting", () -> BlastingResultRecipe.SERIALIZER);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmokingResultRecipe>> SMOKING = RECIPES.register("smoking", () -> SmokingResultRecipe.SERIALIZER);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CampfireResultRecipe>> CAMPFIRE = RECIPES.register("campfire", () -> CampfireResultRecipe.SERIALIZER);
}
