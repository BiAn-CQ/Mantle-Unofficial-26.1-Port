package slimeknights.mantle.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

import java.util.Map;

/**
 * Compatibility loader for the model JSON shape used by the 1.20.1 ports.
 *
 * <p>26.1 moved the old Forge geometry loader API to {@link UnbakedModelLoader}.
 * The gameplay model loaders are being migrated separately, so this loader keeps
 * their standard parent/elements portion usable while ignoring legacy-only
 * extension data.  This is deliberately a model-data compatibility boundary,
 * not a behavior hook.</p>
 */
public final class LegacyModelLoader implements UnbakedModelLoader<UnbakedModel> {
  public static final LegacyModelLoader INSTANCE = new LegacyModelLoader();

  private LegacyModelLoader() {}

  @Override
  public UnbakedModel read(JsonObject json, JsonDeserializationContext context) {
    JsonObject normalized = json.deepCopy();
    String loader = getString(normalized, "loader");

    // Forge composite models contain ordinary child models.  Selecting the
    // first child keeps the resource valid until the native composite model is
    // migrated; all children still use the same standard model codec.
    if ("forge:composite".equals(loader)) {
      JsonElement children = normalized.get("children");
      if (children != null && children.isJsonObject() && !children.getAsJsonObject().entrySet().isEmpty()) {
        Map.Entry<String, JsonElement> first = children.getAsJsonObject().entrySet().iterator().next();
        JsonObject child = first.getValue().isJsonObject() ? first.getValue().getAsJsonObject().deepCopy() : new JsonObject();
        copyIfMissing(child, normalized, "parent");
        copyIfMissing(child, normalized, "textures");
        normalized = child;
      }
    }

    normalized.remove("loader");
    // These fields belong to the pre-26.1 custom model implementations.  The
    // standard cuboid codec must not try to interpret their nested payloads.
    normalized.remove("connection");
    normalized.remove("children");
    normalized.remove("colors");
    normalized.remove("retextured");
    normalized.remove("fluids");
    normalized.remove("fluid");
    normalized.remove("layers");
    normalized.remove("modifier_maps");
    normalized.remove("modifier_roots");
    normalized.remove("first_modifiers");
    normalized.remove("parts");
    normalized.remove("nbt_key");
    normalized.remove("extra_textures_key");
    normalized.remove("gui");

    JsonElement parent = normalized.get("parent");
    if (parent instanceof JsonPrimitive primitive && primitive.isString()) {
      String parentId = primitive.getAsString();
      if ("forge:item/default".equals(parentId)) {
        normalized.addProperty("parent", "minecraft:item/generated");
      } else if ("forge:item/default-tool".equals(parentId)) {
        normalized.addProperty("parent", "minecraft:item/handheld");
      }
    }

    // A few item loaders use the old names "base" or "texture" for the
    // generated item layer.  Preserve their appearance in the standard model.
    JsonElement textures = normalized.get("textures");
    if (textures != null && textures.isJsonObject()) {
      JsonObject textureObject = textures.getAsJsonObject();
      if (!textureObject.has("layer0") && isItemLikeLoader(loader)) {
        JsonElement base = textureObject.get("base");
        if (base == null) {
          base = textureObject.get("texture");
        }
        // Tool and NBT-key models store several possible layers rather than a
        // vanilla layer0.  Until their runtime item-model implementations are
        // available, keep the first declared texture visible instead of
        // silently baking an empty model.
        if (base == null) {
          for (Map.Entry<String, JsonElement> entry : textureObject.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
              base = entry.getValue();
              break;
            }
          }
        }
        if (base != null) {
          textureObject.add("layer0", base.deepCopy());
        }
      }
      if ("tconstruct:tool".equals(loader)) {
        int layer = 0;
        JsonObject sourceTextures = textureObject.deepCopy();
        for (Map.Entry<String, JsonElement> entry : sourceTextures.entrySet()) {
          JsonElement value = entry.getValue();
          if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            textureObject.addProperty("layer" + layer++, value.getAsString());
            if (layer == 5) {
              break;
            }
          }
        }
      }
    }

    return context.deserialize(normalized, CuboidModel.class);
  }

  private static String getString(JsonObject object, String key) {
    JsonElement element = object.get(key);
    return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
           ? element.getAsString() : "";
  }

  private static boolean isItemLikeLoader(String loader) {
    return loader.equals("mantle:item_layer")
           || loader.equals("mantle:nbt_key")
           || loader.equals("tconstruct:tool")
           || loader.equals("tconstruct:material")
           || loader.equals("tconstruct:material_block")
           || loader.equals("tconstruct:fluid_container")
           || loader.equals("tconstruct:fluid_texture")
           || loader.equals("tconstruct:gui")
           || loader.equals("tconstruct:tank");
  }

  private static void copyIfMissing(JsonObject target, JsonObject source, String key) {
    if (!target.has(key) && source.has(key)) {
      target.add(key, source.get(key).deepCopy());
    }
  }
}
