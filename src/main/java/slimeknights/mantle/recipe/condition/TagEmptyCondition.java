package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.Mantle;

/** Condition that checks when a tag is empty. Same as {@link net.minecraftforge.common.crafting.conditions.TagEmptyCondition} but for any registry */
public class TagEmptyCondition<T> extends TagCondition<T> implements LootItemCondition {
  public static final Identifier ID = Mantle.getResource("tag_empty");
  public static final MapCodec<TagEmptyCondition<?>> CODEC = TagCondition.codec(TagEmptyCondition::new);

  public TagEmptyCondition(TagKey<T> tag) {
    super(tag);
  }

  public TagEmptyCondition(ResourceKey<? extends Registry<T>> registry, Identifier name) {
    this(TagKey.create(registry, name));
  }

  @Override
  public MapCodec<TagEmptyCondition<?>> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    return context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    return context.getLevel().registryAccess().lookup(tag.registry())
      .flatMap(registry -> registry.get(tag))
      .map(values -> !values.iterator().hasNext())
      .orElse(false);
  }
}
