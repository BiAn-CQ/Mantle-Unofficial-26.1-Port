package slimeknights.mantle.recipe.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.recipe.helper.ItemOutput;

/** Extension of {@link BlastingRecipe} to support {@link ItemOutput} */
public class BlastingResultRecipe extends BlastingRecipe implements CookingResultRecipe {
  public static final MapCodec<BlastingResultRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
    AbstractCookingRecipe.CookingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
    Ingredient.CODEC.fieldOf("ingredient").forGetter(BlastingResultRecipe::input),
    ItemOutput.REQUIRED_STACK_CODEC.fieldOf("result").forGetter(BlastingResultRecipe::getResult),
    Codec.FLOAT.optionalFieldOf("experience", 0f).forGetter(BlastingResultRecipe::experience),
    Codec.INT.optionalFieldOf("cookingtime", 100).forGetter(BlastingResultRecipe::cookingTime)
  ).apply(instance, BlastingResultRecipe::new));
  public static final StreamCodec<RegistryFriendlyByteBuf, BlastingResultRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
    AbstractCookingRecipe.CookingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
    Ingredient.CONTENTS_STREAM_CODEC, BlastingResultRecipe::input,
    ItemOutput.STREAM_CODEC, BlastingResultRecipe::getResult,
    ByteBufCodecs.FLOAT, BlastingResultRecipe::experience,
    ByteBufCodecs.INT, BlastingResultRecipe::cookingTime,
    BlastingResultRecipe::new);
  public static final RecipeSerializer<BlastingResultRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

  private final ItemOutput result;
  public BlastingResultRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo,
                              Ingredient ingredient, ItemOutput result, float experience, int cookingTime) {
    super(commonInfo, bookInfo, ingredient, CookingResultRecipe.template(result), experience, cookingTime);
    this.result = result;
  }
  @Override public ItemOutput getResult() { return result; }
  @Override public ItemStack assemble(SingleRecipeInput input) { return result.copy(); }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  public RecipeSerializer<BlastingRecipe> getSerializer() { return (RecipeSerializer)MantleRecipes.BLASTING.get(); }
}
