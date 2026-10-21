package slimeknights.mantle.loot.modifier.condition;

/** Uses the shared modifier condition registry. */
public class EmptyModifierLootCondition extends slimeknights.mantle.loot.condition.EmptyModifierLootCondition implements ILootModifierCondition {
  public static final EmptyModifierLootCondition INSTANCE = new EmptyModifierLootCondition();
  private EmptyModifierLootCondition() {}
}
