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
        JsonElement serialized = loadable.serialize(recipe, context(ops));
        if (!serialized.isJsonObject()) {
          throw new JsonParseException("RecordLoadable serialized a recipe to a non-object value");
        }
        return serialized;
      }

      private TypedMap context(DynamicOps<?> ops) {
        return buildContext(serializerReference.get(), type, ops);
      }

      @Override
      public String codecError() {
        return "Mantle loadable recipe";
      }
    };

    MapCodec<T> mapCodec = MapCodec.assumeMapUnsafe(codec);
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.of(
      (buffer, recipe) -> loadable.encode(buffer, recipe, buildContext(serializerReference.get(), type, buffer.registryAccess())),
      buffer -> loadable.decode(buffer, buildContext(serializerReference.get(), type, buffer.registryAccess()))
    );
    RecipeSerializer<T> serializer = new RecipeSerializer<>(mapCodec, streamCodec);
    serializerReference.set(serializer);
    SERIALIZER_INFO.put(serializer, new SerializerInfo<>(loadable, type));
    return serializer;
  }

  /** Builds a recipe directly from its loadable while retaining its data-pack ID. */
  @SuppressWarnings("unchecked")
  public static <T extends Recipe<?>> T fromJson(RecipeSerializer<? extends T> serializer, Identifier id, JsonObject json) {
    SerializerInfo<?> rawInfo = SERIALIZER_INFO.get(serializer);
    if (rawInfo == null) {
      throw new IllegalArgumentException("Recipe serializer is not backed by a Mantle record loadable: " + serializer);
    }
    SerializerInfo<T> info = (SerializerInfo<T>) rawInfo;
    TypedMap context = buildContext(serializer, info.type(), json, id);
    return info.loadable().deserialize(json, context);
  }

  private static TypedMap buildContext(RecipeSerializer<?> serializer, Supplier<? extends RecipeType<?>> type, Object opsOrAccess) {
    return buildContext(serializer, type, opsOrAccess, UNBOUND_RECIPE_ID);
  }

  private static TypedMap buildContext(RecipeSerializer<?> serializer, Supplier<? extends RecipeType<?>> type, Object opsOrAccess, Identifier id) {
    TypedMapBuilder builder = TypedMapBuilder.builder()
      .put(ContextKey.ID, id)
      .put(ContextKey.DEBUG, "Recipe of type " + net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer))
      .put(SERIALIZER, serializer);
    if (opsOrAccess instanceof DynamicOps<?> ops) {
      builder.putAll(ContextKey.opsContext(ops).build());
    } else if (opsOrAccess instanceof net.minecraft.core.RegistryAccess registryAccess) {
      builder.put(ContextKey.REGISTRY_ACCESS, registryAccess);
    }
    if (type != null) {
      builder.put(TYPE, type.get());
    }
    return builder.build();
  }

  private record SerializerInfo<T extends Recipe<?>>(RecordLoadable<T> loadable,
                                                       Supplier<? extends RecipeType<?>> type) {}
}
