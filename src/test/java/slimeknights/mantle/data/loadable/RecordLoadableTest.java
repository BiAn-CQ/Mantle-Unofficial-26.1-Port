package slimeknights.mantle.data.loadable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordLoadableTest {
  private record Example(String name, int level) {}

  private static final RecordLoadable<Example> LOADABLE = RecordLoadable.create(
    StringLoadable.DEFAULT.requiredField("name", Example::name),
    IntLoadable.range(1, 5).defaultField("level", 1, Example::level),
    Example::new
  );

  @Test
  void requiredAndDefaultFieldsDeserialize() {
    JsonObject json = new JsonObject();
    json.addProperty("name", "sample");
    assertEquals(new Example("sample", 1), LOADABLE.deserialize(json));
  }

  @Test
  void explicitDefaultFieldValueDeserializes() {
    JsonObject json = new JsonObject();
    json.addProperty("name", "sample");
    json.addProperty("level", 4);
    assertEquals(new Example("sample", 4), LOADABLE.deserialize(json));
  }

  @Test
  void missingRequiredFieldFailsClearly() {
    assertThrows(JsonSyntaxException.class, () -> LOADABLE.deserialize(new JsonObject()));
  }

  @Test
  void defaultValueIsOmittedDuringSerialization() {
    JsonObject json = LOADABLE.serialize(new Example("sample", 1)).getAsJsonObject();
    assertEquals("sample", json.get("name").getAsString());
    assertFalse(json.has("level"));
  }
}
