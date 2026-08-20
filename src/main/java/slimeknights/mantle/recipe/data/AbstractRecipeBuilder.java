package slimeknights.mantle.recipe.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Common logic to create a recipe builder class
 * @param <T>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractRecipeBuilder<T extends AbstractRecipeBuilder<T>> {
  /** Advancement builder for this class */
  protected final Advancement.Builder advancementBuilder = Advancement.Builder.advancement();
  /** Group for this recipe */
  @Nonnull
  protected String group = "";

  /**
   * Adds a criteria to the recipe
   * @param name      Criteria name
   * @param criteria  Criteria instance
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T unlockedBy(String name, Criterion<?> criterion) {
    this.advancementBuilder.addCriterion(name, criterion);
    return (T) this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe group
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T group(String group) {
    this.group = group;
    return (T) this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe resource location group
   * @return  Builder
   */
  public T group(Identifier group) {
    // if minecraft, no namepsace. Groups are technically not namespaced so this is for consistency with vanilla
    if ("minecraft".equals(group.getNamespace())) {
      return group(group.getPath());
    }
    return group(group.toString());
  }

  /**
   * Builds the recipe with a default recipe ID, typically based on the output
   * @param consumerIn  Recipe consumer
   */
  public abstract void save(RecipeOutput output);

  /**
   * Builds the recipe
   * @param consumerIn  Recipe consumer
   * @param id          Recipe ID
   */
  public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    save(output, id.identifier());
  }

  /**
   * Identifier overload retained for builders shared with older data-provider
   * sources. Native 26.1 callers are bridged to a recipe resource key.
   */
  public void save(RecipeOutput output, Identifier id) {
    save(output, recipeKey(id));
  }

  private static ResourceKey<Recipe<?>> recipeKey(Identifier id) {
    return ResourceKey.create(Registries.RECIPE, id);
  }

  @Nullable
  protected AdvancementHolder buildOptionalAdvancement(ResourceKey<Recipe<?>> id, String folder) {
    if (this.advancementBuilder.criteria.build().isEmpty()) {
      return null;
    }
    return buildAdvancement(id, folder);
  }

  @Nullable
  protected AdvancementHolder buildOptionalAdvancement(Identifier id, String folder) {
    return buildOptionalAdvancement(recipeKey(id), folder);
  }

  /**
   * Builds and validates the advancement, intended to be called in {@link #save(Consumer, ResourceLocation)}
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement ID
   */
  @SuppressWarnings("removal") // Vanilla 26.1 RecipeProvider still uses the identifier parent overload.
  protected AdvancementHolder buildAdvancement(ResourceKey<Recipe<?>> id, String folder) {
    if (this.advancementBuilder.criteria.build().isEmpty()) {
      throw new IllegalStateException("No way of obtaining recipe " + id);
    }
    Identifier location = id.identifier();
    Identifier advancementId = Identifier.fromNamespaceAndPath(location.getNamespace(), "recipes/" + folder + "/" + location.getPath());
    this.advancementBuilder
        .parent(Identifier.parse("recipes/root"))
        .rewards(AdvancementRewards.Builder.recipe(id))
        .requirements(AdvancementRequirements.Strategy.OR)
    // we directly add the critera through the map as we want to replace it if already added instead of erroring
    // the rest of these setters all replace our previous recipe data
        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id));
    return this.advancementBuilder.build(advancementId);
  }

  protected AdvancementHolder buildAdvancement(Identifier id, String folder) {
    return buildAdvancement(recipeKey(id), folder);
  }

  protected <R extends Recipe<?>> void saveRecipe(RecipeOutput output, R recipe, @Nullable AdvancementHolder advancement, ResourceKey<Recipe<?>> id) {
    output.accept(id, recipe, advancement);
  }

  /** Compatibility ordering used by the original TConstruct recipe builders. */
  protected <R extends Recipe<?>> void saveRecipe(RecipeOutput output, Identifier id, R recipe,
                                                   @Nullable AdvancementHolder advancement) {
    saveRecipe(output, recipe, advancement, recipeKey(id));
  }
}
