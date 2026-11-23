package slimeknights.mantle.loot;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import slimeknights.mantle.Mantle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loads injections before the reloadable loot-table registry is decoded. */
public final class LootTableInjector implements BakeCallback<LootTableInjection> {
  public static final LootTableInjector INSTANCE = new LootTableInjector();
  public static final ResourceKey<Registry<LootTableInjection>> REGISTRY_KEY = ResourceKey.createRegistryKey(Mantle.getResource("loot_injectors"));
  public static final String FOLDER = "mantle/loot_injectors";
  private Map<Identifier,LootTableInjection> injections = Map.of();

  private LootTableInjector() {}

  public static void init(IEventBus modBus) {
    modBus.addListener(EventPriority.NORMAL, false, DataPackRegistryEvent.NewRegistry.class, INSTANCE::addRegistries);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LootTableLoadEvent.class, INSTANCE::lootTableLoad);
  }

  private void addRegistries(DataPackRegistryEvent.NewRegistry event) {
    event.dataPackRegistry(REGISTRY_KEY, LootTableInjection.CODEC, null, builder -> builder.onBake(this));
  }

  @Override
  public void onBake(Registry<LootTableInjection> registry) {
    Map<Identifier,LootTableInjection.Builder> builders = new HashMap<>();
    for (LootTableInjection injection : registry) {
      LootTableInjection.Builder builder = builders.computeIfAbsent(injection.name(), id -> new LootTableInjection.Builder());
      for (var pool : injection.pools()) builder.addToPool(pool);
    }
    injections = builders.entrySet().stream().map(entry -> entry.getValue().build(entry.getKey()))
      .collect(Collectors.toUnmodifiableMap(LootTableInjection::name, Function.identity()));
    Mantle.logger.info("Loaded {} loot table injectors injecting into {} tables", registry.size(), injections.size());
  }

  private void lootTableLoad(LootTableLoadEvent event) {
    LootTableInjection injection = injections.get(event.getName());
    if (injection == null) return;
    LootTable table = event.getTable();
    for (var poolInjection : injection.pools()) {
      LootPool target = table.getPool(poolInjection.name());
      // 26.1 vanilla chest tables can leave the first pool unnamed. Keep the
      // existing data contract for injections naming that first pool "main".
      if (target == null && "main".equals(poolInjection.name()) && !table.pools.isEmpty()) target = table.pools.getFirst();
      if (target == null) {
        Mantle.logger.warn("Failed to inject loot into {} pool {}", injection.name(), poolInjection.name());
        continue;
      }
      var entries = new ArrayList<>(target.entries);
      entries.addAll(poolInjection.entries());
      target.entries = entries;
    }
  }
}
