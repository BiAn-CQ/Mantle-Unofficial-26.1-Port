package slimeknights.mantle.loot;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.util.JsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Data provider for adding new loot table injections */
public abstract class AbstractLootTableInjectionProvider extends GenericDataProvider {
  private final List<Builder> builders = new ArrayList<>();
  private final String domain;
  private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries;

  public AbstractLootTableInjectionProvider(PackOutput output, String domain) {
    this(output, domain, CompletableFuture.completedFuture(slimeknights.mantle.util.RegistryHelper.BUILTIN_REGISTRY_ACCESS));
  }

  public AbstractLootTableInjectionProvider(PackOutput output, String domain, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
    super(output, Target.DATA_PACK, LootTableInjector.FOLDER);
    this.domain = domain;
    this.registries = registries;
  }

  /** Method to add all relevant tables */
  protected void addTables() {}
  protected void addTables(net.minecraft.core.HolderLookup.Provider lookup) { addTables(); }

  @Override
  public final CompletableFuture<?> run(CachedOutput output) {
    return registries.thenCompose(lookup -> {
      addTables(lookup);
      var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
      return allOf(builders.stream().map(builder -> {
        JsonObject json = LootTableInjection.CODEC.encodeStart(ops, builder.build()).getOrThrow().getAsJsonObject();
        JsonHelper.serializeConditions(ops, json, List.of(builder.conditions));
        return saveJson(output, Identifier.fromNamespaceAndPath(domain, builder.path), json);
      }));
    });
  }

  /** Creates a new injection */
  protected LootTableInjection.Builder inject(String path, Identifier name, ICondition... conditions) {
    LootTableInjection.Builder builder = new LootTableInjection.Builder();
    builders.add(new Builder(path, name, builder, conditions));
    return builder;
  }

  /** Creates a new injection for the Minecraft domain */
  protected LootTableInjection.Builder inject(String path, String name, ICondition... conditions) {
    return inject(path, Identifier.parse(name), conditions);
  }

  /** Creates a new injection for the Minecraft domain */
  protected LootTableInjection.Builder injectChest(String name, ICondition... conditions) {
    return inject(name, Identifier.parse("chests/" + name), conditions);
  }

  /** Creates a new injection for the Minecraft domain */
  protected LootTableInjection.Builder injectGameplay(String name, ICondition... conditions) {
    return inject(name, Identifier.parse("gameplay/" + name), conditions);
  }

  /** Internal builder tuple */
  private record Builder(String path, Identifier name, LootTableInjection.Builder builder, ICondition[] conditions) {
    public LootTableInjection build() {
      return builder.build(name);
    }
  }
}
