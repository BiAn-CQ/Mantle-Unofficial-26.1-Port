package slimeknights.mantle.plugin.jei;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** JEI 29 extension that animates and focuses {@link ShapedRetexturedRecipe} texture variants. */
public final class RetexturableRecipeExtension implements ICraftingCategoryExtension<ShapedRetexturedRecipe> {
  public static final RetexturableRecipeExtension INSTANCE = new RetexturableRecipeExtension();

  private RetexturableRecipeExtension() {}

  /** Resolves tag ingredients after registries have been bound without forcing an early named-holder lookup. */
  private static Stream<Holder<Item>> resolveItems(Ingredient ingredient) {
    try {
      var tag = ingredient.getValues().unwrapKey();
      if (tag.isPresent()) {
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag.get()).spliterator(), false);
      }
    } catch (IllegalStateException ignored) {
      // Custom ingredients need to expose their display stream themselves.
    }
    return ingredient.items();
  }

  private static List<ItemStack> stacks(Optional<Ingredient> ingredient) {
    return ingredient.map(value -> resolveItems(value).map(ItemStack::new).toList()).orElseGet(List::of);
  }

  /** Checks ingredient equality by resolved item identity; 26.1 ingredients no longer carry stack NBT. */
  private static boolean ingredientsMatch(Ingredient left, Ingredient right) {
    List<Item> leftItems = resolveItems(left).map(Holder::value).toList();
    List<Item> rightItems = resolveItems(right).map(Holder::value).toList();
    return leftItems.equals(rightItems);
  }

  @Override
  public List<SlotDisplay> getIngredients(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getIngredients().stream()
      .map(ingredient -> ingredient.<SlotDisplay>map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE))
      .toList();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedRetexturedRecipe> holder, IRecipeLayoutBuilder builder,
                        ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ShapedRetexturedRecipe recipe = holder.value();
    RegistryAccess access = Objects.requireNonNull(SafeClientAccess.getRegistryAccess());
    ItemStack plainResult = recipe.assemble(CraftingInput.EMPTY);

    List<ItemStack> displayOutputs = resolveItems(recipe.getTexture())
      .map(item -> recipe.getResultItem(item.value(), access))
      .toList();
    if (displayOutputs.isEmpty()) {
      displayOutputs = List.of(plainResult);
    }

    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(plainResult);
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(
      builder, VanillaTypes.ITEM_STACK, recipe.getIngredients().stream().map(RetexturableRecipeExtension::stacks).toList(),
      recipe.getWidth(), recipe.getHeight());
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, displayOutputs);

    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3",
        holder.id(), builder.getClass().getName());
      return;
    }

    Ingredient texture = recipe.getTexture();
    int[] textureSlots = IntStream.range(0, recipe.getIngredients().size())
      .filter(index -> recipe.getIngredients().get(index).filter(input -> ingredientsMatch(texture, input)).isPresent())
      .toArray();
    builder.createFocusLink(Streams.concat(
      Stream.of(output),
      Arrays.stream(textureSlots)
        .mapToObj(index -> inputs.get(MantleJEIConstants.getCraftingIndex(index, recipe.getWidth(), recipe.getHeight())))
    ).toArray(IRecipeSlotBuilder[]::new));
  }
}
