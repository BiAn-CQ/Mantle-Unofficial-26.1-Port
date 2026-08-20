package slimeknights.mantle.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Gson adapter for identifier-like values which supplies a namespace for
 * unqualified strings. The type is intentionally not bounded to
 * {@link Identifier}; Tinkers' Construct also uses small type-safe wrappers
 * around identifiers.
 */
public class ResourceLocationSerializer<T> implements JsonDeserializer<T>, JsonSerializer<T> {
  private final Function<String,T> constructor;
  private final String modId;

  public ResourceLocationSerializer(Function<String,T> constructor, String modId) {
    this.constructor = constructor;
    this.modId = modId;
  }

  /** Creates an adapter for vanilla identifiers. */
  public static ResourceLocationSerializer<Identifier> resourceLocation(String modId) {
    return new ResourceLocationSerializer<>(Identifier::parse, modId);
  }

  @Override
  public JsonElement serialize(T location, Type type, JsonSerializationContext context) {
    return new JsonPrimitive(location.toString());
  }

  @Override
  public T deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
    String location = GsonHelper.convertToString(element, "location");
    if (!location.contains(":")) {
      location = modId + ':' + location;
    }
    return constructor.apply(location);
  }
}
