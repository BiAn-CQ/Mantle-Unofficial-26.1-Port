package slimeknights.mantle.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class MultiRecipeTest {
  private static RecipeHolder<CraftingRecipe> holder(String path, CraftingRecipe recipe) {
    return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("mantle", path)), recipe);
  }

  private static CraftingRecipe recipe() {
    CraftingRecipe recipe = mock(CraftingRecipe.class);
    doReturn(RecipeType.CRAFTING).when(recipe).getType();
    return recipe;
  }

  private static CraftingRecipe multi(RegistryAccess access, boolean special, List<?> expanded) {
    CraftingRecipe source = mock(CraftingRecipe.class, withSettings().extraInterfaces(IMultiRecipe.class));
    doReturn(RecipeType.CRAFTING).when(source).getType();
    when(source.isSpecial()).thenReturn(special);
    doReturn(expanded).when((IMultiRecipe<?>) source).getRecipes(access);
    return source;
  }

  @Test
  void rawRecipesReceiveStableDistinctKeysAndExistingHoldersKeepTheirKeys() {
    RegistryAccess access = mock(RegistryAccess.class);
    CraftingRecipe first = recipe();
    CraftingRecipe second = recipe();
    RecipeHolder<CraftingRecipe> existing = holder("existing", recipe());
    RecipeMap recipes = RecipeMap.create(List.of(holder("source", multi(access, true, List.of(first, existing, second)))));
    List<RecipeHolder<CraftingRecipe>> expanded = IMultiRecipe.getMultiRecipeHolders(access, recipes, RecipeType.CRAFTING).toList();
    assertThat(expanded).hasSize(3);
    assertThat(expanded.get(0).id().identifier().getPath()).isEqualTo("source/mantle_multirecipe/0");
    assertThat(expanded.get(0).value()).isSameAs(first);
    assertThat(expanded.get(1)).isSameAs(existing);
    assertThat(expanded.get(2).id()).isNotEqualTo(expanded.get(0).id());
    assertThat(IMultiRecipe.getMultiRecipeHolders(access, recipes, RecipeType.CRAFTING).map(RecipeHolder::id).toList())
      .isEqualTo(expanded.stream().map(RecipeHolder::id).toList());
  }

  @Test
  void ordinaryRecipesAndMultiRecipesNotMarkedSpecialAreNotExpanded() {
    RegistryAccess access = mock(RegistryAccess.class);
    RecipeMap recipes = RecipeMap.create(List.of(holder("ordinary", recipe()), holder("not_special", multi(access, false, List.of(recipe())))));
    assertThat(IMultiRecipe.getMultiRecipeHolders(access, recipes, RecipeType.CRAFTING)).isEmpty();
  }

  @Test
  void entriesFromOtherCategoriesAreIgnored() {
    RegistryAccess access = mock(RegistryAccess.class);
    Recipe<?> otherCategory = mock(Recipe.class);
    doReturn(RecipeType.SMELTING).when(otherCategory).getType();
    CraftingRecipe accepted = recipe();
    RecipeMap recipes = RecipeMap.create(List.of(holder("source", multi(access, true, List.of(otherCategory, "other", accepted)))));
    assertThat(IMultiRecipe.getMultiRecipeHolders(access, recipes, RecipeType.CRAFTING).map(RecipeHolder::value))
      .containsExactly(accepted);
  }

  @Test
  void genericExpansionFiltersTheRequestedDisplayClass() {
    RegistryAccess access = mock(RegistryAccess.class);
    CraftingRecipe accepted = recipe();
    RecipeMap recipes = RecipeMap.create(List.of(holder("source", multi(access, true, List.of(accepted, "other")))));
    assertThat(IMultiRecipe.getMultiRecipes(access, recipes, RecipeType.CRAFTING, CraftingRecipe.class)).containsExactly(accepted);
  }
}
