package slimeknights.mantle.registration.deferred;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.mantle.network.codec.MantleStreamCodecs;

/** Typed registration helpers for data components. */
public class DataComponentTypeDeferredRegister extends DeferredRegister.DataComponents {
  public DataComponentTypeDeferredRegister(String namespace) {
    super(Registries.DATA_COMPONENT_TYPE, namespace);
  }

  /** Registers a persistent presence flag synchronized with clients. */
  public DeferredHolder<DataComponentType<?>,DataComponentType<Unit>> registerUnit(String name) {
    return registerComponentType(name, builder -> builder.persistent(Unit.CODEC).networkSynchronized(MantleStreamCodecs.UNIT));
  }
}
