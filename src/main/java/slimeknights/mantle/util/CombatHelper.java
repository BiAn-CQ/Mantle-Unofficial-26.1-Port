package slimeknights.mantle.util;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Helpers for attacking with weapons */
public final class CombatHelper {
  private static final float TO_RADIAN = (float)Math.PI / 180f;
  private static final AttributeModifier ANTI_KNOCKBACK_MODIFIER =
    new AttributeModifier(Mantle.getResource("anti_knockback"), 1f, Operation.ADD_VALUE);
  /** Tool action to disable the base knockback of the weapon. Requires replacing left click behavior of your weapon. */
  public static final ItemAbility NO_BASE_KNOCKBACK = ItemAbility.get("no_base_knockback");

  private CombatHelper() {}

  /** Gets the item stack in the main hand that contributes to attributes. Exposed for benefit of Tinkers' Construct which can optimize these methods for its tools. */
  public static ItemStack getMainhandAttributeStack(LivingEntity entity) {
    return entity.getMainHandItem();
  }

  /**
   * Gets a modifiable map that is a copy of the modifiers from the given attribute instance. All operations are guaranteed to have a valid set.
   * Note we use a map instead of a full attribute instance as we don't need the cache or other data structures.
   */
  public static Map<Operation, Set<AttributeModifier>> copyModifiers(AttributeInstance instance) {
    Map<Operation, Set<AttributeModifier>> modifiers = new EnumMap<>(Operation.class);
    for (Operation operation : Operation.values()) {
      modifiers.put(operation, new HashSet<>());
    }
    for (AttributeModifier modifier : instance.getModifiers()) {
      modifiers.get(modifier.operation()).add(modifier);
    }
    return modifiers;
  }

  /** Gets the attribute for the offhand by subtracting mainhand attributes and adding in offhand stack attributes. */
  public static float getOffhandAttribute(ItemStack stack, LivingEntity entity, Holder<Attribute> attribute) {
    AttributeInstance instance = entity.getAttribute(attribute);
    if (instance == null) {
      return (float)entity.getAttributeBaseValue(attribute);
    }

    List<AttributeModifier> mainModifiers = new ArrayList<>();
    // fetch attributes for both relevant stacks
    ItemStack mainStack = getMainhandAttributeStack(entity);
    if (!mainStack.isEmpty()) {
      mainStack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (candidate, modifier) -> {
        if (candidate.equals(attribute)) mainModifiers.add(modifier);
      });
    }
    List<AttributeModifier> offhandModifiers = new ArrayList<>();
    stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (candidate, modifier) -> {
      if (candidate.equals(attribute)) offhandModifiers.add(modifier);
    });
    // if no modifier changed, can save some work by just using the cached value
    if (mainModifiers.isEmpty() && offhandModifiers.isEmpty()) {
      return (float)instance.getValue();
    }

    // start by creating a modifiable copy of the per operation attribute map
    Map<Operation,Set<AttributeModifier>> modifiers = copyModifiers(instance);
    for (AttributeModifier modifier : mainModifiers) modifiers.get(modifier.operation()).remove(modifier);
    // add in all offhand modifiers
    for (AttributeModifier modifier : offhandModifiers) modifiers.get(modifier.operation()).add(modifier);
    // compute the value
    return (float)computeAttribute(attribute, instance.getBaseValue(), modifiers);
  }

  /** Computes the value for the given attribute. Copied from {@link AttributeInstance#calculateValue} */
  public static double computeAttribute(Holder<Attribute> attribute, double base, Map<Operation,Set<AttributeModifier>> modifiers) {
    // addition modifiers
    for (AttributeModifier modifier : modifiers.get(Operation.ADD_VALUE)) base += modifier.amount();
    // multiply base
    double value = base;
    for (AttributeModifier modifier : modifiers.get(Operation.ADD_MULTIPLIED_BASE)) value += base * modifier.amount();
    // multiply total
    for (AttributeModifier modifier : modifiers.get(Operation.ADD_MULTIPLIED_TOTAL)) value *= 1.0 + modifier.amount();
    return attribute.value().sanitizeValue(value);
  }

  /** Checks if the given entity can be attacked. */
  public static boolean isAttackable(Entity attacker, Entity target) {
    return target.isAttackable() && !target.skipAttackInteraction(attacker);
  }

  /**
   * Performs an attack, mimicking  {@link Player#attack(Entity)}.
   * For use in {@link net.minecraft.world.item.Item#interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand)} primarily,
   * but can also be used to fake an attack similar to {@link net.minecraftforge.common.extensions.IForgeItem#onLeftClickEntity(ItemStack, Player, Entity)}.
   *
   * @param stack         Stack used for attacking.
   * @param target        Entity target
   * @param targetLiving  Living entity target. May be different in the case of multipart entities.
   * @param hand          Hand used for attacking.
   */
  public static boolean attack(ItemStack stack, Player player, Entity target, @Nullable LivingEntity targetLiving, InteractionHand hand) {
    return attack(stack, player, target, targetLiving, hand, player.damageSources().playerAttack(player));
  }

  /**
   * Performs an attack, mimicking {@link Player#attack(Entity)} but allowing the damage source to be swapped.
   * For use in {@link net.minecraft.world.item.Item#interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand)} primarily,
   * but can also be used to fake an attack similar to {@link net.minecraftforge.common.extensions.IForgeItem#onLeftClickEntity(ItemStack, Player, Entity)}.
   *
   * @param stack         Stack used for attacking.
   * @param target        Entity target
   * @param targetLiving  Living entity target. May be different in the case of multipart entities.
   * @param hand          Hand used for attacking.
   * @param damageSource  Damage source to apply
   */
  public static boolean attack(ItemStack stack, Player player, Entity target, @Nullable LivingEntity targetLiving,
                               InteractionHand hand, DamageSource damageSource) {
    if (!CommonHooks.onPlayerAttackTarget(player, target) || !isAttackable(player, target)) {
      return false;
    }

      // apply cooldown
    float baseDamage = hand == InteractionHand.OFF_HAND
      ? getOffhandAttribute(stack, player, Attributes.ATTACK_DAMAGE)
      : (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      // scale damage cooldown
    float cooldown = hand == InteractionHand.OFF_HAND ? OffhandCooldownTracker.getCooldown(player) : player.getAttackStrengthScale(0.5F);
    float magicBoost = 0;
    if (player.level() instanceof ServerLevel serverLevel) {
      magicBoost = cooldown * (EnchantmentHelper.modifyDamage(serverLevel, stack, target, damageSource, baseDamage) - baseDamage);
    }
    baseDamage *= 0.2F + cooldown * cooldown * 0.8F;
    baseDamage += stack.getItem().getAttackDamageBonus(target, baseDamage, damageSource);

    if (baseDamage > 0 || magicBoost > 0) {
      boolean fullStrength = cooldown > 0.9F;
      boolean knockbackAttack = player.isSprinting() && fullStrength;
      if (knockbackAttack) {
        playAttackSound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
      }

      boolean vanillaCritical = fullStrength && player.fallDistance > 0 && !player.onGround() && !player.onClimbable()
        && !player.isInWater() && !player.isMobilityRestricted() && !player.isPassenger() && !player.isSprinting()
        && targetLiving != null;
      var criticalEvent = CommonHooks.fireCriticalHit(player, target, vanillaCritical, vanillaCritical ? 1.5f : 1f);
      boolean critical = criticalEvent.isCriticalHit();
      if (critical) baseDamage *= criticalEvent.getDamageMultiplier();

      double maxSweepSpeed = player.getSpeed() * 2.5;
      boolean vanillaSweep = fullStrength && !(critical && criticalEvent.disableSweep()) && !knockbackAttack && player.onGround()
        && player.getKnownMovement().horizontalDistanceSqr() < Mth.square(maxSweepSpeed)
        && stack.canPerformAction(ItemAbilities.SWORD_SWEEP);
      boolean sweep = CommonHooks.fireSweepAttack(player, target, vanillaSweep).isSweeping();

      float oldHealth = targetLiving == null ? 0 : targetLiving.getHealth();
        // hit the target
      Vec3 oldMovement = target.getDeltaMovement();
      boolean hit = hurtWithOptionalKnockbackSuppression(stack, target, targetLiving, damageSource, baseDamage + magicBoost);
        // apply hit effects
      if (hit) {
        float knockback = hand == InteractionHand.OFF_HAND
          ? getOffhandAttribute(stack, player, Attributes.ATTACK_KNOCKBACK)
          : (float)player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (player.level() instanceof ServerLevel serverLevel) {
          knockback = EnchantmentHelper.modifyKnockback(serverLevel, stack, target, damageSource, knockback) / 2f;
        } else {
          knockback /= 2f;
        }
        if (knockbackAttack) knockback += 0.5f;
        causeKnockback(player, target, targetLiving, knockback, oldMovement);

        if (sweep && player.level() instanceof ServerLevel serverLevel) {
          float sweepDamage = 1 + (float)player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * baseDamage;
          for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, stack.getSweepHitBox(player, target))) {
            if (nearby != player && nearby != targetLiving && !player.isAlliedTo(nearby)
              && (!(nearby instanceof ArmorStand armorStand) || !armorStand.isMarker())
              && player.distanceToSqr(nearby) < Mth.square(player.entityInteractionRange())) {
              float enchanted = EnchantmentHelper.modifyDamage(serverLevel, stack, nearby, damageSource, sweepDamage) * cooldown;
              if (nearby.hurtServer(serverLevel, damageSource, enchanted)) {
                nearby.knockback(0.4f, Mth.sin(player.getYRot() * TO_RADIAN), -Mth.cos(player.getYRot() * TO_RADIAN));
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, nearby, damageSource, stack);
              }
            }
          }
          playAttackSound(player, SoundEvents.PLAYER_ATTACK_SWEEP);
        }

        if (critical) {
          playAttackSound(player, SoundEvents.PLAYER_ATTACK_CRIT);
          player.crit(target);
        } else if (!sweep) {
          playAttackSound(player, fullStrength ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK);
        }
        if (magicBoost > 0) player.magicCrit(target);

          // enchantment post effects
        player.setLastHurtMob(target);
        if (player.level() instanceof ServerLevel serverLevel) {
          EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, target, damageSource, stack);
        }
        damageStack(stack, player, target, hand);

        if (targetLiving != null) {
          float damageDealt = oldHealth - targetLiving.getHealth();
          player.awardStat(Stats.DAMAGE_DEALT, Math.round(damageDealt * 10));
            // particles
          if (player.level() instanceof ServerLevel serverLevel && damageDealt > 2) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(),
              (int)(damageDealt * 0.5), 0.1, 0, 0.1, 0.2);
          }
        }
        player.causeFoodExhaustion(0.1f);
      } else {
        playAttackSound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE);
      }
    }

    if (hand == InteractionHand.OFF_HAND) {
      OffhandCooldownTracker.applyCooldown(player, getOffhandAttribute(stack, player, Attributes.ATTACK_SPEED), 20);
    } else {
      player.onAttack();
    }
    return true;
  }

  private static boolean hurtWithOptionalKnockbackSuppression(ItemStack stack, Entity target, @Nullable LivingEntity targetLiving,
                                                               DamageSource source, float damage) {
        // cancel knockback if requested
    if (!stack.canPerformAction(NO_BASE_KNOCKBACK) || targetLiving == null) {
      return target.hurtOrSimulate(source, damage);
    }
    AttributeInstance resistance = targetLiving.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (resistance == null || resistance.hasModifier(ANTI_KNOCKBACK_MODIFIER.id())) {
      return target.hurtOrSimulate(source, damage);
    }
    resistance.addTransientModifier(ANTI_KNOCKBACK_MODIFIER);
    try {
      return target.hurtOrSimulate(source, damage);
    } finally {
      resistance.removeModifier(ANTI_KNOCKBACK_MODIFIER.id());
    }
  }

  private static void causeKnockback(Player player, Entity target, @Nullable LivingEntity targetLiving, float knockback, Vec3 oldMovement) {
          // apply knockback
    if (knockback > 0) {
      if (targetLiving != null) {
        targetLiving.knockback(knockback, Mth.sin(player.getYRot() * TO_RADIAN), -Mth.cos(player.getYRot() * TO_RADIAN));
      } else {
        target.push(-Mth.sin(player.getYRot() * TO_RADIAN) * knockback, 0.1, Mth.cos(player.getYRot() * TO_RADIAN) * knockback);
      }
      player.setDeltaMovement(player.getDeltaMovement().multiply(0.6, 1, 0.6));
      player.setSprinting(false);
    }
          // sync player motion
    if (target instanceof ServerPlayer serverPlayer && target.hurtMarked) {
      serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
      target.hurtMarked = false;
      target.setDeltaMovement(oldMovement);
    }
  }

  private static void damageStack(ItemStack stack, Player player, Entity target, InteractionHand hand) {
    Entity parent = target instanceof PartEntity<?> part ? part.getParent() : target;
          // damage the tool
    if (!(player.level() instanceof ServerLevel) || stack.isEmpty() || !(parent instanceof LivingEntity living)) return;
    ItemStack copy = stack.copy();
    if (stack.hurtEnemy(living, player)) stack.postHurtEnemy(living, player);
    if (stack.isEmpty()) {
      EventHooks.onPlayerDestroyItem(player, copy, hand);
      player.setItemInHand(hand, ItemStack.EMPTY);
    }
  }

  private static void playAttackSound(Player player, net.minecraft.sounds.SoundEvent sound) {
    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1, 1);
  }

  /** Makes a damage source from the given key */
  public static Holder<DamageType> damageType(RegistryAccess access, ResourceKey<DamageType> key) {
    return access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
  }

  /** Makes a damage source from the given key */
  public static DamageSource damageSource(RegistryAccess access, ResourceKey<DamageType> key) {
    return new DamageSource(damageType(access, key));
  }

  /** Makes a damage source from the given key */
  public static DamageSource damageSource(Level level, ResourceKey<DamageType> key) {
    return new DamageSource(damageType(level.registryAccess(), key));
  }

  /** Makes a damage source from the given key for direct damage from an entity. */
  public static DamageSource damageSource(ResourceKey<DamageType> key, Entity entity) {
    return new DamageSource(damageType(entity.level().registryAccess(), key), entity);
  }

  /** Makes a damage source from the given key for indirect damage, such as from a projectile. */
  public static DamageSource damageSource(ResourceKey<DamageType> key, Entity direct, @Nullable Entity causing) {
    return new DamageSource(damageType(direct.level().registryAccess(), key), direct, causing);
  }
}
