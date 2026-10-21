package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMap.Key;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Key for fetching properties from a loadable context. This key doubles as a record field for a required context key.
 * @param <T>  Field type
 */
public class ContextKey<T> implements Key<T> {
  /** Context field representing the object's ID */
  public static final ContextKey<Identifier> ID = new ContextKey<>("id");
  /** Key for adding debug info to log messages. Generally not useful as a field */
  public static final ContextKey<String> DEBUG = new ContextKey<>("debug info");
  /** Key for adding condition context, used in {@link slimeknights.mantle.data.loadable.mapping.ConditionalLoadable} */
  public static final ContextKey<IContext> CONDITION_CONTEXT = new ContextKey<>("condition context");
  /** Registry access used by codecs whose JSON form contains registry tags. */
  public static final ContextKey<RegistryAccess> REGISTRY_ACCESS = new ContextKey<>("registry access");
  /** Registry lookup carried by RegistryOps during codec-backed data reloads. */
  public static final ContextKey<RegistryOps.RegistryInfoLookup> REGISTRY_LOOKUP = new ContextKey<>("registry lookup");

  /** Name of the field, used primarily for debug */
  private final String name;

  public ContextKey(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /** Creates context for data generation or a world registry lookup. */
  public static TypedMapBuilder lookupContext(@Nullable HolderLookup.Provider lookup) {
    return TypedMapBuilder.builder().put(REGISTRY_LOOKUP,
      new RegistryOps.HolderLookupAdapter(lookup == null ? RegistryAccess.EMPTY : lookup));
  }

  /** Preserves the registry and condition information carried by a native codec. */
  public static <O> TypedMapBuilder opsContext(DynamicOps<O> ops) {
    TypedMapBuilder builder = TypedMapBuilder.builder();
    if (ops instanceof RegistryOps<?> registryOps) {
      builder.put(REGISTRY_LOOKUP, registryOps.lookupProvider);
    }
    if (ops instanceof ConditionalOps<?>) {
      ConditionalOps.retrieveContext().codec().parse(ops, ops.emptyMap()).result()
        .ifPresent(condition -> builder.put(CONDITION_CONTEXT, condition));
    }
    return builder;
  }

  /** Context supplied by the native codec around an older Gson adapter. */
  public static TypedMap currentContext() {
    DynamicOps<?> ops = IngredientLoadable.activeOps();
    return ops == null ? TypedMap.EMPTY : opsContext(ops).build();
  }

  /** Includes a legacy Gson codec's context while keeping explicit keys authoritative. */
  public static TypedMap currentContext(TypedMap context) {
    TypedMap current = currentContext();
    return current.isEmpty() ? context : TypedMapBuilder.builder().putAll(current).putAll(context).build();
  }

  /** Applies explicit context without replacing the caller's native operations when absent. */
  public static <O> DynamicOps<O> withRegistryContext(DynamicOps<O> ops, TypedMap context) {
    RegistryOps.RegistryInfoLookup lookup = context.get(REGISTRY_LOOKUP);
    if (lookup == null) {
      RegistryAccess access = context.get(REGISTRY_ACCESS);
      if (access != null) {
        lookup = new RegistryOps.HolderLookupAdapter(access);
      }
    }
    RegistryOps<O> registryOps = lookup != null ? RegistryOps.create(ops, lookup)
      : ops instanceof RegistryOps<O> existing ? existing : null;
    IContext conditions = context.get(CONDITION_CONTEXT);
    if (conditions != null) {
      if (registryOps == null) {
        registryOps = RegistryOps.create(ops, RegistryAccess.EMPTY);
      }
      return new ConditionalOps<>(registryOps, conditions);
    }
    return registryOps == null ? ops : registryOps;
  }

  public static DynamicOps<JsonElement> optionalRegistryOps(TypedMap context) {
    return withRegistryContext(JsonOps.INSTANCE, currentContext(context));
  }

  public static RegistryOps<JsonElement> createRegistryOps(TypedMap context, ErrorFactory error) {
    TypedMap resolved = currentContext(context);
    if (resolved.get(REGISTRY_LOOKUP) == null && resolved.get(REGISTRY_ACCESS) == null) {
      throw error.create("Missing registry lookup in loadable context");
    }
    DynamicOps<JsonElement> ops = withRegistryContext(JsonOps.INSTANCE, resolved);
    if (ops instanceof RegistryOps<JsonElement> registryOps) {
      return registryOps;
    }
    throw error.create("Missing registry lookup in loadable context");
  }



  @Override
  public String toString() {
    return "ContextKey('" + name + "')'";
  }


  /* Fields */
  private RecordField<T,Object> requiredField;
  private RecordField<T,Object> nullableField;

  /** Gets a field requiring this context parameter */
  public RecordField<T,Object> requiredField() {
    if (requiredField == null) {
      requiredField = new Required<>(this, (t,e) -> t);
    }
    return requiredField;
  }

  /** Gets a field requiring this context parameter, but mapped using the passed function */
  public <M> RecordField<M,Object> mappedField(BiFunction<T,ErrorFactory,M> mapper) {
    return new Required<>(this, mapper);
  }

  /** Field using null in place of this parameter if missing */
  public RecordField<T,Object> nullableField() {
    if (nullableField == null) {
      nullableField = new Defaulting<>(this, null);
    }
    return nullableField;
  }

  /** Creates a defaulting field for this key */
  public RecordField<T,Object> defaultField(T defaultValue) {
    return new Defaulting<>(this, defaultValue);
  }


  /** Field instance for making the key required */
  private record Required<T,M>(ContextKey<T> key, BiFunction<T,ErrorFactory,M> mapper) implements ContextField<M> {
    @Override
    public M get(TypedMap context, ErrorFactory error) {
      T value = context.get(key);
      if (value != null) {
        return mapper.apply(value, error);
      }
      throw error.create("Unable to fetch " + key.name + " from context, this usually implements a broken JSON deserializer");
    }
  }

  /** Field instance that defaults to a given value */
  private record Defaulting<T>(ContextKey<T> key, @Nullable T defaultValue) implements ContextField<T> {
    @Override
    public T get(TypedMap context, ErrorFactory error) {
      // it is potentially faster to use get over getOrDefault so call it directly if we have no default value
      if (defaultValue == null) {
        return context.get(key);
      }
      return context.getOrDefault(key, defaultValue);
    }
  }
}
