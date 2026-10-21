package slimeknights.mantle.data.loadable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.function.Supplier;

/** Adapts a loadable's network form to a native stream codec. */
public record StreamableCodec<B extends FriendlyByteBuf,T>(Streamable<T> streamable, Supplier<TypedMap> context) implements StreamCodec<B,T> {
  public StreamableCodec(Streamable<T> streamable) {
    this(streamable, TypedMap.EMPTY_SUPPLIER);
  }

  private TypedMap context(B buffer) {
    TypedMapBuilder builder = TypedMapBuilder.builder().putAll(context.get());
    if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      builder.putAll(ContextKey.lookupContext(registryBuffer.registryAccess()).build());
    }
    return builder.build();
  }

  @Override
  public T decode(B buffer) {
    return streamable.decode(buffer, context(buffer));
  }

  @Override
  public void encode(B buffer, T value) {
    streamable.encode(buffer, value, context(buffer));
  }
}
