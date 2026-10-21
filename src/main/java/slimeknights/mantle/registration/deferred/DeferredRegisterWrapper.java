package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Base logic for a deferred register wrapper
 * @param <T>  Registry type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DeferredRegisterWrapper<T> {
  /** Registry instance, use this to provide register methods */
  protected final SynchronizedDeferredRegister<T> register;
  /** Mod ID for registration */
  protected final String modID;

  protected DeferredRegisterWrapper(ResourceKey<Registry<T>> reg, String modID) {
    this(DeferredRegister.create(reg, modID), modID);
  }

  protected DeferredRegisterWrapper(DeferredRegister<T> register, String modID) {
    this.register = SynchronizedDeferredRegister.create(register);
    this.modID = modID;
  }

  /**
   * Initializes this registry wrapper. Needs to be called during mod construction
   */
  public void register(IEventBus bus) {
    register.register(bus);
  }

  /* Utilities */

  /**
   * Gets a resource location object for the given name
   * @param name  Name
   * @return  Resource location string
   */
  protected Identifier resource(String name) {
    return Identifier.fromNamespaceAndPath(modID, name);
  }

  /**
   * Gets a resource location string for the given name
   * @param name  Name
   * @return  Resource location string
   */
  protected String resourceName(String name) {
    return modID + ":" + name;
  }


  /** Registers a factory with the final registry ID. */
  protected <I extends T> Supplier<? extends I> registerMapped(String name, java.util.function.Function<Identifier,? extends I> factory) {
    return register.register(name, () -> factory.apply(resource(name)));
  }

  /** Registers enum variants with a caller-provided name function. */
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(E[] values, java.util.function.Function<? super E,String> names, java.util.function.Function<E,? extends I> factory) {
    return registerEnum(values, names, (id, value) -> factory.apply(value));
  }
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(E[] values, java.util.function.Function<? super E,String> names, BiFunction<Identifier,E,? extends I> factory) {
    return EnumObject.generate(values, value -> registerMapped(names.apply(value), id -> factory.apply(id, value)));
  }
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(E[] values, String name, java.util.function.Function<E,? extends I> factory) {
    return registerEnum(values, EnumDeferredRegister.suffix(name), factory);
  }
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(E[] values, String name, BiFunction<Identifier,E,? extends I> factory) {
    return registerEnum(values, EnumDeferredRegister.suffix(name), factory);
  }
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(String name, E[] values, java.util.function.Function<E,? extends I> factory) {
    return registerEnum(values, EnumDeferredRegister.prefix(name), factory);
  }
  public <E extends Enum<E>, I extends T> EnumObject<E,I> registerEnum(String name, E[] values, BiFunction<Identifier,E,? extends I> factory) {
    return registerEnum(values, EnumDeferredRegister.prefix(name), factory);
  }

  /* Enum objects */

  /** Gets the name of an enum value */
  protected static String getName(Enum<?> value) {
    return value instanceof StringRepresentable representable ? representable.getSerializedName() : value.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Registers an item with multiple variants, prefixing the name with the value name
   * @param values    Enum values to use for this block
   * @param name      Name of the block
   * @param register  Function to register an entry
   * @return  EnumObject mapping between different block types
   */
  protected static <E extends Enum<E>, V extends T, T> EnumObject<E,V> registerEnumEntries(E[] values, String name, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    // note this cast only works because you cannot extend an enum
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(getName(value) + "_" + name, value));
    }
    return builder.build();
  }

  /**
   * Registers an item with multiple variants, suffixing the name with the value name
   * @param name      Name of the block
   * @param values    Enum values to use for this block
   * @param register  Function to register an entry
   * @return  EnumObject mapping between different block types
   */
  protected static <E extends Enum<E>, V extends T, T> EnumObject<E,V> registerEnumEntries(String name, E[] values, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    // note this cast only works because you cannot extend an enum
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(name + "_" + getName(value), value));
    }
    return builder.build();
  }
}
