package slimeknights.mantle.data.predicate.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.NamedComponentRegistry;

/** Predicate matching a specific mob type */
public record MobTypePredicate(TagKey<EntityType<?>> type) implements LivingEntityPredicate {
  /**
   * Registry of mob types, to allow addons to register types
   * TODO: support registering via IMC
   */
  public static final NamedComponentRegistry<TagKey<EntityType<?>>> MOB_TYPES = new NamedComponentRegistry<>("Unknown mob type");
  /** Loader for a mob type predicate */
  public static RecordLoadable<MobTypePredicate> LOADER = RecordLoadable.create(MOB_TYPES.requiredField("mobs", MobTypePredicate::type), MobTypePredicate::new);

  @Override
  public boolean matches(LivingEntity input) {
    return input.getType().builtInRegistryHolder().is(type);
  }

  @Override
  public RecordLoadable<? extends LivingEntityPredicate> getLoader() {
    return LOADER;
  }
}
