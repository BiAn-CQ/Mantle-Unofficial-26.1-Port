package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.recipe.ingredient.ItemTagIngredient;
import slimeknights.mantle.recipe.ingredient.OrIngredient;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Loadable for ingredients, handling Forge ingredients */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  /** Shared empty ingredient used by optional recipe fields and their network form. */
  public static final Ingredient EMPTY_INGREDIENT = Ingredient.of(HolderSet.emptyNamed(
    BuiltInRegistries.ITEM,
    TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mantle", "__empty"))));

  /** Registry operations active while a Gson-backed codec is decoding. */
  private static final ThreadLocal<DynamicOps<?>> ACTIVE_OPS = new ThreadLocal<>();

  /**
   * Binds the native codec operations for legacy Gson adapters. Gson does not expose
   * DynamicOps to its type adapters, so this small scoped bridge keeps registry tags
   * resolvable without making the adapter global-stateful between reload tasks.
   */
  public static Scope pushOps(DynamicOps<?> ops) {
    DynamicOps<?> previous = ACTIVE_OPS.get();
    ACTIVE_OPS.set(ops);
    return new Scope(previous);
  }

  public static final class Scope implements AutoCloseable {
    private final DynamicOps<?> previous;

    private Scope(DynamicOps<?> previous) {
      this.previous = previous;
    }

    @Override
    public void close() {
      if (previous == null) {
        ACTIVE_OPS.remove();
      } else {
        ACTIVE_OPS.set(previous);
      }
    }
  }

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    RegistryAccess registryAccess = context.get(ContextKey.REGISTRY_ACCESS);
    RegistryOps.RegistryInfoLookup registryLookup = context.get(ContextKey.REGISTRY_LOOKUP);
    if (registryLookup == null && registryAccess == null && ACTIVE_OPS.get() instanceof RegistryOps<?> activeOps) {
      registryLookup = activeOps.lookupProvider;
    }
    var ops = registryLookup != null ? RegistryOps.create(JsonOps.INSTANCE, registryLookup)
                                     : registryAccess == null ? JsonOps.INSTANCE : RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    JsonElement normalized = normalizeLegacyIngredient(element);
    // Resolve a compact root tag before wrapping mixed legacy arrays. A root
    // tag normalizes to ["#namespace:tag"]; treating that single value as a
    // mixed OR creates mantle:or -> "#tag" -> mantle:or forever.
    Ingredient tagIngredient = resolveTagIngredient(normalized);
    if (tagIngredient != null) {
      return tagIngredient;
    }
    normalized = normalizeLegacyIngredientForRecipe(normalized);
    Ingredient combinator = decodeCombinator(normalized, context);
    if (combinator != null) {
      return combinator;
    }
    Ingredient directItems = resolveDirectItems(normalized);
    if (directItems != null) {
      return directItems;
    }
    return Ingredient.CODEC.parse(ops, normalized).getOrThrow();
  }

  /**
   * Decodes combinators recursively so legacy tag objects and compact tag
   * strings use the same root-level tag resolver.  The nested vanilla codec in
   * 26.1 intentionally only accepts holder lists, which is not equivalent to
   * the 1.20.1 Forge combinator JSON.
   */
  private static Ingredient decodeCombinator(JsonElement element, TypedMap context) {
    if (!element.isJsonObject()) {
      return null;
    }
    JsonObject object = element.getAsJsonObject();
    JsonElement typeElement = object.get("neoforge:ingredient_type");
    if (typeElement == null || !typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive().isString()) {
      return null;
    }
    String type = typeElement.getAsString();
    return switch (type) {
      case "neoforge:intersection" -> {
        JsonElement children = object.get("children");
        yield children != null && children.isJsonArray()
          ? new IntersectionIngredient(decodeChildren(children.getAsJsonArray(), context)).toVanilla()
          : null;
      }
      case "neoforge:difference" -> {
        JsonElement base = object.get("base");
        JsonElement subtracted = object.get("subtracted");
        yield base != null && subtracted != null
          ? new DifferenceIngredient(ALLOW_EMPTY.convert(base, "base", context), ALLOW_EMPTY.convert(subtracted, "subtracted", context)).toVanilla()
          : null;
      }
      case "neoforge:compound" -> {
        JsonElement children = object.has("children") ? object.get("children") : object.get("ingredients");
        yield children != null && children.isJsonArray()
          ? new CompoundIngredient(decodeChildren(children.getAsJsonArray(), context)).toVanilla()
          : null;
      }
      case "mantle:or" -> {
        JsonElement ingredients = object.get("ingredients");
        yield ingredients != null && ingredients.isJsonArray()
          ? new OrIngredient(decodeChildren(ingredients.getAsJsonArray(), context)).toVanilla()
          : null;
      }
      default -> null;
    };
  }

  private static List<Ingredient> decodeChildren(JsonArray values, TypedMap context) {
    List<Ingredient> children = new ArrayList<>(values.size());
    for (JsonElement value : values) {
      children.add(ALLOW_EMPTY.convert(value, "ingredient", context));
    }
    return List.copyOf(children);
  }

  /** Resolves a legacy root tag without making a missing optional compatibility tag fatal. */
  private static Ingredient resolveTagIngredient(JsonElement element) {
    String tagName = null;
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
      tagName = element.getAsString();
    } else if (element.isJsonArray() && element.getAsJsonArray().size() == 1) {
      JsonElement child = element.getAsJsonArray().get(0);
      if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
        tagName = child.getAsString();
      }
    }
    if (tagName == null || !tagName.startsWith("#")) {
      return null;
    }
    Identifier id = Identifier.tryParse(tagName.substring(1));
    if (id == null) {
      return null;
    }
    TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
    return ItemTagIngredient.of(tag);
  }

  /** Resolves compact item holder arrays without requiring a RegistryOps context. */
  private static Ingredient resolveDirectItems(JsonElement element) {
    if (!element.isJsonArray() || element.getAsJsonArray().isEmpty()) {
      return null;
    }
    List<Item> items = new ArrayList<>(element.getAsJsonArray().size());
    for (JsonElement value : element.getAsJsonArray()) {
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
        return null;
      }
      String name = value.getAsString();
      if (name.startsWith("#")) {
        return null;
      }
      Identifier id = Identifier.tryParse(name);
      if (id == null) {
        return null;
      }
      var item = BuiltInRegistries.ITEM.getOptional(id);
      if (item.isEmpty()) {
        return null;
      }
      items.add(item.orElseThrow());
    }
    return Ingredient.of(items.stream());
  }

  /**
   * Vanilla's 26.1 holder-set codec accepts a tag at the root, but a list is a
   * list of item holders and no longer accepts {@code #tag} entries mixed into
   * that list.  Legacy Forge ingredients used exactly that mixed-list form for
   * OR ingredients, so keep the semantics through Mantle's custom OR type.
   */
  private static boolean containsLegacyTagOrCustom(JsonArray values) {
    if (values.isEmpty()) {
      return false;
    }
    for (JsonElement value : values) {
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
          && value.getAsString().startsWith("#")) {
        return true;
      }
      if (value.isJsonObject() || value.isJsonArray()) {
        return true;
      }
    }
    return false;
  }

  /** Wraps legacy mixed OR values in the registered Mantle custom ingredient. */
  private static JsonObject wrapLegacyOr(JsonArray values) {
    JsonObject object = new JsonObject();
    object.addProperty("neoforge:ingredient_type", "mantle:or");
    JsonArray ingredients = new JsonArray();
    for (JsonElement value : values) {
      ingredients.add(normalizeLegacyIngredient(value, true));
    }
    object.add("ingredients", ingredients);
    return object;
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    // Custom ingredients deliberately do not expose a vanilla HolderSet.
    // Serialize them through their registered NeoForge codec before touching
    // getValues(), which throws for every custom ingredient in 26.1.
    if (object.isCustom()) {
      DynamicOps<?> activeOps = ACTIVE_OPS.get();
      return activeOps == null
        ? Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow()
        : serializeWithOps(activeOps, object);
    }
    // A named holder set has a concrete tag identity even while the data
    // generator is still constructing that tag. Ingredient.isEmpty() tries to
    // dereference the set, so only call it for direct/custom ingredients.
    if (this == DISALLOW_EMPTY && object.getValues().unwrapKey().isEmpty() && object.isEmpty()) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    Optional<TagKey<Item>> namedTag = object.getValues().unwrapKey();
    if (namedTag.isPresent()) {
      return new JsonPrimitive("#" + namedTag.orElseThrow().location());
    }
    DynamicOps<?> activeOps = ACTIVE_OPS.get();
    if (activeOps != null) {
      return serializeWithOps(activeOps, object);
    }
    return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow();
  }

  /** Encodes an ingredient with the registry context supplied to a legacy Gson adapter. */
  private static <T> JsonElement serializeWithOps(DynamicOps<T> ops, Ingredient object) {
    T encoded = Ingredient.CODEC.encodeStart(ops, object).getOrThrow();
    return ops.convertTo(JsonOps.INSTANCE, encoded);
  }

  @Override
  public Ingredient decode(FriendlyByteBuf buffer, TypedMap context) {
    if (this == ALLOW_EMPTY) {
      return buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf)buffer) : EMPTY_INGREDIENT;
    }
    return Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf)buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Ingredient object) {
    if (this == ALLOW_EMPTY) {
      boolean present = object != EMPTY_INGREDIENT;
      buffer.writeBoolean(present);
      if (!present) {
        return;
      }
      Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf)buffer, materializeForNetwork(object));
      return;
    }
    Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf)buffer, materializeForNetwork(object));
  }

  /**
   * Recipe payloads are decoded before the client has bound its named item
   * tags.  Send the currently resolved item holders instead of a lazy tag
   * holder set; the server remains authoritative for matching.
   */
  private static Ingredient materializeForNetwork(Ingredient ingredient) {
    try {
      return Ingredient.of(ingredient.items().map(holder -> (ItemLike) holder.value()));
    } catch (UnsupportedOperationException exception) {
      return ingredient;
    }
  }

  /**
   * Converts the 1.20.1 object form ({@code {"item":"..."}} or
   * {@code {"tag":"..."}}) to the compact 26.1 ingredient form.
   */
  public static JsonElement normalizeLegacyIngredient(JsonElement element) {
    return normalizeLegacyIngredient(element, false);
  }

  /**
   * Normalizes an ingredient for a native recipe codec. Unlike the generic
   * form, this also converts a mixed legacy OR array to Mantle's registered
   * combinator. Keeping this separate avoids wrapping the child list inside
   * an already-custom Mantle ingredient a second time.
   */
  public static JsonElement normalizeLegacyIngredientForRecipe(JsonElement element) {
    JsonElement normalized = normalizeLegacyIngredient(element);
    return normalized.isJsonArray() && containsLegacyTagOrCustom(normalized.getAsJsonArray())
      ? wrapLegacyOr(normalized.getAsJsonArray()) : normalized;
  }

  /**
   * Normalizes one ingredient value.  Direct item holders in 26.1 are encoded
   * as a list, while an item inside that list remains a single string.
   */
  private static JsonElement normalizeLegacyIngredient(JsonElement element, boolean listElement) {
    if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("type") && object.get("type").isJsonPrimitive()
          && object.getAsJsonPrimitive("type").isString() && !object.has("neoforge:ingredient_type")) {
        // Forge 1.20.1 custom ingredients used the generic `type` discriminator.
        // NeoForge 26.1 reserves that field for holder-set syntax and dispatches
        // custom ingredients through its namespaced discriminator instead.
        JsonObject custom = object.deepCopy();
        String type = custom.remove("type").getAsString();
        String modernType = switch (type) {
          case "forge:intersection", "neoforge:intersection" -> "neoforge:intersection";
          case "forge:difference", "neoforge:difference" -> "neoforge:difference";
          case "forge:compound", "neoforge:compound" -> "neoforge:compound";
          default -> type;
        };
        custom.addProperty("neoforge:ingredient_type", modernType);
        normalizeCustomIngredientFields(custom);
        // Tinkers' 1.20.1 no-container ingredient accepted the nested
        // ingredient directly as `item`/`tag`, while its 26.1 codec exposes
        // the same value through the `match` field.  Normalize both forms
        // before the native custom-ingredient codec sees them.
        if ("tconstruct:no_container".equals(modernType)) {
          if (!custom.has("match")) {
            if (custom.has("item")) {
              JsonElement item = custom.remove("item");
              custom.add("match", normalizeLegacyIngredient(normalizeLegacyItemId(item)));
            } else if (custom.has("tag")) {
              String tag = custom.remove("tag").getAsString();
              custom.addProperty("match", "#" + tag);
            }
          } else {
            custom.add("match", normalizeLegacyIngredient(custom.get("match")));
          }
        }
        return custom;
      }
      if (object.has("item") && (object.size() == 1 || isSizedIngredientObject(object))) {
        JsonElement item = normalizeLegacyItemId(object.get("item"));
        if (listElement) {
          return item;
        }
        JsonArray values = new JsonArray();
        values.add(item);
        return values;
      }
      if (object.has("tag") && (object.size() == 1 || isSizedIngredientObject(object))) {
        JsonObject tagIngredient = new JsonObject();
        tagIngredient.addProperty("neoforge:ingredient_type", "mantle:item_tag");
        tagIngredient.addProperty("tag", object.get("tag").getAsString());
        return tagIngredient;
      }
      return element;
    }
    if (element.isJsonArray()) {
      JsonArray normalized = new JsonArray();
      for (JsonElement child : element.getAsJsonArray()) {
        normalized.add(normalizeLegacyIngredient(child, true));
      }
      return normalized;
    }
    if (listElement || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      return normalizeLegacyItemId(element);
    }
    JsonArray values = new JsonArray();
    values.add(normalizeLegacyItemId(element));
    return values;
  }

  /** Renames vanilla item IDs removed or split by the 26.1 registry update. */
  private static JsonElement normalizeLegacyItemId(JsonElement element) {
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
      String id = element.getAsString();
      if ("minecraft:chain".equals(id)) {
        return new com.google.gson.JsonPrimitive("minecraft:iron_chain");
      }
      if ("minecraft:scute".equals(id)) {
        return new com.google.gson.JsonPrimitive("minecraft:turtle_scute");
      }
    }
    return element;
  }

  /** Legacy SizedIngredient stores its amount beside the direct item/tag. */
  private static boolean isSizedIngredientObject(JsonObject object) {
    return object.size() == 2 && object.has("amount_needed");
  }

  /** Normalizes ingredient children nested inside Forge/NeoForge combinators. */
  private static void normalizeCustomIngredientFields(JsonObject object) {
    for (String field : new String[]{"children", "ingredients"}) {
      JsonElement value = object.get(field);
      if (value != null && value.isJsonArray()) {
        JsonArray normalized = new JsonArray();
        for (JsonElement child : value.getAsJsonArray()) {
          normalized.add(normalizeLegacyIngredient(child));
        }
        object.add(field, normalized);
      }
    }
    for (String field : new String[]{"base", "subtracted"}) {
      JsonElement value = object.get(field);
      if (value != null) {
        object.add(field, normalizeLegacyIngredient(value));
      }
    }
  }
}
