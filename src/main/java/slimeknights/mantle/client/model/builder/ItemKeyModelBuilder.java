package slimeknights.mantle.client.model.builder;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.client.model.ItemKey;
public class ItemKeyModelBuilder extends MantleItemLayerBuilder {
  private ItemKey key;
  private Identifier extraTexturesKey;
  public ItemKeyModelBuilder key(ItemKey key) { this.key=key; return this; }
  public ItemKeyModelBuilder extraTexturesKey(Identifier id) { extraTexturesKey=id; return this; }
  @Override public JsonObject toJson(JsonObject json) {
    if (key==null) { throw new IllegalStateException("Must set key to use ItemKeyModel"); }
    super.toJson(json); json.addProperty("type","mantle:item_key"); json.add("key",ItemKey.LOADER.serialize(key));
    if (extraTexturesKey!=null) { json.addProperty("extra_textures_key",extraTexturesKey.toString()); }
    return json;
  }
}
