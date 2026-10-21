package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.function.Supplier;

/** Implementation of a codec using a loadable. Note this will be inefficient comparatively when using {@link net.minecraft.nbt.NbtOps} */
public record LoadableCodec<T>(Loadable<T> loadable, Supplier<TypedMap> context) implements JsonCodec<T> {
  public LoadableCodec(Loadable<T> loadable) {
    this(loadable, TypedMap.EMPTY_SUPPLIER);
  }

  private TypedMap context(DynamicOps<?> ops) {
    return TypedMapBuilder.builder().putAll(context.get()).putAll(ContextKey.opsContext(ops).build()).build();
  }
  @Override
  public T deserialize(JsonElement element, DynamicOps<?> ops) {
    return loadable.convert(element, "codec", context(ops));
  }

  @Override
  public JsonElement serialize(T object, DynamicOps<?> ops) {
    return loadable.serialize(object, context(ops));
  }
}
