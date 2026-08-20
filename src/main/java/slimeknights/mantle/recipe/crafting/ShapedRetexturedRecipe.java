package slimeknights.mantle.recipe.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.util.RetexturedHelper;

/** Recipe which sets the texture for a {@link slimeknights.mantle.block.RetexturedBlock} based on an ingredient input. */
// TODO 1.21: rework to be more like the ShapedMaterialsRecipe from Tinkers for more efficient network syncing
@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe extends ShapedRecipe {
  private static final Codec<Character> TEXTURE_KEY_CODEC = Codec.STRING.comapFlatMap(value -> {
    if (value.length() != 1) {
      return DataResult.error(() -> "Invalid texture key '" + value + "': expected exactly one character");
    }
    return DataResult.success(value.charAt(0));
  }, String::valueOf);
  private static final Codec<Either<Character, Ingredient>> TEXTURE_CODEC = Codec.either(TEXTURE_KEY_CODEC, Ingredient.CODEC);

  private record JsonData(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                          ShapedRecipePattern.Data patternData, ItemStackTemplate result,
                          Either<Character, Ingredient> texture, boolean matchAll) {
    private static final MapCodec<JsonData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Recipe.CommonInfo.MAP_CODEC.forGetter(JsonData::commonInfo),
      CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(JsonData::bookInfo),
      ShapedRecipePattern.Data.MAP_CODEC.forGetter(JsonData::patternData),
      ItemStackTemplate.CODEC.fieldOf("result").forGetter(JsonData::result),
      TEXTURE_CODEC.fieldOf("texture").forGetter(JsonData::texture),
      Codec.BOOL.optionalFieldOf("match_all", false).forGetter(JsonData::matchAll)
    ).apply(instance, JsonData::new));

    private DataResult<ShapedRetexturedRecipe> unpack() {
      Ingredient resolvedTexture = texture.map(patternData.key()::get, ingredient -> ingredient);
    // set the texture if found. No texture will use the fallback
      if (resolvedTexture == null) {
        return DataResult.error(() -> "Texture ingredient references a symbol that is not defined in the shaped recipe key");
      }
      try {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(patternData.key(), patternData.pattern());
        return DataResult.success(new ShapedRetexturedRecipe(commonInfo, bookInfo, pattern, result, resolvedTexture, matchAll, this));
      } catch (RuntimeException exception) {
        return DataResult.error(exception::getMessage);
      }
    }
  }

  public static final MapCodec<ShapedRetexturedRecipe> MAP_CODEC = JsonData.MAP_CODEC.flatXmap(
    JsonData::unpack,
    recipe -> recipe.jsonData == null
      ? DataResult.error(() -> "Cannot encode a shaped retextured recipe decoded from the network")
      : DataResult.success(recipe.jsonData)
  );

  public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRetexturedRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
    ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
    ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
    Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.texture,
    ByteBufCodecs.BOOL, recipe -> recipe.matchAll,
    ShapedRetexturedRecipe::new
  );

  public static final RecipeSerializer<ShapedRetexturedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

  private final ItemStackTemplate result;
  private final Ingredient texture;
  private final boolean matchAll;
  private final @Nullable JsonData jsonData;

  ShapedRetexturedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                         ShapedRecipePattern pattern, ItemStackTemplate result,
                         Ingredient texture, boolean matchAll) {
    this(commonInfo, bookInfo, pattern, result, texture, matchAll, null);
  }

  static ShapedRetexturedRecipe fromJsonData(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                             ShapedRecipePattern.Data pattern, ItemStackTemplate result,
                                             Either<Character,Ingredient> texture, boolean matchAll) {
    return new JsonData(commonInfo, bookInfo, pattern, result, texture, matchAll).unpack().getOrThrow();
  }

  private ShapedRetexturedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                 ShapedRecipePattern pattern, ItemStackTemplate result,
                                 Ingredient texture, boolean matchAll, @Nullable JsonData jsonData) {
    super(commonInfo, bookInfo, pattern, result);
    this.result = result;
    this.texture = texture;
    this.matchAll = matchAll;
    this.jsonData = jsonData;
  }

  public Ingredient getTexture() {
    return texture;
  }

  /** Returns an otherwise identical recipe with a data-component-aware result. */
  public ShapedRetexturedRecipe withResult(ItemStack result) {
    ItemStackTemplate template = ItemStackTemplate.fromNonEmptyStack(result);
    if (jsonData != null) {
      return new JsonData(commonInfo, bookInfo, jsonData.patternData(), template,
                          jsonData.texture(), matchAll).unpack().getOrThrow();
    }
    return new ShapedRetexturedRecipe(commonInfo, bookInfo, pattern, template, texture, matchAll);
  }

  /**
   * Gets the output using the given texture
   * @param texture  Texture to use
   * @return  Output with texture. Will be blank if the input is not a block
   */
  public ItemStack getResultItem(Item texture, RegistryAccess access) {
    return RetexturedHelper.setTexture(result.create(), Block.byItem(texture));
  }

  @Override
  public ItemStack assemble(CraftingInput input) {
    ItemStack assembled = super.assemble(input);
    Block currentTexture = null;
    for (int slot = 0; slot < input.size(); slot++) {
      ItemStack stack = input.getItem(slot);
      if (!stack.isEmpty() && texture.test(stack)) {
        // fetch texture from the block if it has one
        Block block = RetexturedHelper.getTexture(stack);
        // assuming it does not, use the block itself as the texture (provided it is not the result that is)
        if (block == Blocks.AIR && stack.getItem() != assembled.getItem()) {
          block = Block.byItem(stack.getItem());
        }
        // if no texture, skip
        if (block == Blocks.AIR) {
          continue;
        }

        // if we have not found a texture yet, store the found block
        if (currentTexture == null) {
          currentTexture = block;
          // match all means we must check the rest. If not match all, we can be done
          if (!matchAll) {
            break;
          }

          // if we found a texture before, must match or we do no texture
        } else if (currentTexture != block) {
          currentTexture = null;
          break;
        }
      }
    }
    return currentTexture == null ? assembled : RetexturedHelper.setTexture(assembled, currentTexture);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public RecipeSerializer<ShapedRecipe> getSerializer() {
    return (RecipeSerializer) MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }
}
