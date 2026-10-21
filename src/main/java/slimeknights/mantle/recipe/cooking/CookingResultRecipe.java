package slimeknights.mantle.recipe.cooking;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import slimeknights.mantle.recipe.helper.ItemOutput;

/** Simplifies the serializers for result recipes */
public interface CookingResultRecipe {
  /** Gets the recipe result */
  ItemOutput getResult();

  /** Accepts both the native field and the official Mantle spelling. */
  static com.mojang.serialization.MapCodec<Integer> cookingTimeField(int defaultTime) {
    return com.mojang.serialization.codecs.RecordCodecBuilder.<Integer>mapCodec(instance -> instance.group(
      net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("cookingtime").forGetter(java.util.Optional::of),
      net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("cooking_time").forGetter(value -> java.util.Optional.empty())
    ).apply(instance,(nativeTime,mantleTime) -> nativeTime.or(() -> mantleTime).orElse(defaultTime)));
  }

  /** A missing tag preference is a valid empty display even though native templates cannot be empty. */
  static java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display(
      net.minecraft.world.item.crafting.AbstractCookingRecipe recipe, ItemOutput output, net.minecraft.world.item.Item furnace) {
    ItemStack stack = output.get();
    net.minecraft.world.item.crafting.display.SlotDisplay result = stack.isEmpty()
      ? net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE
      : new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    return java.util.List.of(new net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay(
      recipe.input().display(), net.minecraft.world.item.crafting.display.SlotDisplay.AnyFuel.INSTANCE, result,
      new net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay(furnace),recipe.cookingTime(),recipe.experience()));
  }

  static ItemStackTemplate template(ItemOutput output) {
    ItemStack stack = output.get();
    if (stack.isEmpty()) {
      throw new IllegalStateException("Cooking recipe result resolved to an empty item stack");
    }
    return ItemStackTemplate.fromNonEmptyStack(stack);
  }
}
