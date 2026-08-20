package slimeknights.mantle.loot;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.CodecLoadable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Record holding a list of entries to inject into the given loot table
 */
public record LootTableInjection(Identifier name, List<LootPoolInjection> pools) {
  public static final Codec<LootTableInjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Identifier.CODEC.fieldOf("name").forGetter(LootTableInjection::name),
    LootPoolInjection.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("pools").forGetter(LootTableInjection::pools)
  ).apply(instance, LootTableInjection::new));

  public static final Loadable<LootTableInjection> LOADABLE = new CodecLoadable<>(CODEC);

  public record LootPoolInjection(String name, List<LootPoolEntryContainer> entries) {
    public static final Codec<LootPoolInjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("name").forGetter(LootPoolInjection::name),
      LootPoolEntries.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("entries").forGetter(LootPoolInjection::entries)
    ).apply(instance, LootPoolInjection::new));

    public static final Loadable<LootPoolInjection> LOADABLE = new CodecLoadable<>(CODEC);

  /**
   * Record holding a list of entries to inject into the given pool
   */
    public LootPoolInjection(String name, LootPoolEntryContainer[] entries) {
      this(name, List.of(entries));
    }
  }

  /** Builder instance for a loot table injection */
  public static class Builder {
    private final Map<String,List<LootPoolEntryContainer>> pools = new LinkedHashMap<>();

    /** Inserts the given entries into the pool */
    @CanIgnoreReturnValue
    public Builder addToPool(String name, LootPoolEntryContainer... entries) {
      Collections.addAll(pools.computeIfAbsent(name, ignored -> new ArrayList<>()), entries);
      return this;
    }

    /** Inserts the given entries into the pool */
    @CanIgnoreReturnValue
    public Builder addToPool(LootPoolInjection injection) {
      pools.computeIfAbsent(injection.name(), ignored -> new ArrayList<>()).addAll(injection.entries());
      return this;
    }

    /** Builds the list of injections */
    public LootTableInjection build(Identifier name) {
      return new LootTableInjection(name, pools.entrySet().stream()
        .map(entry -> new LootPoolInjection(entry.getKey(), List.copyOf(entry.getValue())))
        .toList());
    }
  }
}
