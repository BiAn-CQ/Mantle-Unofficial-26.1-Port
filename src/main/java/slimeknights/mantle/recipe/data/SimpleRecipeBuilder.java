package slimeknights.mantle.recipe.data;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;

import java.util.function.Supplier;

/**
 * Recipe builder for an existing recipe instance.
 * Primarily used with {@link SimpleRecipeSerializer#singleton(Recipe)} or
 * {@link SimpleRecipeSerializer#noArguments(Supplier)}.
 */
@RequiredArgsConstructor(staticName = "recipe")
public class SimpleRecipeBuilder extends AbstractRecipeBuilder<SimpleRecipeBuilder> {
  private final Recipe<?> recipe;
  @Accessors(fluent = true)
  @Setter
  private String folder = "simple";

  /** @deprecated simple recipes require an explicit recipe ID. */
  @Deprecated(forRemoval = true)
  @Override
  public void save(RecipeOutput output) {
    throw new UnsupportedOperationException("Cannot generate automatic recipe name");
  }

  @Override
  public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    output.accept(id, recipe, buildOptionalAdvancement(output, id.identifier(), folder));
  }
}
