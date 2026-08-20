package slimeknights.mantle.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.RetexturedHelper;

import java.util.List;
import java.util.Set;

/**
 * Applies the data for a retextured block to the dropped item. No configuration needed.
 */
@SuppressWarnings("WeakerAccess")
public class RetexturedLootFunction extends LootItemConditionalFunction {
  public static final MapCodec<RetexturedLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
    commonFields(instance).apply(instance, RetexturedLootFunction::new));

  public RetexturedLootFunction(List<LootItemCondition> conditions) {
    super(conditions);
  }

  /**
   * Creates a new instance from the given conditions
   * @param conditions Conditions list
   */
  public RetexturedLootFunction(LootItemCondition[] conditions) {
    this(List.of(conditions));
  }

  /** Creates a new instance with no conditions */
  public RetexturedLootFunction() {
    this(List.of());
  }

  @Override
  public Set<ContextKey<?>> getReferencedContextParams() {
    return Set.of(LootContextParams.BLOCK_ENTITY);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    if (blockEntity instanceof IRetexturedBlockEntity retextured) {
      RetexturedHelper.setTexture(stack, retextured.getTextureName());
    } else {
      String name = blockEntity == null ? "null" : blockEntity.getClass().getName();
      Mantle.logger.warn("Found wrong block entity for retextured loot function, expected IRetexturedBlockEntity, found {}", name);
    }
    return stack;
  }

  @Override
  public MapCodec<RetexturedLootFunction> codec() {
    return CODEC;
  }
}
