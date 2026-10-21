package slimeknights.mantle.recipe.cooking;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;

import javax.annotation.Nullable;

/** Builder for {@link SmeltingResultRecipe}, {@link BlastingResultRecipe}, {@link SmokingResultRecipe}, and {@link CampfireResultRecipe} */
@SuppressWarnings({"unchecked", "unused"})
@CanIgnoreReturnValue
public class CookingRecipeBuilder<T extends CookingRecipeBuilder<T>> extends AbstractRecipeBuilder<T> {
  protected final ItemOutput result;
  protected float experience = 1.0f;
  protected int cookingTime = 200;
  @Nullable
  protected Ingredient ingredient = null;
  protected CookingBookCategory category = CookingBookCategory.MISC;
  protected CookingType type = CookingType.SMELTING;
  protected boolean forceItemOutput;

  public T forceItemOutput() { forceItemOutput = true; return (T)this; }

  protected CookingRecipeBuilder(ItemOutput result) {
    this.result = result;
  }

  /** Creates a new builder instance */
  public static CookingRecipeBuilder<?> builder(ItemOutput result) {
    return new CookingRecipeBuilder<>(result);
  }

  /** Creates a new builder instance */
  public static CookingRecipeBuilder<?> builder(ItemLike output, int amount) {
    return builder(ItemOutput.fromItem(output, amount));
  }

  /** Creates a new builder instance */
  public static CookingRecipeBuilder<?> builder(ItemLike output) {
    return builder(output, 1);
  }

  /** Creates a new builder instance for a tag with the given size */
  public static CookingRecipeBuilder<?> builder(TagKey<Item> result, int amount) {
    return builder(ItemOutput.fromTag(result, amount));
  }

  /** Creates a new builder instance for a tag with size of 1 */
  public static CookingRecipeBuilder<?> builder(TagKey<Item> result) {
    return builder(result, 1);
  }


  /**
   * Sets the type of {@link #save(Consumer, ResourceLocation)} for the sake of {@link net.minecraftforge.common.crafting.ConditionalRecipe}.
   * Note you can also just directly use {@link #saveSmelting(Consumer, ResourceLocation)}, {@link #saveBlasting(Consumer, ResourceLocation)},
   * {@link #saveSmoking(Consumer, ResourceLocation)}, and {@link #saveCampfire(Consumer, ResourceLocation)} directly.
   */
  public T type(CookingType type) {
    this.type = type;
    return (T) this;
  }

  /** Sets the input ingredient */
  public T requires(Ingredient ingredient) {
    this.ingredient = ingredient;
    return (T) this;
  }

  /** Sets the input ingredient */
  public T requires(ItemLike item) {
    return requires(Ingredient.of(item));
  }

  /** Sets the input ingredient */
  public T requires(TagKey<Item> tag) {
    return requires(Ingredient.of(net.minecraft.core.HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag)));
  }

  private static ResourceKey<Recipe<?>> recipeKey(net.minecraft.resources.Identifier id) {
    return ResourceKey.create(Registries.RECIPE, id);
  }

  /** Sets the XP gain from this recipe */
  public T experience(float experience) {
    this.experience = experience;
    return (T) this;
  }

  /** Sets the cooking time for this recipe relative to smelting. Note its halved for {@link CookingType#BLASTING} and {@link CookingType#SMOKING} and tripled for {@link CookingType#CAMPFIRE} */
  public T cookingTime(int cookingTime) {
    this.cookingTime = cookingTime;
    return (T) this;
  }

  protected interface RecipeFactory<R extends Recipe<?>> {
    R create(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient ingredient, ItemOutput result, float experience, int cookingTime);
  }

  protected <R extends Recipe<?>> T save(RecipeOutput output, ResourceKey<Recipe<?>> id, RecipeFactory<R> factory, int cookingTime) {
    if (ingredient == null) {
      throw new IllegalStateException("Ingredient must be set");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "cooking");
    R recipe = factory.create(new Recipe.CommonInfo(true), new AbstractCookingRecipe.CookingBookInfo(category, group), ingredient, result, experience, cookingTime);
    Recipe<?> selected = recipe;
    if (!forceItemOutput && result.getTag() == null) {
      var template = net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result.get());
      var common = new Recipe.CommonInfo(true);
      var book = new AbstractCookingRecipe.CookingBookInfo(category, group);
      if (recipe instanceof SmeltingResultRecipe) { selected = new net.minecraft.world.item.crafting.SmeltingRecipe(common, book, ingredient, template, experience, cookingTime); }
      else if (recipe instanceof BlastingResultRecipe) { selected = new net.minecraft.world.item.crafting.BlastingRecipe(common, book, ingredient, template, experience, cookingTime); }
      else if (recipe instanceof SmokingResultRecipe) { selected = new net.minecraft.world.item.crafting.SmokingRecipe(common, book, ingredient, template, experience, cookingTime); }
      else if (recipe instanceof CampfireResultRecipe) { selected = new net.minecraft.world.item.crafting.CampfireCookingRecipe(common, book, ingredient, template, experience, cookingTime); }
    }
    saveRecipe(output, selected, advancement, id);
    return (T) this;
  }

  /** Saves the smelting recipe */
  public T saveSmelting(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    return save(output, id, SmeltingResultRecipe::new, cookingTime);
  }

  /** Saves the blasting recipe */
  public T saveBlasting(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    return save(output, id, BlastingResultRecipe::new, cookingTime / 2);
  }

  public T saveSmoking(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    return save(output, id, SmokingResultRecipe::new, cookingTime / 2);
  }

  /** Saves the campfire recipe */
  public T saveCampfire(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    return save(output, id, CampfireResultRecipe::new, cookingTime * 3);
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, recipeKey(Loadables.ITEM.getKey(result.get().getItem())));
  }

  @Override
  public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    switch (type) {
      case SMELTING -> saveSmelting(output, id);
      case BLASTING -> saveBlasting(output, id);
      case SMOKING -> saveSmoking(output, id);
      case CAMPFIRE -> saveCampfire(output, id);
    }
  }

  /** Helper to change the cooking type in {@link #save(Consumer, ResourceLocation)} for the sake of {@link net.minecraftforge.common.crafting.ConditionalRecipe} */
  public enum CookingType {
    SMELTING, BLASTING, SMOKING, CAMPFIRE;
    public int getTime(int time) { return this == CAMPFIRE ? time * 3 : this == SMELTING ? time : time / 2; }
    public Recipe<?> create(String group, CookingBookCategory category, Ingredient input, ItemOutput output, float experience, int time) {
      return create(group, category, input, output, experience, time, false);
    }
    public Recipe<?> create(String group, CookingBookCategory category, Ingredient input, ItemOutput output, float experience, int time, boolean force) {
      var common = new Recipe.CommonInfo(true);
      var book = new AbstractCookingRecipe.CookingBookInfo(category, group);
      int duration = getTime(time);
      if (force || output.getTag() != null) {
        return switch (this) {
          case SMELTING -> new SmeltingResultRecipe(common, book, input, output, experience, duration);
          case BLASTING -> new BlastingResultRecipe(common, book, input, output, experience, duration);
          case SMOKING -> new SmokingResultRecipe(common, book, input, output, experience, duration);
          case CAMPFIRE -> new CampfireResultRecipe(common, book, input, output, experience, duration);
        };
      }
      var result = net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(output.get());
      return switch (this) {
        case SMELTING -> new net.minecraft.world.item.crafting.SmeltingRecipe(common, book, input, result, experience, duration);
        case BLASTING -> new net.minecraft.world.item.crafting.BlastingRecipe(common, book, input, result, experience, duration);
        case SMOKING -> new net.minecraft.world.item.crafting.SmokingRecipe(common, book, input, result, experience, duration);
        case CAMPFIRE -> new net.minecraft.world.item.crafting.CampfireCookingRecipe(common, book, input, result, experience, duration);
      };
    }
  }
}
