package slimeknights.mantle.registration.deferred;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
/** Registers native loot codecs without the wrapper types removed in 26.1. */
public class LootConditionDeferredRegister extends DeferredRegister<MapCodec<? extends LootItemCondition>> {
  public LootConditionDeferredRegister(String namespace) { super(Registries.LOOT_CONDITION_TYPE, namespace); }
  public <T extends LootItemCondition> DeferredHolder<MapCodec<? extends LootItemCondition>,MapCodec<T>> register(String name, MapCodec<T> codec) { return register(name, () -> codec); }
}
