package slimeknights.mantle.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Interface for a pair of recipe and name. Like {@link net.minecraft.world.item.crafting.RecipeHolder} but without the strict bound. */
public interface NamedRecipe<T> {
  /** Gets the ID for the recipe. */
  Identifier id();

  /** Gets a list of source IDs for a dynamic recipe. */
  default List<Identifier> sourceIds() {
    return List.of(id());
  }

  /** Gets the recipe instance */
  T recipe();

  /** Named recipe that is itself the recipe */
  interface Self<T> extends NamedRecipe<T> {
    @Override
    @SuppressWarnings("unchecked")
    default T recipe() {
      return (T) this;
    }
  }


  /** Creates a new named recipe instance */
  static <T> NamedRecipe<T> of(Identifier id, T recipe) {
    return new Wrapped<>(id, recipe);
  }

  /** Creates a new named recipe instance from the given recipe holder. */
  static <T extends Recipe<?>> NamedRecipe<T> of(RecipeHolder<T> holder) {
    return new Wrapped<>(holder.id().identifier(), holder.value());
  }

  /** Creates a new named recipe instance with the given list of parent IDs. */
  static <T> NamedRecipe<T> of(Identifier id, List<Identifier> parents, T recipe) {
    return new Wrapped<>(id, parents, recipe);
  }

  /** Gets a recipe class for JEI with the given type. */
  @SuppressWarnings({"unchecked", "RedundantCast"})
  static <T> Class<? extends NamedRecipe<T>> recipeClass() {
    return (Class<? extends NamedRecipe<T>>)(Class<?>) NamedRecipe.class;
  }

  /** Wrapper around an ID and recipe pair. */
  record Wrapped<T>(Identifier id, List<Identifier> sourceIds, T recipe) implements NamedRecipe<T> {
    /** Creates a wrapped recipe using the recipe ID as the parent ID. */
    public Wrapped(Identifier id, T recipe) {
      this(id, List.of(id), recipe);
    }
  }
}
