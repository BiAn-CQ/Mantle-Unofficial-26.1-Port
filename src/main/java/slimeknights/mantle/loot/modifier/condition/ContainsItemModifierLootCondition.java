package slimeknights.mantle.loot.modifier.condition;

/** Uses the shared modifier condition registry. */
public class ContainsItemModifierLootCondition extends slimeknights.mantle.loot.condition.ContainsItemModifierLootCondition implements ILootModifierCondition {
  public ContainsItemModifierLootCondition(net.minecraft.world.item.crafting.Ingredient ingredient) { super(ingredient); }
  public ContainsItemModifierLootCondition(net.minecraft.world.item.crafting.Ingredient ingredient, int amount) { super(ingredient, amount); }
}
