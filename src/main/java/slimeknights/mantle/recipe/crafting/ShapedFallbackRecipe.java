package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ShapedFallbackRecipe extends ShapedRecipe {
  public static final MapCodec<ShapedFallbackRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
    Identifier.CODEC.listOf().fieldOf("alternatives").forGetter(recipe -> recipe.alternatives)
      // write extra data
  ).apply(instance, ShapedFallbackRecipe::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
    ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
    ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.alternatives,
    ShapedFallbackRecipe::new
  );

  public static final RecipeSerializer<ShapedFallbackRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

  private final ItemStackTemplate result;
  /** Recipes to skip if they match */
  private final List<Identifier> alternatives;
  private RecipeManager cachedManager;
  private List<CraftingRecipe> alternativeCache = List.of();

  public ShapedFallbackRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                              ShapedRecipePattern pattern, ItemStackTemplate result, List<Identifier> alternatives) {
    super(commonInfo, bookInfo, pattern, result);
    this.result = result;
    this.alternatives = List.copyOf(alternatives);
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    // if this recipe does not match, fail it
    if (!super.matches(input, level)) {
      return false;
    }

    if (!(level instanceof ServerLevel serverLevel)) {
      return true;
    }

    RecipeManager manager = serverLevel.recipeAccess();
    if (manager != cachedManager) {
      cachedManager = manager;
      alternativeCache = alternatives.stream()
        .map(id -> ResourceKey.<Recipe<?>>create(Registries.RECIPE, id))
        .map(manager::byKey)
        .flatMap(java.util.Optional::stream)
        .map(holder -> holder.value())
        .filter(recipe -> recipe.getClass() == ShapedRecipe.class || recipe.getClass() == ShapelessRecipe.class)
        .map(recipe -> (CraftingRecipe) recipe)
        .toList();
    }
    // fail if any alterntaive matches
    return alternativeCache.stream().noneMatch(recipe -> recipe.matches(input, level));
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public RecipeSerializer<ShapedRecipe> getSerializer() {
    return (RecipeSerializer) MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }
}
