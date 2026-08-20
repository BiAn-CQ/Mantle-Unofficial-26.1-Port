package slimeknights.mantle.recipe.helper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;

@Deprecated(forRemoval = true)
/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible
 * @param <T>  Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> {
  T fromNetworkSafe(RegistryFriendlyByteBuf buffer);

  /**
   * Write the method to the buffer
   * @param buffer  Buffer instance
   * @param recipe  Recipe instance
   * @throws RuntimeException  If any errors happen, the exception will be logged automatically
   */
  void toNetworkSafe(RegistryFriendlyByteBuf buffer, T recipe);
}
