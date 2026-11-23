package slimeknights.mantle.registration.deferred;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;

import java.util.function.Function;
import java.util.function.Supplier;

/** Typed registration helpers for native 26.1 recipe serializers. */
public class RecipeSerializerDeferredRegister extends DeferredRegister<RecipeSerializer<?>> {
  public RecipeSerializerDeferredRegister(String namespace) {
    super(Registries.RECIPE_SERIALIZER, namespace);
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerSerializer(String name, Function<Identifier,RecipeSerializer<T>> constructor) {
    return register(name, constructor);
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerSerializer(String name, Supplier<RecipeSerializer<T>> constructor) {
    return register(name, constructor);
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerSimple(String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf,T> streamCodec) {
    return registerSerializer(name, () -> new RecipeSerializer<>(codec, streamCodec));
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerSingleton(String name, T instance) {
    return registerSerializer(name, () -> SimpleRecipeSerializer.singleton(instance));
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerNoArguments(String name, Supplier<T> constructor) {
    return registerSerializer(name, () -> SimpleRecipeSerializer.noArguments(constructor));
  }

  public <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerLoadable(String name, RecordLoadable<T> loader) {
    return registerSerializer(name, () -> LoadableRecipeSerializer.of(loader));
  }

  public <T extends R,R extends Recipe<?>> DeferredHolder<RecipeSerializer<?>,RecipeSerializer<T>> registerLoadable(String name, RecordLoadable<T> loader, Supplier<? extends RecipeType<R>> type) {
    return registerSerializer(name, () -> LoadableRecipeSerializer.of(loader, type));
  }
}
