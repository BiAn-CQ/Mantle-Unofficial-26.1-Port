package slimeknights.mantle.recipe.data;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a recipe consumer wrapper, which adds some extra properties to wrap the result of another recipe
 */
@SuppressWarnings("unused")
public class ConsumerWrapperBuilder {
  private final List<ICondition> conditions = new ArrayList<>();

  private ConsumerWrapperBuilder() {}

  /**
   * Creates a wrapper builder with the default serializer
   * @return Default serializer builder
   */
  public static ConsumerWrapperBuilder wrap() {
    return new ConsumerWrapperBuilder();
  }

  /**
   * Adds a conditional to the consumer
   * @param condition Condition to add
   * @return Added condition
   */
  @CanIgnoreReturnValue
  public ConsumerWrapperBuilder addCondition(ICondition condition) {
    conditions.add(condition);
    return this;
  }

  public RecipeOutput build(RecipeOutput consumer) {
    return consumer.withConditions(conditions.toArray(ICondition[]::new));
  }
}
