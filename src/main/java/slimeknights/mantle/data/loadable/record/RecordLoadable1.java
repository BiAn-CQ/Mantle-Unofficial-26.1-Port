package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/** Record loadable with a single field */
record RecordLoadable1<A,R>(
  RecordField<A,? super R> fieldA,
  Function<A,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(fieldA.get(json, context));
  }

  @Override
  public void serialize(R object, JsonObject json) {
    serialize(object, json, TypedMap.EMPTY);
  }

  public void serialize(R object, JsonObject json, TypedMap context) {
    fieldA.serialize(object, json, context);
  }

  @Override
  public R decode(FriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(fieldA.decode(buffer, context));
  }

  @Override
  public void encode(FriendlyByteBuf buffer, R object) {
    encode(buffer, object, TypedMap.EMPTY);
  }

  public void encode(FriendlyByteBuf buffer, R object, TypedMap context) {
    fieldA.encode(buffer, object, context);
  }
}
