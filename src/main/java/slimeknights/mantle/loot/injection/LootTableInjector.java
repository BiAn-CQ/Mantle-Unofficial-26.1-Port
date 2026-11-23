package slimeknights.mantle.loot.injection;

/** The native injector transforms loot JSON before immutable loot tables are decoded. */
public final class LootTableInjector {
  public static final String FOLDER = slimeknights.mantle.loot.LootTableInjector.FOLDER;
  public static final slimeknights.mantle.loot.LootTableInjector INSTANCE = slimeknights.mantle.loot.LootTableInjector.INSTANCE;
  private LootTableInjector() {}
  public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<slimeknights.mantle.loot.LootTableInjection>> REGISTRY_KEY = slimeknights.mantle.loot.LootTableInjector.REGISTRY_KEY;
  public static void init(net.neoforged.bus.api.IEventBus bus) { slimeknights.mantle.loot.LootTableInjector.init(bus); }
}
