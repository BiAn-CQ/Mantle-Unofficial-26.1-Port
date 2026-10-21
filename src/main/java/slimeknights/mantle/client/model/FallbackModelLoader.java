package slimeknights.mantle.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.codec.JsonMapCodec;

import java.util.List;
import java.util.function.Predicate;

/** Selects an available native block-state model before decoding optional model types. */
public final class FallbackModelLoader {
  public static final Identifier ID = Mantle.getResource("fallback");
  private FallbackModelLoader() {}

  public static JsonObject select(List<JsonObject> models, Predicate<String> isLoaded) {
    if (models.size() < 2) { throw new JsonSyntaxException("Fallback model must contain at least 2 models"); }
    for (JsonObject model : models) {
      String required = model.has("fallback_mod_id") ? GsonHelper.getAsString(model, "fallback_mod_id")
        : model.has("type") ? Identifier.parse(GsonHelper.getAsString(model, "type")).getNamespace() : null;
      if (required == null || isLoaded.test(required)) { return model; }
    }
    throw new JsonSyntaxException("All fallback model conditions failed");
  }

  public record Unbaked(BlockStateModel.Unbaked selected, List<JsonObject> models) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = new JsonMapCodec<>() {
      @Override protected Unbaked deserialize(JsonObject json, DynamicOps<?> ops) {
        List<JsonObject> models = GsonHelper.getAsJsonArray(json, "models").asList().stream().map(JsonElement::getAsJsonObject).toList();
        JsonObject selected = select(models, mod -> ModList.get().isLoaded(mod));
        return new Unbaked(BlockStateModel.Unbaked.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, selected).getOrThrow(JsonSyntaxException::new), models);
      }
      @Override protected JsonObject serialize(Unbaked value, DynamicOps<?> ops) {
        JsonObject result = new JsonObject();
        com.google.gson.JsonArray models = new com.google.gson.JsonArray();
        value.models.forEach(models::add);
        result.add("models", models);
        return result;
      }
    };
    @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { selected.resolveDependencies(resolver); }
    @Override public BlockStateModel bake(ModelBaker baker) { return selected.bake(baker); }
    @Override public MapCodec<Unbaked> codec() { return MAP_CODEC; }
  }
}
