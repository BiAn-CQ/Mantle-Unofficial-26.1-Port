package slimeknights.mantle.block.fluid;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** Liquid block setting the entity on fire */
public class MobEffectLiquidBlock extends LiquidBlock {
  private final Supplier<MobEffectInstance> effect;
  public MobEffectLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, Supplier<MobEffectInstance> effect) {
    super(supplier.get(), properties);
    this.effect = effect;
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createEffect(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
    return fluid -> new MobEffectLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), effect);
  }
}
