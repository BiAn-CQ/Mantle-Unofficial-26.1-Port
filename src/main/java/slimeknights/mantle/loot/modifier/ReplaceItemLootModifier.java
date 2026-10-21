package slimeknights.mantle.loot.modifier;

/** Canonical entry point sharing the registered native implementation. */
public class ReplaceItemLootModifier extends slimeknights.mantle.loot.ReplaceItemLootModifier {
  protected ReplaceItemLootModifier(net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] conditions, int priority,
      net.minecraft.world.item.crafting.Ingredient original, slimeknights.mantle.recipe.helper.ItemOutput replacement,
      net.minecraft.world.level.storage.loot.functions.LootItemFunction[] functions) {
    super(conditions, priority, original, replacement, functions);
  }
}
