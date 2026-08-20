package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.Mantle;

/** Inverted form of {@link TagEmptyCondition} as filled is way more common a desire than empty. */
public class TagFilledCondition<T> extends TagCondition<T> implements LootItemCondition {
  public static final Identifier ID = Mantle.getResource("tag_filled");
  public static final MapCodec<TagFilledCondition<?>> CODEC = TagCondition.codec(TagFilledCondition::new);

  public TagFilledCondition(TagKey<T> tag) {
    super(tag);
  }

  public TagFilledCondition(ResourceKey<? extends Registry<T>> registry, Identifier name) {
    this(TagKey.create(registry, name));
  }

  @Override
  public MapCodec<TagFilledCondition<?>> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    return !context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    return context.getLevel().registryAccess().lookup(tag.registry())
      .flatMap(registry -> registry.get(tag))
      .map(values -> values.iterator().hasNext())
      .orElse(false);
  }
}
