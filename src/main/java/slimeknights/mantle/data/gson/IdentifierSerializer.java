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

/** Identifier serializer which supplies a chosen namespace for unqualified values. */
public final class IdentifierSerializer implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {
  private final String modId;

  private IdentifierSerializer(String modId) {
    this.modId = modId;
  }

  public static IdentifierSerializer resourceLocation(String modId) {
    return new IdentifierSerializer(modId);
  }

  @Override
  public JsonElement serialize(Identifier location, Type type, JsonSerializationContext context) {
    return new JsonPrimitive(location.toString());
  }

  @Override
  public Identifier deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
    String location = GsonHelper.convertToString(element, "location");
    return Identifier.parse(location.contains(":") ? location : modId + ':' + location);
  }
}
