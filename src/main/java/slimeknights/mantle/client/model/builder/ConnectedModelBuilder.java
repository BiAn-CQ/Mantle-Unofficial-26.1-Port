package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
public class ConnectedModelBuilder extends ColoredModelBuilder {
  private final JsonObject textures = new JsonObject();
  private Direction[] sides;
  private String predicate;
  public ConnectedModelBuilder(Identifier model) { super(model); }
  public ConnectedModelBuilder connected(String name, String type) { textures.addProperty(name,type); return this; }
  public ConnectedModelBuilder setSides(Direction first, Direction... other) { sides = slimeknights.mantle.util.LogicHelper.append(new Direction[]{first}, other); return this; }
  public ConnectedModelBuilder setPredicate(String predicate) { this.predicate = predicate; return this; }
  @Override public JsonObject toJson(JsonObject json) {
    super.toJson(json); json.addProperty("type", "mantle:connected"); json.add("textures",textures.deepCopy());
    if (predicate != null) { json.addProperty("predicate",predicate); }
    if (sides != null) { JsonArray array = new JsonArray(); for (Direction side:sides) { array.add(side.getSerializedName()); } json.add("sides",array); }
    return json;
  }
}
