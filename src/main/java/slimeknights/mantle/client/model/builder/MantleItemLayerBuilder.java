package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.model.sprite.Material;
import slimeknights.mantle.client.model.NativeMantleItemModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/** Native item-model data generator. Its JSON belongs under assets/<mod>/items/. */
public class MantleItemLayerBuilder {
  protected Identifier parent = Identifier.withDefaultNamespace("item/generated");
  protected final Map<String,Material> textures = new HashMap<>();
  private final java.util.List<NativeMantleItemModel.LayerData> layers = new ArrayList<>();
  public MantleItemLayerBuilder() {}
  public MantleItemLayerBuilder parent(Identifier parent) { this.parent = parent; return this; }
  public MantleItemLayerBuilder texture(String name, Identifier texture) { textures.put(name,new Material(texture)); return this; }
  public MantleItemLayerBuilder addLayer(NativeMantleItemModel.LayerData data) { layers.add(data); return this; }
  public MantleItemLayerBuilder color(int color) { return addLayer(new NativeMantleItemModel.LayerData(color,0,false)); }
  public MantleItemLayerBuilder luminosity(int value) { return addLayer(new NativeMantleItemModel.LayerData(-1,value,false)); }
  public JsonObject toJson(JsonObject json) {
    json.addProperty("type", "mantle:item_layer"); json.addProperty("parent",parent.toString());
    json.add("textures",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, Material.CODEC).encodeStart(com.mojang.serialization.JsonOps.INSTANCE,textures).getOrThrow());
    JsonArray data = new JsonArray(); for (var layer:layers) { JsonObject value=new JsonObject(); value.addProperty("color",String.format(java.util.Locale.ROOT,"#%08X",layer.color())); value.addProperty("luminosity",layer.luminosity()); value.addProperty("no_tint",layer.noTint()); data.add(value); } if (!layers.isEmpty()) { json.add("layers",data); }
    return json;
  }
  public JsonObject toJson() { return toJson(new JsonObject()); }
}
