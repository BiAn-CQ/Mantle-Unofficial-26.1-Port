package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.function.Function;
import java.util.function.Supplier;

/** Generic deferred register for an object using registry objects and wanting enums */
public class EnumDeferredRegister<T> extends DeferredRegisterWrapper<T> {
  public EnumDeferredRegister(ResourceKey<Registry<T>> reg, String modID) {
    super(reg, modID);
  }

  /** Registers a standard object */
  public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> value) {
    return register.register(name, value);
  }

  public <I extends T> DeferredHolder<T,I> register(String name, Function<net.minecraft.resources.Identifier,? extends I> factory) {
    return register.register(name, () -> factory.apply(resource(name)));
  }
  public static Function<Enum<?>,String> prefix(String prefix) { return value -> prefix + '_' + getName(value); }
  public static Function<Enum<?>,String> suffix(String suffix) { return value -> getName(value) + '_' + suffix; }
}
