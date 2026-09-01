package slimeknights.mantle.fluid;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type whose color and textures are determined by the model.
 * Just implements {@link ClientTextureFluidType} in initializeClient as the Forge API is dumb and does not let me do that in a client place.
 */
public class TextureFluidType extends FluidType {
  public TextureFluidType(Properties properties) {
    super(properties);
  }

  /**
   * Applies the standard fluid movement used by Tinkers-style fluids.
   * NeoForge 26.1 no longer falls back to this movement for custom fluid types.
   */
  @Override
  public boolean move(LivingEntity entity, Vec3 input, double gravity) {
    boolean isFalling = entity.getDeltaMovement().y <= 0.0;
    double oldY = entity.getY();
    float slowdown = entity.isSprinting() ? 0.9F : 0.8F;
    float speed = 0.02F;
    float waterMovementEfficiency = (float)entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
    if (!entity.onGround()) {
      waterMovementEfficiency *= 0.5F;
    }

    if (waterMovementEfficiency > 0.0F) {
      slowdown += (0.54600006F - slowdown) * waterMovementEfficiency;
      speed += (entity.getSpeed() - speed) * waterMovementEfficiency;
    }

    if (entity.hasEffect(MobEffects.DOLPHINS_GRACE)) {
      slowdown = 0.96F;
    }

    speed *= (float)entity.getAttributeValue(NeoForgeMod.SWIM_SPEED);
    entity.moveRelative(speed, input);
    entity.move(MoverType.SELF, entity.getDeltaMovement());
    Vec3 movement = entity.getDeltaMovement();
    if (entity.horizontalCollision && entity.onClimbable()) {
      movement = new Vec3(movement.x, 0.2, movement.z);
    }

    movement = movement.multiply(slowdown, 0.8F, slowdown);
    entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, isFalling, movement));
    Vec3 adjustedMovement = entity.getDeltaMovement();
    if (entity.horizontalCollision && entity.isFree(adjustedMovement.x, adjustedMovement.y + 0.6F - entity.getY() + oldY, adjustedMovement.z)) {
      entity.setDeltaMovement(adjustedMovement.x, 0.3F, adjustedMovement.z);
    }
    return true;
  }
}
