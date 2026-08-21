package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Extension of the vanilla ingredient to make stack size checks
 */
public class SizedIngredient implements Predicate<ItemStack> {
  /** Empty sized ingredient wrapper. Matches only the empty stack of size 0 */
  public static final SizedIngredient EMPTY = new SizedIngredient(null, 0);

  public static final RecordLoadable<SizedIngredient> LOADABLE = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.tryDirectField("ingredient", SizedIngredient::getIngredient, "amount_needed"),
    IntLoadable.FROM_ONE.defaultField("amount_needed", 1, SizedIngredient::getAmountNeeded),
    SizedIngredient::new);

  /** Ingredient to use in recipe match */
  private final Ingredient ingredient;
  /** Amount of this ingredient needed */
  private final int amountNeeded;

  /** Last list of matching stacks from the ingredient */
  private WeakReference<ItemStack[]> lastIngredientMatch;
  /** Cached matching stacks from last time it was requested */
  private List<ItemStack> matchingStacks;

  public SizedIngredient(Ingredient ingredient, int amountNeeded) {
    if (amountNeeded < 0) {
      throw new IllegalArgumentException("Ingredient amount cannot be negative");
    }
    this.ingredient = ingredient;
    this.amountNeeded = amountNeeded;
  }

  public static SizedIngredient of(Ingredient ingredient, int amountNeeded) {
    return new SizedIngredient(ingredient, amountNeeded);
  }

  public Ingredient getIngredient() {
    return ingredient;
  }

  public int getAmountNeeded() {
    return amountNeeded;
  }

  /**
   * Gets a new sized ingredient with a size of 1
   * @param ingredient  Ingredient
   * @return  Sized ingredient matching any size
   */
  public static SizedIngredient of(Ingredient ingredient) {
    return of(ingredient, 1);
  }

  /**
   * Gets a new sized ingredient with a size of 1
   * @param amountNeeded  Number that must match of this ingredient
   * @param items         List of items
   * @return  Sized ingredient matching any size
   */
  public static SizedIngredient fromItems(int amountNeeded, ItemLike... items) {
    return of(Ingredient.of(items), amountNeeded);
  }

  /**
   * Gets a new sized ingredient with a size of 1
   * @param items  List of items
   * @return  Sized ingredient matching any size
   */
  public static SizedIngredient fromItems(ItemLike... items) {
    return fromItems(1, items);
  }

  /**
   * Gets a new sized ingredient with a size of 1
   * @param tag           Tag to match
   * @param amountNeeded  Number that must match of this ingredient
   * @return  Sized ingredient matching any size
   */
  public static SizedIngredient fromTag(TagKey<Item> tag, int amountNeeded) {
    // Resolve through the live tag manager. Construction-time named holder
    // sets cannot be enumerated while client book data is being prepared.
    return of(ItemTagIngredient.of(tag), amountNeeded);
  }

  /**
   * Gets a new sized ingredient with a size of 1
   * @param tag  Tag to match
   * @return  Sized ingredient matching any size
   */
  public static SizedIngredient fromTag(TagKey<Item> tag) {
    return fromTag(tag, 1);
  }

  @Override
  public boolean test(ItemStack stack) {
    return ingredient == null ? stack.isEmpty() : stack.getCount() >= amountNeeded && ingredient.test(stack);
  }

  /**
   * Checks if the ingredient has no matching stacks
   * @return  True if the ingredient has no matching stacks
   */
  public boolean isEmpty() {
    return ingredient == null || ingredient.isEmpty();
  }

  /**
   * Gets a list of matching stacks for display in JEI
   * @return  List of matching stacks
   */
  public List<ItemStack> getMatchingStacks() {
    if (ingredient == null) {
      return List.of();
    }
    ItemStack[] ingredientMatch = ingredient.items().map(holder -> new ItemStack(holder, amountNeeded)).toArray(ItemStack[]::new);
    // if we never cached, or the array instance changed since we last cached, recache
    if (matchingStacks == null || lastIngredientMatch == null || lastIngredientMatch.get() != ingredientMatch) {
      matchingStacks = Arrays.stream(ingredientMatch).map(stack -> {
        if (stack.getCount() != amountNeeded) {
          stack = stack.copy();
          stack.setCount(amountNeeded);
        }
        return stack;
      }).collect(Collectors.toList());
      lastIngredientMatch = new WeakReference<>(ingredientMatch);
    }
    return matchingStacks;
  }

  /** use {@link #LOADABLE} with {@link slimeknights.mantle.data.loadable.Loadable#encode(FriendlyByteBuf, Object)} */
  @Deprecated(forRemoval = true)
  public void write(FriendlyByteBuf buffer) {
    LOADABLE.encode(buffer, this);
  }

  /** @deprecated use {@link #LOADABLE} with {@link slimeknights.mantle.data.loadable.Loadable#serialize(Object)} or {@link RecordLoadable#serialize(Object, JsonObject)} */
  @Deprecated(forRemoval = true)
  public JsonObject serialize() {
    JsonObject json = new JsonObject();
    LOADABLE.serialize(this, json);
    return json;
  }

  /** @deprecated use {@link #LOADABLE} with {@link slimeknights.mantle.data.loadable.Loadable#decode(FriendlyByteBuf)}  */
  @Deprecated(forRemoval = true)
  public static SizedIngredient read(FriendlyByteBuf buffer) {
    return LOADABLE.decode(buffer);
  }

  /** @deprecated use {@link #LOADABLE} with {@link RecordLoadable#deserialize(JsonObject)} */
  @Deprecated(forRemoval = true)
  public static SizedIngredient deserialize(JsonObject json) {
    return LOADABLE.deserialize(json);
  }
}
