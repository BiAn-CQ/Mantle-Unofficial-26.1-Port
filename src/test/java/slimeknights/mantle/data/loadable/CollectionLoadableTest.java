package slimeknights.mantle.data.loadable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionLoadableTest {
  @Test
  void listRoundTripsJsonArray() {
    Loadable<List<Integer>> loadable = IntLoadable.ANY_FULL.list(0);
    JsonArray json = new JsonArray();
    json.add(1);
    json.add(2);

    List<Integer> parsed = loadable.convert(json, "values");
    assertEquals(List.of(1, 2), parsed);
    assertEquals(json, loadable.serialize(parsed));
  }

  @Test
  void listEnforcesMinimumSize() {
    Loadable<List<Integer>> loadable = IntLoadable.ANY_FULL.list(2);
    JsonArray oneValue = new JsonArray();
    oneValue.add(1);
    assertThrows(JsonSyntaxException.class, () -> loadable.convert(oneValue, "values"));
    assertThrows(RuntimeException.class, () -> loadable.serialize(List.of(1)));
  }

  @Test
  void compactListAcceptsSinglePrimitive() {
    Loadable<List<Integer>> loadable = IntLoadable.ANY_FULL.list(ArrayLoadable.COMPACT);
    assertEquals(List.of(7), loadable.convert(new JsonPrimitive(7), "values"));
    assertEquals(new JsonPrimitive(7), loadable.serialize(List.of(7)));
  }

  @Test
  void setUsesImmutableDistinctValues() {
    Loadable<Set<String>> loadable = StringLoadable.DEFAULT.set(1);
    JsonArray json = new JsonArray();
    json.add("alpha");
    json.add("beta");
    assertEquals(Set.of("alpha", "beta"), loadable.convert(json, "values"));
  }

  @Test
  void mapConvertsKeysAndValues() {
    Loadable<Map<String,Boolean>> loadable = StringLoadable.DEFAULT.mapWithValues(BooleanLoadable.DEFAULT, 1);
    JsonObject json = new JsonObject();
    json.addProperty("enabled", true);
    assertEquals(Map.of("enabled", true), loadable.convert(json, "settings"));
    assertEquals(json, loadable.serialize(Map.of("enabled", true)));
  }
}
