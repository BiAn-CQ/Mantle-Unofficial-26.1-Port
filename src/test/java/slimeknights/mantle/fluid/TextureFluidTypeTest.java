package slimeknights.mantle.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextureFluidTypeTest {
  @Test
  void customFluidUsesStandardMovementWithoutWaterBehavior() {
    TextureFluidType type = new TextureFluidType(FluidType.Properties.create());
    LivingEntity entity = mock(LivingEntity.class);
    Vec3 input = new Vec3(1.0, 0.0, 0.0);
    Vec3 initialMovement = new Vec3(0.1, -0.02, 0.2);
    Vec3 slowedMovement = initialMovement.multiply(0.8F, 0.8F, 0.8F);
    Vec3 adjustedMovement = new Vec3(slowedMovement.x, -0.03, slowedMovement.z);
    double gravity = 0.08;

    when(entity.getDeltaMovement()).thenReturn(initialMovement, initialMovement, initialMovement, adjustedMovement);
    when(entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY)).thenReturn(0.0);
    when(entity.getAttributeValue(NeoForgeMod.SWIM_SPEED)).thenReturn(1.0);
    when(entity.getFluidFallingAdjustedMovement(gravity, true, slowedMovement)).thenReturn(adjustedMovement);

    assertThat(type.getIsWaterLike()).isFalse();
    assertThat(type.move(entity, input, gravity)).isTrue();
    verify(entity).moveRelative(0.02F, input);
    verify(entity).move(MoverType.SELF, initialMovement);
    verify(entity).setDeltaMovement(adjustedMovement);
  }

  @Test
  void invertedFluidUsesTheSameMovementType() {
    assertThat(new InvertedFluidType(FluidType.Properties.create())).isInstanceOf(TextureFluidType.class);
  }
}
