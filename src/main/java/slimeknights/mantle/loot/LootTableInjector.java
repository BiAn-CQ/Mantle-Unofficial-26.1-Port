package slimeknights.mantle.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.JsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/** Class handling injecting additional entries into loot tables */
public final class LootTableInjector extends ContextAwareReloadListener {
  public static final LootTableInjector INSTANCE = new LootTableInjector();
  /** Datapack folder for the injector */
  public static final String FOLDER = "mantle/loot_injectors";
  private static final Identifier LISTENER_ID = Mantle.getResource("loot_table_injectors");
  private static final String CONDITIONAL_VALUE_KEY = "neoforge:value";
  private static final Codec<java.util.Optional<LootTableInjection>> CONDITIONAL_CODEC =
    ConditionalOps.createConditionalCodec(LootTableInjection.CODEC);

  /** Map of injections to use on loot table load */
  private volatile Map<Identifier,RawLootTableInjection> injections = Collections.emptyMap();

  private LootTableInjector() {}

  /** Initializes the loot table injector */
  public static void init() {
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddServerReloadListenersEvent.class,
      event -> event.addListener(LISTENER_ID, INSTANCE));
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LootTableLoadEvent.class, INSTANCE::lootTableLoad);
  }

  @Override
  public CompletableFuture<Void> reload(PreparableReloadListener.SharedState state, Executor taskExecutor,
                                        PreparableReloadListener.PreparationBarrier barrier, Executor reloadExecutor) {
    return CompletableFuture.supplyAsync(() -> load(state.resourceManager()), taskExecutor)
      .thenCompose(barrier::wait)
      .thenAcceptAsync(loaded -> injections = loaded, reloadExecutor);
  }

  private Map<Identifier,RawLootTableInjection> load(ResourceManager manager) {
    long time = System.nanoTime();
    Map<Identifier,RawBuilder> builders = new HashMap<>();
    int loaded = 0;
    for (Entry<Identifier,Resource> entry : manager.listResources(FOLDER, loc -> loc.getPath().endsWith(".json")).entrySet()) {
      try (Reader reader = entry.getValue().openAsReader()) {
        JsonObject json = GsonHelper.fromJson(JsonHelper.DEFAULT_GSON, reader, JsonObject.class);
        if (json == null) {
          Mantle.logger.error("Couldn't parse loot table injection from {} as it is null", entry.getKey());
          continue;
        }
        if (json.isEmpty()) {
          continue;
        }
        java.util.Optional<LootTableInjection> optional = CONDITIONAL_CODEC.parse(makeConditionalOps(), json)
          .getOrThrow(JsonParseException::new);
        if (optional.isPresent()) {
            // the builder allows us to merge from multiple sources, for efficiency
            // ensures a given table name and pool name both show just once
          LootTableInjection injection = optional.get();
          RawBuilder builder = builders.computeIfAbsent(injection.name(), ignored -> new RawBuilder());
          JsonObject value = json.has(CONDITIONAL_VALUE_KEY)
            ? GsonHelper.getAsJsonObject(json, CONDITIONAL_VALUE_KEY) : json;
          JsonArray pools = GsonHelper.getAsJsonArray(value, "pools");
          for (JsonElement poolElement : pools) {
            JsonObject pool = GsonHelper.convertToJsonObject(poolElement, "pool");
            builder.addToPool(
              GsonHelper.getAsString(pool, "name"),
              GsonHelper.getAsJsonArray(pool, "entries"));
          }
          loaded++;
        }
      } catch (IllegalArgumentException | IOException | JsonParseException ex) {
        Mantle.logger.error("Couldn't parse loot injection from {}", entry.getKey(), ex);
      }
    }
    Map<Identifier,RawLootTableInjection> result = builders.entrySet().stream()
    // build final map
      .map(entry -> entry.getValue().build(entry.getKey()))
      .collect(Collectors.toUnmodifiableMap(RawLootTableInjection::name, injection -> injection));
    // log timer
    Mantle.logger.info("Loaded {} loot table injectors injecting into {} tables in {} ms", loaded, result.size(), (System.nanoTime() - time) / 1_000_000f);
    return result;
  }

  /** Called on loot table load to handle the actual injection */
  private void lootTableLoad(LootTableLoadEvent event) {
    RawLootTableInjection injection = injections.get(event.getName());
    if (injection == null) {
      return;
    }
    try {
      inject(event.getTable(), injection, event.getRegistries());
      Mantle.logger.debug("Injected into {} pools in the table {}", injection.pools().size(), injection.name());
    } catch (JsonParseException ex) {
      Mantle.logger.error("Failed to inject entries into loot table {}", injection.name(), ex);
    }
  }

  private static void inject(LootTable table, RawLootTableInjection injection, HolderLookup.Provider registries) {
    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    for (RawLootPoolInjection poolInjection : injection.pools()) {
      LootPool target = table.getPool(poolInjection.name());
      // Some 26.1 vanilla chest tables dropped the old explicit "main" name.
      // Injection data has always used "main" for the first pool, so retain
      // that data contract when the pool is otherwise unnamed.
      if (target == null && "main".equals(poolInjection.name()) && !table.pools.isEmpty()) {
        target = table.pools.getFirst();
      }
      if (target == null) {
        Mantle.logger.warn("Failed to inject loot into {} pool {}", injection.name(), poolInjection.name());
        continue;
      }

      List<LootPoolEntryContainer> entries = new ArrayList<>(target.entries);
      for (JsonElement rawEntry : poolInjection.entries()) {
        entries.add(LootPoolEntries.CODEC.parse(ops, rawEntry.deepCopy()).getOrThrow(JsonParseException::new));
      }
      target.entries = entries;
    }
  }

  /** Registry-independent injection data. Loot entries must be decoded using the registry set of the
   * loot-table reload currently in progress, not the registry set from the previous reload. */
  private record RawLootPoolInjection(String name, List<JsonElement> entries) {}

  private record RawLootTableInjection(Identifier name, List<RawLootPoolInjection> pools) {}

  private static final class RawBuilder {
    private final Map<String,List<JsonElement>> pools = new LinkedHashMap<>();

    private void addToPool(String name, JsonArray entries) {
      List<JsonElement> target = pools.computeIfAbsent(name, ignored -> new ArrayList<>());
      for (JsonElement entry : entries) {
        target.add(entry.deepCopy());
      }
    }

    private RawLootTableInjection build(Identifier name) {
      List<RawLootPoolInjection> result = pools.entrySet().stream()
        .map(entry -> new RawLootPoolInjection(entry.getKey(), List.copyOf(entry.getValue())))
        .toList();
      return new RawLootTableInjection(name, result);
    }
  }
}
