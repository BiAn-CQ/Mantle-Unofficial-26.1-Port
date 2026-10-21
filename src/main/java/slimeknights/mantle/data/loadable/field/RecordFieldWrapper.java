package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.util.typed.TypedMap;

/** Wrapper around a {@link RecordField} which redirects all methods. */
public interface RecordFieldWrapper<T,P> extends RecordField<T,P> {
  /** Base field being wrapped */
  RecordField<T,P> base();

  @Override
  default T get(JsonObject json, TypedMap context) {
    return base().get(json, context);
  }

  @Override
  default void serialize(P parent, JsonObject json, TypedMap context) {
    base().serializeInto(parent, json, context);
  }

  @Override
  default T decode(net.minecraft.network.FriendlyByteBuf buffer, TypedMap context) {
    return base().decode(buffer, context);
  }

  @Override
  default void encode(net.minecraft.network.FriendlyByteBuf buffer, P parent, TypedMap context) {
    base().encode(buffer, parent, context);
  }
}
