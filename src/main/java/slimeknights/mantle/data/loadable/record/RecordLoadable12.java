package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Function12;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/** Record loadable with 12 fields */
@SuppressWarnings("DuplicatedCode")
record RecordLoadable12<A,B,C,D,E,F,G,H,I,J,K,L,R>(
  RecordField<A,? super R> fieldA,
  RecordField<B,? super R> fieldB,
  RecordField<C,? super R> fieldC,
  RecordField<D,? super R> fieldD,
  RecordField<E,? super R> fieldE,
  RecordField<F,? super R> fieldF,
  RecordField<G,? super R> fieldG,
  RecordField<H,? super R> fieldH,
  RecordField<I,? super R> fieldI,
  RecordField<J,? super R> fieldJ,
  RecordField<K,? super R> fieldK,
  RecordField<L,? super R> fieldL,
  Function12<A,B,C,D,E,F,G,H,I,J,K,L,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(
      fieldA.get(json, context),
      fieldB.get(json, context),
      fieldC.get(json, context),
      fieldD.get(json, context),
      fieldE.get(json, context),
      fieldF.get(json, context),
      fieldG.get(json, context),
      fieldH.get(json, context),
      fieldI.get(json, context),
      fieldJ.get(json, context),
      fieldK.get(json, context),
      fieldL.get(json, context)
    );
  }

  @Override
  public void serialize(R object, JsonObject json) {
    serialize(object, json, TypedMap.EMPTY);
  }

  public void serialize(R object, JsonObject json, TypedMap context) {
    fieldA.serialize(object, json, context);
    fieldB.serialize(object, json, context);
    fieldC.serialize(object, json, context);
    fieldD.serialize(object, json, context);
    fieldE.serialize(object, json, context);
    fieldF.serialize(object, json, context);
    fieldG.serialize(object, json, context);
    fieldH.serialize(object, json, context);
    fieldI.serialize(object, json, context);
    fieldJ.serialize(object, json, context);
    fieldK.serialize(object, json, context);
    fieldL.serialize(object, json, context);
  }

  @Override
  public R decode(FriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(
      fieldA.decode(buffer, context),
      fieldB.decode(buffer, context),
      fieldC.decode(buffer, context),
      fieldD.decode(buffer, context),
      fieldE.decode(buffer, context),
      fieldF.decode(buffer, context),
      fieldG.decode(buffer, context),
      fieldH.decode(buffer, context),
      fieldI.decode(buffer, context),
      fieldJ.decode(buffer, context),
      fieldK.decode(buffer, context),
      fieldL.decode(buffer, context)
    );
  }

  @Override
  public void encode(FriendlyByteBuf buffer, R object) {
    encode(buffer, object, TypedMap.EMPTY);
  }

  public void encode(FriendlyByteBuf buffer, R object, TypedMap context) {
    fieldA.encode(buffer, object, context);
    fieldB.encode(buffer, object, context);
    fieldC.encode(buffer, object, context);
    fieldD.encode(buffer, object, context);
    fieldE.encode(buffer, object, context);
    fieldF.encode(buffer, object, context);
    fieldG.encode(buffer, object, context);
    fieldH.encode(buffer, object, context);
    fieldI.encode(buffer, object, context);
    fieldJ.encode(buffer, object, context);
    fieldK.encode(buffer, object, context);
    fieldL.encode(buffer, object, context);
  }
}
