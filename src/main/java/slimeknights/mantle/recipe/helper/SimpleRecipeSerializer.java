package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Simple implementation of a recipe serializer with no properties other than recipe ID. */

public final class SimpleRecipeSerializer {
  private static final Map<RecipeSerializer<?>,Supplier<? extends Recipe<?>>> CONSTRUCTORS =
    Collections.synchronizedMap(new IdentityHashMap<>());

  private SimpleRecipeSerializer() {}

  public static <T extends Recipe<?>> RecipeSerializer<T> of(Supplier<T> constructor) {
    RecipeSerializer<T> serializer = new RecipeSerializer<>(MapCodec.unit(constructor), StreamCodec.of(
      (RegistryFriendlyByteBuf buffer, T recipe) -> {}, buffer -> constructor.get()));
    CONSTRUCTORS.put(serializer, constructor);
    return serializer;
  }

  public static boolean isSimple(RecipeSerializer<?> serializer) {
    return CONSTRUCTORS.containsKey(serializer);
  }

  /** Creates a fieldless recipe from a serializer registered through {@link #of(Supplier)}. */
  @SuppressWarnings("unchecked")
  public static <T extends Recipe<?>> T create(RecipeSerializer<? extends T> serializer) {
    Supplier<?> constructor = CONSTRUCTORS.get(serializer);
    if (constructor == null) {
      throw new IllegalArgumentException("Recipe serializer is not backed by a Mantle fieldless constructor: " + serializer);
    }
    return (T) constructor.get();
  }
}
