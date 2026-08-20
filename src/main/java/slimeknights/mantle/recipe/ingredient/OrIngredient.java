package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Custom ingredient representing the OR of several vanilla or custom ingredients. */
public final class OrIngredient implements ICustomIngredient {
  public static final MapCodec<OrIngredient> MAP_CODEC = Ingredient.CODEC.listOf()
    .fieldOf("ingredients")
    .xmap(OrIngredient::new, ingredient -> ingredient.ingredients);

  private final List<Ingredient> ingredients;

  public OrIngredient(List<Ingredient> ingredients) {
    if (ingredients.isEmpty()) {
      throw new IllegalArgumentException("OR ingredient cannot be empty");
    }
    this.ingredients = List.copyOf(ingredients);
  }

  /** Creates a vanilla ingredient whose serialized form explicitly represents an OR. */
  public static Ingredient of(List<Ingredient> ingredients) {
    return new OrIngredient(ingredients).toVanilla();
  }

  @Override
  public boolean test(ItemStack stack) {
    return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
  }

  @Override
  public Stream<Holder<Item>> items() {
    return ingredients.stream().flatMap(Ingredient::items).distinct();
  }

  @Override
  public boolean isSimple() {
    return ingredients.stream().allMatch(Ingredient::isSimple);
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.OR.get();
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof OrIngredient other && ingredients.equals(other.ingredients);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ingredients);
  }
}
