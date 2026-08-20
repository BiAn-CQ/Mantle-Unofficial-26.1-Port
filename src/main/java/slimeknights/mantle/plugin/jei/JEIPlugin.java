package slimeknights.mantle.plugin.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientHelper;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientRenderer;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

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
