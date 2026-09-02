package slimeknights.mantle.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

/**
 * Loot function to set the fluid on a dropped item
 */
public class SetFluidLootFunction extends LootItemConditionalFunction {
  public static final MapCodec<SetFluidLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
    commonFields(instance).and(FluidStackTemplate.CODEC.fieldOf("fluid").forGetter(function -> function.fluid))
      .apply(instance, SetFluidLootFunction::new));

  /** Fluid to add to the item */
  private final FluidStackTemplate fluid;

  protected SetFluidLootFunction(List<LootItemCondition> conditions, FluidStackTemplate fluid) {
    super(conditions);
    this.fluid = fluid;
  }

  protected SetFluidLootFunction(LootItemCondition[] conditions, FluidStackTemplate fluid) {
    this(List.of(conditions), fluid);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (stack.isEmpty()) {
      return stack;
    }
    SimpleContainer container = new SimpleContainer(stack);
    ItemAccess access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(container), 0);
    ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
    FluidStack created = fluid.create();
    if (handler != null && !created.isEmpty()) {
      try (Transaction transaction = Transaction.openRoot()) {
        if (handler.insert(FluidResource.of(created), created.getAmount(), transaction) > 0) {
          transaction.commit();
        }
      }
      return container.getItem(0);
    }
    return stack;
  }

  @Override
  public MapCodec<SetFluidLootFunction> codec() {
    return CODEC;
  }

  /**
   * Creates a new builder with the given fluid
   * @param fluid  Fluid to set
   * @return  Builder instance
   */
  public static Builder<?> builder(FluidStack fluid) {
    return simpleBuilder(conditions -> new SetFluidLootFunction(conditions, FluidStackTemplate.fromNonEmptyStack(fluid)));
  }
}
