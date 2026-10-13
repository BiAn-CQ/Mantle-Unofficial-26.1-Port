package slimeknights.mantle.plugin.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.ClientEvents;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientHelper;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientRenderer;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  private static final Codec<EntityIngredient.EntityInput> ENTITY_CODEC =
    BuiltInRegistries.ENTITY_TYPE.byNameCodec().xmap(EntityIngredient.EntityInput::new, EntityIngredient.EntityInput::type);

  @Override
  public Identifier getPluginUid() {
    return Mantle.getResource("jei");
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    registration.register(MantleJEIConstants.ENTITY_TYPE, Collections.emptyList(), new EntityIngredientHelper(), new EntityIngredientRenderer(16), ENTITY_CODEC);
  }

  private static <I extends RecipeInput, R extends Recipe<I>> void addMultiRecipes(IRecipeRegistration registration, RegistryAccess access,
                                                                                 RecipeMap recipes, RecipeType<R> type, IRecipeHolderType<R> jeiType) {
    List<RecipeHolder<R>> expanded = IMultiRecipe.getMultiRecipeHolders(access, recipes, type).toList();
    if (!expanded.isEmpty()) {
      registration.addRecipes(jeiType, expanded);
    }
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }
    RegistryAccess access = minecraft.level.registryAccess();
    RecipeMap recipes = ClientEvents.getSyncedRecipes();
    addMultiRecipes(registration, access, recipes, RecipeType.CRAFTING, RecipeTypes.CRAFTING);
    addMultiRecipes(registration, access, recipes, RecipeType.SMELTING, RecipeTypes.SMELTING);
    addMultiRecipes(registration, access, recipes, RecipeType.SMOKING, RecipeTypes.SMOKING);
    addMultiRecipes(registration, access, recipes, RecipeType.BLASTING, RecipeTypes.BLASTING);
    addMultiRecipes(registration, access, recipes, RecipeType.CAMPFIRE_COOKING, RecipeTypes.CAMPFIRE_COOKING);
    addMultiRecipes(registration, access, recipes, RecipeType.STONECUTTING, RecipeTypes.STONECUTTING);
    addMultiRecipes(registration, access, recipes, RecipeType.SMITHING, RecipeTypes.SMITHING);
  }

  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    registration.getCraftingCategory().addExtension(ShapedRetexturedRecipe.class, RetexturableRecipeExtension.INSTANCE);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGuiContainerHandler(MultiModuleScreen.class, new MultiModuleContainerHandler());
  }

  private static class MultiModuleContainerHandler<C extends MultiModuleContainerMenu<?>> implements IGuiContainerHandler<MultiModuleScreen<C>> {
    @Override
    public List<Rect2i> getGuiExtraAreas(MultiModuleScreen<C> guiContainer) {
      return guiContainer.getModuleAreas();
    }
  }
}
