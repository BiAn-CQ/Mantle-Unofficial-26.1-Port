package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated(forRemoval = true)
/** Ingredient serializer made using loadables */
public record LoadableIngredientSerializer<T>(RecordLoadable<T> loadable) {
  public T parse(JsonObject json) {
    return loadable.deserialize(json);
  }

  /** Serializes the ingredient to JSON */
  public JsonObject serialize(T ingredient) {
    JsonObject json = new JsonObject();
    loadable.serialize(ingredient, json);
    return json;
  }

  /** 26.1 codec bridge for custom ingredients. */
  public MapCodec<T> codec() {
    return mapCodec(this::parse, this::serialize);
  }

  /** Creates a registry codec from the legacy JSON parser/serializer pair. */
  public static <T> MapCodec<T> mapCodec(Function<JsonObject,T> parser, BiConsumer<T,JsonObject> serializer) {
    JsonCodec<T> codec = new JsonCodec<>() {
      @Override
      public T deserialize(com.google.gson.JsonElement element, DynamicOps<?> ops) {
        if (!element.isJsonObject()) {
          throw new JsonParseException("Expected ingredient to be a JSON object");
        }
        return parser.apply(element.getAsJsonObject());
      }

      @Override
      public com.google.gson.JsonElement serialize(T ingredient, DynamicOps<?> ops) {
        JsonObject json = new JsonObject();
        serializer.accept(ingredient, json);
        return json;
      }
    };
    return MapCodec.assumeMapUnsafe(codec);
  }

  /** Convenience overload for serializers that return their JSON object directly. */
  public static <T> MapCodec<T> mapCodec(Function<JsonObject,T> parser, Function<T,JsonObject> serializer) {
    return mapCodec(parser, (value, json) -> {
      JsonObject encoded = serializer.apply(value);
      encoded.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
    });
  }
}
