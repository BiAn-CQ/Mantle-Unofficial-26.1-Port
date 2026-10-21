package slimeknights.mantle.recipe.crafting.fallback;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;
import java.util.List;

/** A shapeless recipe that yields to any matching alternative recipe. */
public class ShapelessFallbackRecipe extends ShapelessRecipe implements FallbackCraftingRecipe {
  public static final MapCodec<ShapelessFallbackRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
    slimeknights.mantle.data.codec.MantleCodecs.SHAPELESS_INGREDIENTS.fieldOf("ingredients").forGetter(r -> r.ingredients),
    Identifier.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("alternatives").forGetter(r -> r.alternatives)
  ).apply(i, ShapelessFallbackRecipe::new));
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapelessFallbackRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
    ItemStackTemplate.STREAM_CODEC, r -> r.result,
    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.alternatives, ShapelessFallbackRecipe::new);
  public static final RecipeSerializer<ShapelessFallbackRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
  private final ItemStackTemplate result;
  private final List<Ingredient> ingredients;
  private final List<Identifier> alternatives;
  private RecipeManager cachedManager;
  private List<CraftingRecipe> alternativeCache = List.of();
  public ShapelessFallbackRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ItemStackTemplate result, List<Ingredient> ingredients, List<Identifier> alternatives) {
    super(common, book, result, ingredients);
    this.result = result;
    this.ingredients = List.copyOf(ingredients);
    this.alternatives = List.copyOf(alternatives);
  }
  public ShapelessFallbackRecipe(String group, CraftingBookCategory category, ItemStack result, List<Ingredient> ingredients, List<Identifier> alternatives) {
    this(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(category, group), ItemStackTemplate.fromNonEmptyStack(result), ingredients, alternatives);
  }
  @Override public List<CraftingRecipe> getAlternatives(Level level) {
    if (level instanceof ServerLevel server && server.recipeAccess() != cachedManager) {
      cachedManager = server.recipeAccess();
      alternativeCache = FallbackCraftingRecipe.computeAlternatives(level, alternatives);
    }
    return alternativeCache;
  }
  @Override public boolean matches(CraftingInput input, Level level) { return super.matches(input, level) && matchesAlternatives(input, level); }
  @Override @SuppressWarnings({"rawtypes","unchecked"}) public RecipeSerializer<ShapelessRecipe> getSerializer() { return (RecipeSerializer)MantleRecipes.CRAFTING_SHAPELESS_FALLBACK.get(); }
}
