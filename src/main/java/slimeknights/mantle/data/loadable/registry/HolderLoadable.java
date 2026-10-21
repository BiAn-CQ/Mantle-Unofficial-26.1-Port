package slimeknights.mantle.data.loadable.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Common logic between {@link RegistryHolderLoadable} and {@link DatapackRegistryLoadable} */
public interface HolderLoadable<T> extends ResourceLocationLoadable<Holder<T>> {
  /** Gets the key for this registry */
  ResourceKey<? extends Registry<T>> key();

  /** Gets the stream codec for this type. */
  StreamCodec<RegistryFriendlyByteBuf,Holder<T>> streamCodec();

  @Override
  default Identifier getKey(Holder<T> holder, TypedMap context) {
    ResourceKey<T> key = holder.getKey();
    if (key != null) {
      return key.identifier();
    }
    throw new RuntimeException("Holder " + holder + " of registry " + key().identifier() + " has no key");
  }

  @Override
  default Holder<T> decode(net.minecraft.network.FriendlyByteBuf buffer, TypedMap context) {
    return streamCodec().decode((RegistryFriendlyByteBuf)buffer);
  }

  @Override
  default void encode(net.minecraft.network.FriendlyByteBuf buffer, Holder<T> holder, TypedMap context) {
    streamCodec().encode((RegistryFriendlyByteBuf)buffer, holder);
  }
}
