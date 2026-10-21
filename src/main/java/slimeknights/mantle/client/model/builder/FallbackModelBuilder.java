package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
/** Builds a lazy native fallback; unavailable model types are not decoded. */
public class FallbackModelBuilder {
  private final List<JsonObject> models=new ArrayList<>();
  public FallbackModelBuilder fallback(JsonObject model, @Nullable String modId) {
    JsonObject entry=model.deepCopy(); if (modId!=null) { entry.addProperty("fallback_mod_id",modId); } models.add(entry); return this;
  }
  public FallbackModelBuilder fallback(JsonObject model) { return fallback(model,null); }
  public JsonObject toJson(JsonObject json) {
    if (models.size()<2) { throw new IllegalStateException("Must have at least two fallback models"); }
    json.addProperty("type","mantle:fallback"); JsonArray array=new JsonArray(); models.forEach(model -> array.add(model.deepCopy())); json.add("models",array); return json;
  }
  public JsonObject toJson() { return toJson(new JsonObject()); }
}
