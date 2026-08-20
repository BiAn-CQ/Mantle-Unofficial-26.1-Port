package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class LoadableRecipeSerializer {
  private static final Identifier UNBOUND_RECIPE_ID = Mantle.getResource("codec_unbound_recipe");
  private static final Map<RecipeSerializer<?>,SerializerInfo<?>> SERIALIZER_INFO =
    Collections.synchronizedMap(new IdentityHashMap<>());

  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP =
    StringLoadable.DEFAULT.defaultField("group", "", Recipe::group);

  private LoadableRecipeSerializer() {}

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return create(loadable, null, null);
  }

  public static <T extends R, R extends Recipe<?>> RecipeSerializer<T> of(
    RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return create(loadable, type, null);
  }

  /** Creates a serializer that is deprecated, logging a warning when used */
  public static <T extends Recipe<?>> RecipeSerializer<T> deprecated(RecordLoadable<T> loadable, String replacement) {
    return create(loadable, null, replacement);
  }

  /** Helper class that logs a warning on recipe parse about planned removal */
  private static <T extends Recipe<?>> RecipeSerializer<T> create(
    RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type, String replacement) {
    AtomicReference<RecipeSerializer<T>> serializerReference = new AtomicReference<>();
    AtomicReference<TypeAwareRecipeSerializer<T>> typeAwareReference = new AtomicReference<>();

    JsonCodec<T> codec = new JsonCodec<>() {
      @Override
      public T deserialize(JsonElement element, DynamicOps<?> ops) {
        if (!element.isJsonObject()) {
          throw new JsonParseException("Expected recipe to be a JSON object");
        }
        T recipe = loadable.deserialize(element.getAsJsonObject(), context(ops));
        if (replacement != null) {
          Mantle.logger.warn("Loaded a deprecated recipe serializer; {}", replacement);
        }
        return recipe;
      }

      @Override
      public JsonElement serialize(T recipe, DynamicOps<?> ops) {
        JsonElement serialized = loadable.serialize(recipe);
        if (!serialized.isJsonObject()) {
          throw new JsonParseException("RecordLoadable serialized a recipe to a non-object value");
        }
        return serialized;
      }

      private TypedMap context(DynamicOps<?> ops) {
        return buildContext(serializerReference.get(), typeAwareReference.get(), type, ops);
      }

      @Override
      public String codecError() {
        return "Mantle loadable recipe";
      }
    };

    MapCodec<T> mapCodec = MapCodec.assumeMapUnsafe(codec);
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.of(
      (buffer, recipe) -> loadable.encode(buffer, recipe),
      buffer -> loadable.decode(buffer, buildContext(serializerReference.get(), typeAwareReference.get(), type, buffer.registryAccess()))
    );
    RecipeSerializer<T> serializer = new RecipeSerializer<>(mapCodec, streamCodec);
    serializerReference.set(serializer);
    TypeAwareRecipeSerializer<T> typeAware = null;
    if (type != null) {
      typeAware = new TypeAwareInfo<>(serializer, type);
      typeAwareReference.set(typeAware);
    }
    SERIALIZER_INFO.put(serializer, new SerializerInfo<>(loadable, type, typeAware));
    return serializer;
  }

  /**
   * Recovers the type-aware view associated with a serializer created by
   * {@link #of(RecordLoadable, Supplier)}. NeoForge's deferred registry exposes
   * the native serializer type, so data builders use this bridge when they also
   * need the recipe type.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Recipe<?>> TypeAwareRecipeSerializer<T> typeAware(RecipeSerializer<? extends T> serializer) {
    SerializerInfo<?> info = SERIALIZER_INFO.get(serializer);
    if (info == null || info.typeAware() == null) {
      throw new IllegalArgumentException("Recipe serializer is not backed by a type-aware Mantle loadable: " + serializer);
    }
    return (TypeAwareRecipeSerializer<T>) info.typeAware();
  }

  /** Builds a recipe directly from its loadable while retaining its data-pack ID. */
  @SuppressWarnings("unchecked")
  public static <T extends Recipe<?>> T fromJson(RecipeSerializer<? extends T> serializer, Identifier id, JsonObject json) {
    SerializerInfo<?> rawInfo = SERIALIZER_INFO.get(serializer);
    if (rawInfo == null) {
      throw new IllegalArgumentException("Recipe serializer is not backed by a Mantle record loadable: " + serializer);
    }
    SerializerInfo<T> info = (SerializerInfo<T>) rawInfo;
    TypedMap context = buildContext(serializer, info.typeAware(), info.type(), json, id);
    return info.loadable().deserialize(json, context);
  }

  private static TypedMap buildContext(RecipeSerializer<?> serializer, TypeAwareRecipeSerializer<?> typeAware,
                                       Supplier<? extends RecipeType<?>> type, Object opsOrAccess) {
    return buildContext(serializer, typeAware, type, opsOrAccess, UNBOUND_RECIPE_ID);
  }

  private static TypedMap buildContext(RecipeSerializer<?> serializer, TypeAwareRecipeSerializer<?> typeAware,
                                       Supplier<? extends RecipeType<?>> type, Object opsOrAccess, Identifier id) {
    TypedMapBuilder builder = TypedMapBuilder.builder()
      .put(ContextKey.ID, id)
      .put(ContextKey.DEBUG, "Recipe decoded through a 26.1 codec")
      .put(SERIALIZER, serializer);
    if (opsOrAccess instanceof RegistryOps<?> registryOps) {
      builder.put(ContextKey.REGISTRY_LOOKUP, registryOps.lookupProvider);
    } else if (opsOrAccess instanceof net.minecraft.core.RegistryAccess registryAccess) {
      builder.put(ContextKey.REGISTRY_ACCESS, registryAccess);
    }
    if (type != null && typeAware != null) {
      builder.put(TYPE, type.get()).put(TYPED_SERIALIZER, typeAware);
    }
    return builder.build();
  }

  private record TypeAwareInfo<T extends Recipe<?>>(RecipeSerializer<T> serializer,
                                                     Supplier<? extends RecipeType<?>> type)
    implements TypeAwareRecipeSerializer<T> {
    @Override
    public RecipeType<?> getType() {
      return type.get();
    }
  }

  private record SerializerInfo<T extends Recipe<?>>(RecordLoadable<T> loadable,
                                                       Supplier<? extends RecipeType<?>> type,
                                                       TypeAwareRecipeSerializer<T> typeAware) {}
}
