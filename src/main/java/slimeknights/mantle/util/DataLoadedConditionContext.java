package slimeknights.mantle.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Condition context to use when data has already been loaded, used in books for processing their conditions for instance. */
public enum DataLoadedConditionContext implements ICondition.IContext {
  INSTANCE;

  @Override
  public <T> boolean isTagLoaded(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    return registry != null && registry.getTags().anyMatch(tag -> tag.key().equals(key));
  }

  @Override
  public <T> Collection<Holder<T>> getTag(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    if (registry != null) {
      return StreamSupport.stream(registry.getTagOrEmpty(key).spliterator(), false).toList();
    }
    return Set.of();
  }

  public <T> Map<Identifier,Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key);
    if (registry != null) {
      return registry.getTags().collect(Collectors.toMap(tag -> tag.key().location(), tag -> tag.contents));
    }
    return Map.of();
  }
}
