package slimeknights.mantle.recipe.crafting.fallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import java.util.List;
public class ShapedFallbackRecipe extends slimeknights.mantle.recipe.crafting.ShapedFallbackRecipe {
  public ShapedFallbackRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern pattern, ItemStackTemplate result, List<Identifier> alternatives) { super(common, book, pattern, result, alternatives); }
  public ShapedFallbackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, List<Identifier> alternatives) { this(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(category, group), pattern, ItemStackTemplate.fromNonEmptyStack(result), alternatives); }
}
