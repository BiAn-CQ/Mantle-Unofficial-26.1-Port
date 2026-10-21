package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.resources.Identifier;
public class RetexturedModelBuilder extends ColoredModelBuilder {
  private final JsonArray retextured = new JsonArray();
  public RetexturedModelBuilder(Identifier model) { super(model); }
  public RetexturedModelBuilder retexture(String name) { retextured.add(name); return this; }
  @Override public JsonObject toJson(JsonObject json) { super.toJson(json); json.addProperty("type","mantle:retextured"); json.add("retextured_slots",retextured.deepCopy()); return json; }
}
