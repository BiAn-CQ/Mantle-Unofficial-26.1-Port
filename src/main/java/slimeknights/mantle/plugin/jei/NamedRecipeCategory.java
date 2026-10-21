package slimeknights.mantle.plugin.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.recipe.NamedRecipe;

import java.util.List;

/** Recipe category containing {@link NamedRecipe}. Automatically implements recipe ID methods. */
@SuppressWarnings("unused")  // API
public abstract class NamedRecipeCategory<T> extends AbstractRecipeCategory<NamedRecipe<T>> {
  public NamedRecipeCategory(IRecipeType<NamedRecipe<T>> recipeType, Component title, IDrawable icon, int width, int height) {
    super(recipeType, title, icon, width, height);
  }

  @Override
  public Identifier getIdentifier(NamedRecipe<T> recipe) {
    return recipe.id();
  }

  public List<Identifier> getSourceRecipeIds(NamedRecipe<T> recipe) {
    return recipe.sourceIds();
  }
}
