package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import com.mojang.datafixers.util.Either;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.StreamSupport;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
  private final ShapedRecipeBuilder parent;
  @Nullable
  private Ingredient texture = null;
  private char textureKey = '\0';
  private boolean matchAll = false;

  /**
   * Sets the texture source to the given ingredient
   * @param texture Ingredient to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(Ingredient texture) {
    this.texture = texture;
    this.textureKey = '\0';
    return this;
  }

  /**
   * Sets the texture source to the given tag
   * @param tag Tag to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(TagKey<Item> tag) {
    return setSource(Ingredient.of(StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false).map(Holder::value)));
  }

  /** Sets the texture source to a key from the texture map. Is not validated as that is too much work. */
  public ShapedRetexturedRecipeBuilder setSource(char textureKey) {
    this.textureKey = textureKey;
    this.texture = null;
    return this;
  }

  /**
   * Sets the match first property on the recipe.
   * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  public void build(RecipeOutput output) {
    build(output, parent.defaultId());
  }

  public void build(RecipeOutput output, ResourceKey<Recipe<?>> id) {
    validate();
    parent.save(new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void includeRootAdvancement() {
        output.includeRootAdvancement();
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement) {
        output.accept(key, toRecipe((ShapedRecipe) recipe), advancement);
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
        output.accept(key, toRecipe((ShapedRecipe) recipe), advancement, conditions);
      }
    }, id);
  }

  /**
   * Ensures this recipe can be built
   * @throws IllegalStateException If the recipe cannot be built
   */
  private void validate() {
    if (texture == null && textureKey == '\0') {
      throw new IllegalStateException("No texture defined for texture recipe");
    }
  }

  private ShapedRetexturedRecipe toRecipe(ShapedRecipe base) {
    Ingredient resolvedTexture;
    if (textureKey != '\0') {
      resolvedTexture = Objects.requireNonNull(parent.key.get(textureKey), "Texture ingredient references a symbol that is not defined in the shaped recipe key");
    } else {
      resolvedTexture = texture;
    }
    Either<Character,Ingredient> serializedTexture = textureKey != '\0' ? Either.left(textureKey) : Either.right(resolvedTexture);
    return ShapedRetexturedRecipe.fromJsonData(
      new Recipe.CommonInfo(base.showNotification()),
      new CraftingRecipe.CraftingBookInfo(base.category(), base.group()),
      new net.minecraft.world.item.crafting.ShapedRecipePattern.Data(parent.key, parent.rows),
      parent.result,
      serializedTexture,
      matchAll);
  }
}
