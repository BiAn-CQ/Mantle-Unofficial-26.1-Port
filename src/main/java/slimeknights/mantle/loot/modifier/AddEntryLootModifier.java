package slimeknights.mantle.loot.modifier;

/** Canonical entry point sharing the registered native implementation. */
public class AddEntryLootModifier extends slimeknights.mantle.loot.AddEntryLootModifier {
  protected AddEntryLootModifier(net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] conditions, int priority,
      java.util.List<slimeknights.mantle.loot.condition.ILootModifierCondition> postConditions,
      net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer entry,
      net.minecraft.world.level.storage.loot.functions.LootItemFunction[] functions) {
    super(conditions, priority, postConditions, entry, functions);
  }
}
