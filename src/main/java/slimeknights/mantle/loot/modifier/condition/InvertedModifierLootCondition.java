package slimeknights.mantle.loot.modifier.condition;

/** Uses the shared modifier condition registry. */
public class InvertedModifierLootCondition extends slimeknights.mantle.loot.condition.InvertedModifierLootCondition implements ILootModifierCondition {
  public InvertedModifierLootCondition(slimeknights.mantle.loot.condition.ILootModifierCondition base) { super(base); }
}
