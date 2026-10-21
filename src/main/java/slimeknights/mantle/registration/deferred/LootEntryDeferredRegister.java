package slimeknights.mantle.registration.deferred;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
/** Registers native loot codecs without the wrapper types removed in 26.1. */
public class LootEntryDeferredRegister extends DeferredRegister<MapCodec<? extends LootPoolEntryContainer>> {
  public LootEntryDeferredRegister(String namespace) { super(Registries.LOOT_POOL_ENTRY_TYPE, namespace); }
  public <T extends LootPoolEntryContainer> DeferredHolder<MapCodec<? extends LootPoolEntryContainer>,MapCodec<T>> register(String name, MapCodec<T> codec) { return register(name, () -> codec); }
}
