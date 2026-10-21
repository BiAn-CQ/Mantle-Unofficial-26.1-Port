package slimeknights.mantle.registration.deferred;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
/** Registers native loot codecs without the wrapper types removed in 26.1. */
public class LootFunctionDeferredRegister extends DeferredRegister<MapCodec<? extends LootItemFunction>> {
  public LootFunctionDeferredRegister(String namespace) { super(Registries.LOOT_FUNCTION_TYPE, namespace); }
  public <T extends LootItemFunction> DeferredHolder<MapCodec<? extends LootItemFunction>,MapCodec<T>> register(String name, MapCodec<T> codec) { return register(name, () -> codec); }
}
