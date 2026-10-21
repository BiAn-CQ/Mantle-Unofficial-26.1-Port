package slimeknights.mantle.loot.injection;

/** The native injector transforms loot JSON before immutable loot tables are decoded. */
public final class LootTableInjector {
  public static final String FOLDER = slimeknights.mantle.loot.LootTableInjector.FOLDER;
  public static final slimeknights.mantle.loot.LootTableInjector INSTANCE = slimeknights.mantle.loot.LootTableInjector.INSTANCE;
  private LootTableInjector() {}
  public static void init() { slimeknights.mantle.loot.LootTableInjector.init(); }
}
