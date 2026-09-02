package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;

/** Loadable backed by the native Minecraft/NeoForge ingredient codec. */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  /** Shared empty ingredient used by optional recipe fields and their network form. */
  public static final Ingredient EMPTY_INGREDIENT = Ingredient.of(HolderSet.emptyNamed(
    BuiltInRegistries.ITEM,
    TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mantle", "__empty"))));

  /** Registry operations active while a Gson-backed codec is decoding. */
  private static final ThreadLocal<DynamicOps<?>> ACTIVE_OPS = new ThreadLocal<>();
  /** Built-in registry context for standalone Gson callers such as client book data. */
  private static final RegistryAccess BUILTIN_REGISTRY_ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

  /**
   * Binds native codec operations for Gson adapters. Gson does not expose
   * DynamicOps, so the adapter carries the current registry lookup explicitly.
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
    DynamicOps<JsonElement> ops = registryLookup != null ? RegistryOps.create(JsonOps.INSTANCE, registryLookup)
      : RegistryOps.create(JsonOps.INSTANCE, registryAccess == null ? BUILTIN_REGISTRY_ACCESS : registryAccess);
    return Ingredient.CODEC.parse(ops, element).getOrThrow();
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    // Custom ingredients deliberately do not expose a vanilla HolderSet.
    if (object.isCustom()) {
      DynamicOps<?> activeOps = ACTIVE_OPS.get();
      return activeOps == null
        ? Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow()
        : serializeWithOps(activeOps, object);
    }
    // A named holder set has a concrete tag identity even while datagen is
    // still constructing that tag. Only direct ingredients may be empty here.
    if (this == DISALLOW_EMPTY && object.getValues().unwrapKey().isEmpty() && object.isEmpty()) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    Optional<TagKey<Item>> namedTag = object.getValues().unwrapKey();
    if (namedTag.isPresent()) {
      return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow();
    }
    DynamicOps<?> activeOps = ACTIVE_OPS.get();
    return activeOps == null
      ? Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow()
      : serializeWithOps(activeOps, object);
  }

  /** Encodes an ingredient with the registry context supplied to a Gson adapter. */
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
   * Recipe payloads are decoded before the client has bound named item tags.
   * Send resolved item holders; the server remains authoritative for matching.
   */
  private static Ingredient materializeForNetwork(Ingredient ingredient) {
    try {
      return Ingredient.of(ingredient.items().map(holder -> (ItemLike) holder.value()));
    } catch (UnsupportedOperationException exception) {
      return ingredient;
    }
  }
}
