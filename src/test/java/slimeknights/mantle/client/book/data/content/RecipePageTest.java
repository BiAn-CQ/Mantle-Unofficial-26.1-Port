package slimeknights.mantle.client.book.data.content;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.ClientEvents;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class RecipePageTest {
  private static final Recipe.CommonInfo COMMON = new Recipe.CommonInfo(true);
  private static final CraftingRecipe.CraftingBookInfo BOOK = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");

  @BeforeAll
  static void bindItemComponents() {
    if (!Items.STICK.builtInRegistryHolder().areComponentsBound()) {
      Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  private static RecipeHolder<?> holder(String name, Recipe<?> recipe) {
    return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse(name)), recipe);
  }

  @Test
  void recipePagesUseTheRemoteClientsSyncedRecipes() {
    var result = new ItemStackTemplate(Items.STICK, 1, DataComponentPatch.EMPTY);
    var crafting = new ShapedRecipe(COMMON, BOOK, ShapedRecipePattern.of(Map.of('S', Ingredient.of(Items.STICK)), List.of("S")), result);
    var cooking = new SmeltingRecipe(COMMON, new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""),
      Ingredient.of(Items.STICK), result, 0, 200);
    var recipes = RecipeMap.create(List.of(holder("test:crafting", crafting), holder("test:cooking", cooking)));
    var minecraft = mock(Minecraft.class);
    try (var singleton = mockStatic(Minecraft.class); var synced = mockStatic(ClientEvents.class)) {
      singleton.when(Minecraft::getInstance).thenReturn(minecraft);
      synced.when(ClientEvents::getSyncedRecipes).thenReturn(recipes);
      var craftingPage = new ContentCrafting();
      craftingPage.recipe = "test:crafting";
      craftingPage.load();
      assertThat(craftingPage.grid).hasDimensions(1, 1);
      assertThat(craftingPage.result.getItems()).hasSize(1);
      var cookingPage = new ContentSmelting();
      cookingPage.recipe = "test:cooking";
      cookingPage.load();
      assertThat(cookingPage.input.getItems()).hasSize(1);
      assertThat(cookingPage.result.getItems()).hasSize(1);
      assertThat(cookingPage.cookTime).isEqualTo(200);
    }
  }

  @Test
  void anUnknownRecipeLeavesManualPageDataAvailable() {
    var minecraft = mock(Minecraft.class);
    try (var singleton = mockStatic(Minecraft.class); var synced = mockStatic(ClientEvents.class)) {
      singleton.when(Minecraft::getInstance).thenReturn(minecraft);
      synced.when(ClientEvents::getSyncedRecipes).thenReturn(RecipeMap.EMPTY);
      var crafting = new ContentCrafting();
      crafting.recipe = "test:missing";
      var cooking = new ContentSmelting();
      cooking.recipe = "test:missing";
      assertThatCode(crafting::load).doesNotThrowAnyException();
      assertThatCode(cooking::load).doesNotThrowAnyException();
    }
  }

  @Test
  void shapelessRecipesUseARowAndColumnGrid() {
    var result = new ItemStackTemplate(Items.STICK, 1, DataComponentPatch.EMPTY);
    var recipe = new ShapelessRecipe(COMMON, BOOK, result, java.util.Collections.nCopies(5, Ingredient.of(Items.STICK)));
    var minecraft = mock(Minecraft.class);
    try (var singleton = mockStatic(Minecraft.class); var synced = mockStatic(ClientEvents.class)) {
      singleton.when(Minecraft::getInstance).thenReturn(minecraft);
      synced.when(ClientEvents::getSyncedRecipes).thenReturn(RecipeMap.create(List.of(holder("test:shapeless", recipe))));
      var page = new ContentCrafting();
      page.recipe = "test:shapeless";
      page.load();
      assertThat(page.grid_size).isEqualTo("large");
      assertThat(page.grid).hasDimensions(3, 3);
      assertThat(page.grid[1][1]).isNotNull();
      assertThat(page.grid[1][2]).isNull();
    }
  }
}
