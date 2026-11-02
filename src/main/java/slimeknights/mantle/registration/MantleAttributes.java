package slimeknights.mantle.registration;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.registration.deferred.AttributeDeferredRegister;
import slimeknights.mantle.registration.object.DeferredAttribute;

/** Handles all new attributes added by Mantle */

public class MantleAttributes {
  private static final AttributeDeferredRegister ATTRIBUTES = new AttributeDeferredRegister(Mantle.modId);

  private MantleAttributes() {}

  public static void init(IEventBus bus) {
    ATTRIBUTES.addAlias(Mantle.getResource("generic.knockback_multiplier"), Mantle.getResource("knockback_multiplier"));
    ATTRIBUTES.addAlias(Mantle.getResource("player.experience_multiplier"), Mantle.getResource("experience_multiplier"));
    ATTRIBUTES.register(bus);
    bus.addListener(EntityAttributeModificationEvent.class, MantleAttributes::addAttributes);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent.class, MantleAttributes::entityEnchantment);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LivingKnockBackEvent.class, MantleAttributes::onKnockback);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BlockDropsEvent.class, MantleAttributes::blockDrops);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LivingExperienceDropEvent.class, MantleAttributes::livingExperienceDrop);
  }

  // generic
  /** Multiplier for knockback this entity takes. Similar to {@link Attributes#KNOCKBACK_RESISTANCE} but can be used to increase knockback */
  public static final DeferredAttribute<PercentageAttribute> KNOCKBACK_MULTIPLIER = ATTRIBUTES.register("knockback_multiplier").sync().negative().multiplier();

  /** Integer looting bonus; the fractional part is a chance for one extra level. */
  public static final DeferredAttribute<net.minecraft.world.entity.ai.attributes.RangedAttribute> LOOTING_BOOST = ATTRIBUTES.register("looting_boost").sync().ranged(0, -255, 255);
  /** Integer fortune bonus; the fractional part is a chance for one extra level. */
  public static final DeferredAttribute<net.minecraft.world.entity.ai.attributes.RangedAttribute> FORTUNE_BOOST = ATTRIBUTES.register("fortune_boost").sync().ranged(0, -255, 255);

  // player
  /** Multiplier to amount of gained for players. */
  public static final DeferredAttribute<PercentageAttribute> EXPERIENCE_MULTIPLIER = ATTRIBUTES.register("experience_multiplier").multiplier();


  @SubscribeEvent
  static void addAttributes(EntityAttributeModificationEvent event) {
    // general attributes
    addToAll(event, KNOCKBACK_MULTIPLIER);
    addToAll(event, LOOTING_BOOST);
    addToAll(event, FORTUNE_BOOST);
    // player attributes
    event.add(EntityType.PLAYER, EXPERIENCE_MULTIPLIER);
  }

  /** Adds an attribute to all entities with the given default value. */
  public static void addToAll(EntityAttributeModificationEvent event, DeferredHolder<Attribute,?> attribute, double defaultValue) {
    for (EntityType<? extends LivingEntity> entity : event.getTypes()) {
      event.add(entity, attribute, defaultValue);
    }
  }

  /** Adds an attribute to all entities with the standard default value */
  public static void addToAll(EntityAttributeModificationEvent event, DeferredHolder<Attribute,?> attribute) {
    addToAll(event, attribute, attribute.value().getDefaultValue());
  }


  /* Implementation */

  /** Called when an entity takes knockback to apply {@link #KNOCKBACK_MULTIPLIER} */
  @SubscribeEvent
  static void onKnockback(LivingKnockBackEvent event) {
    event.setStrength((float) (event.getStrength() * event.getEntity().getAttributeValue(KNOCKBACK_MULTIPLIER)));
  }

  /** Called when a block is broken to boost its XP using {@link #EXPERIENCE_MULTIPLIER}. */
  @SubscribeEvent
  static void blockDrops(BlockDropsEvent event) {
    if (event.getBreaker() instanceof Player living) {
      event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * living.getAttributeValue(EXPERIENCE_MULTIPLIER)));
    }
  }

  /** Called when a monster is killed to boost its experience using the player's {@link #EXPERIENCE_MULTIPLIER}. */
  @SubscribeEvent
  static void livingExperienceDrop(LivingExperienceDropEvent event) {
    Player player = event.getAttackingPlayer();
    if (player != null) {
      event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * player.getAttributeValue(EXPERIENCE_MULTIPLIER)));
    }
  }
  /** Applies a level bonus with unbiased fractional rounding and the vanilla enchantment bounds. */
  public static int getEnchantmentLevel(LivingEntity entity, net.minecraft.core.Holder<Attribute> attribute, int current) {
    double value = entity.getAttributeValue(attribute);
    int level = net.minecraft.util.Mth.floor(value);
    if (value > level && entity.getRandom().nextFloat() < value - level) ++level;
    return net.minecraft.util.Mth.clamp(current + level, 0, 255);
  }

  private static void entityEnchantment(net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent event) {
    var source = event.getDamageSource();
    if (event.getEnchantment().is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)
        && source != null && source.getEntity() instanceof LivingEntity attacker) {
      event.setEnchantmentLevel(getEnchantmentLevel(attacker, LOOTING_BOOST, event.getEnchantmentLevel()));
    }
  }
}
