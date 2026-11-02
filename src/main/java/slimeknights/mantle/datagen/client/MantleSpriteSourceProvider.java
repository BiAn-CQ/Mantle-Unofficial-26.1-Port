package slimeknights.mantle.datagen.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.GenericDataProvider;
import java.util.concurrent.CompletableFuture;

/** Registers the shared fluid directory with the native block atlas. */
public class MantleSpriteSourceProvider extends GenericDataProvider {
  public MantleSpriteSourceProvider(PackOutput output) { super(output, PackOutput.Target.RESOURCE_PACK, "atlases"); }
  public CompletableFuture<?> run(CachedOutput output) {
    JsonObject source = new JsonObject();
    source.addProperty("type", "minecraft:directory");
    source.addProperty("source", "fluid");
    source.addProperty("prefix", "fluid/");
    JsonArray sources = new JsonArray(); sources.add(source);
    JsonObject json = new JsonObject(); json.add("sources", sources);
    return saveJson(output, Identifier.withDefaultNamespace("blocks"), json);
  }
  public String getName() { return "Mantle fluid atlas"; }
}
