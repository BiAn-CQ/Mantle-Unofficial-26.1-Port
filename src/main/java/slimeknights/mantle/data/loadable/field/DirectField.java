package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * A record loadable that loads directly into the parent instead of nesting.
 * @param <P>  Parent object
 * @param <T>  Loadable type
 */
public record DirectField<T,P>(RecordLoadable<T> loadable, Function<P,T> getter) implements AlwaysPresentRecordField<T,P> {
  @Override
  public T get(JsonObject json, TypedMap context) {
    return loadable.deserialize(json, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    serialize(parent, json, TypedMap.EMPTY);
  }

  public void serialize(P parent, JsonObject json, TypedMap context) {
    loadable.serialize(getter.apply(parent), json, context);
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }
}
