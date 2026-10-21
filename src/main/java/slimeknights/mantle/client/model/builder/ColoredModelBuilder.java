package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.client.model.NativeBlockColorData;
import java.util.ArrayList;
import java.util.List;
/** Builds a native block-state model definition referencing a reusable cuboid model. */
public class ColoredModelBuilder {
  protected final Identifier model;
  protected final List<NativeBlockColorData> colors = new ArrayList<>();
  public ColoredModelBuilder(Identifier model) { this.model = model; }
  public ColoredModelBuilder colorData(NativeBlockColorData value) { colors.add(value); return this; }
  public ColoredModelBuilder color(int color) { return colorData(new NativeBlockColorData(color, -1, null)); }
  public ColoredModelBuilder luminosity(int value) { return colorData(new NativeBlockColorData(-1, value, null)); }
  public JsonObject toJson(JsonObject json) {
    json.addProperty("type", "mantle:colored_block");
    json.addProperty("model", model.toString());
    if (!colors.isEmpty()) { json.add("colors", NativeBlockColorData.CODEC.listOf().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, colors).getOrThrow()); }
    return json;
  }
  public final JsonObject toJson() { return toJson(new JsonObject()); }
}
