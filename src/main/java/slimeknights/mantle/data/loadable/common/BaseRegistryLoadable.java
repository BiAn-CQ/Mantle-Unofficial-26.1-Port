package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.Set;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.SetLoadable;

/** Common logic for {@link RegistryLoadable} and {@link LazyRegistryLoadable} */
public interface BaseRegistryLoadable<T> extends IdentifierLoadable<T> {
  /** Gets the registry associated with this loadable. Null if the registry cannot be located */
  @Nullable
  Registry<T> registry();

  /** Gets the ID of this registry for error messages */
  Identifier registryId();

  /**
   * Gets the registry for the current decode context when the registry is a
   * datapack-backed registry.  Built-in registries remain the fallback for
   * callers that do not provide a context.
   */
  @Nullable
  default Registry<T> registry(TypedMap context) {
    RegistryAccess access = context.get(ContextKey.REGISTRY_ACCESS);
    if (access != null) {
      @SuppressWarnings("unchecked")
      ResourceKey<? extends Registry<T>> key = (ResourceKey<? extends Registry<T>>) (ResourceKey<?>) ResourceKey.createRegistryKey(registryId());
      Registry<T> contextual = access.lookup(key).orElse(null);
      if (contextual != null) {
        return contextual;
      }
    }
    return registry();
  }

  @Override
  default T fromKey(Identifier name, String key, TypedMap context) {
    ResourceKey<? extends Registry<T>> registryKey = (ResourceKey<? extends Registry<T>>) (ResourceKey<?>) ResourceKey.createRegistryKey(registryId());
    RegistryOps.RegistryInfoLookup registryLookup = context.get(ContextKey.REGISTRY_LOOKUP);
    if (registryLookup != null) {
      RegistryOps.RegistryInfo<T> info = registryLookup.lookup(registryKey).orElse(null);
      if (info != null) {
        ResourceKey<T> elementKey = ResourceKey.create(registryKey, name);
        T value = info.getter().get(elementKey).map(Holder::value).orElse(null);
        if (value != null) {
          return value;
        }
        throw new JsonSyntaxException("Unable to parse " + key + " as registry " + registryId() + " does not contain ID " + name);
      }
    }
    Registry<T> registry = registry(context);
    if (registry != null && registry.containsKey(name)) {
      T value = registry.getOptional(name).orElse(null);
      if (value != null) {
        return value;
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as registry " + registryId() + " does not contain ID " + name);
  }

  @Override
  default Identifier getKey(T object) {
    Registry<T> registry = registry();
    if (registry != null) {
      Identifier location = registry.getKey(object);
      if (location != null) {
        return location;
      }
    }
    throw new RuntimeException("Registry " + registryId() + " does not contain object " + object);
  }

  @Override
  default T decode(FriendlyByteBuf buffer, TypedMap context) {
    int id = buffer.readVarInt();
    Registry<T> registry = registry(context);
    if (registry != null) {
      T value = registry.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
      if (value != null) {
        return value;
      }
    }
    throw new DecoderException("Registry " + registryId() + " does not contain ID " + id);
  }

  @Override
  default void encode(FriendlyByteBuf buffer, T object) {
    Registry<T> registry = registry();
    if (registry == null) {
      throw new EncoderException("Registry " + registryId() + " cannot be located");
    }
    buffer.writeVarInt(registry.getId(object));
  }

  @Override
  default Loadable<Set<T>> set(int minSize) {
    return new SetLoadable.Ordered<>(this, minSize);
  }
}
