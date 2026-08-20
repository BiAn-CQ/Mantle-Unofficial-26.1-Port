package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Abstract ingredient that matches a list of items or a tag, mirroring the vanilla syntax */
public abstract class ItemIngredient implements ICustomIngredient {
  protected static final Codec<List<Holder<Item>>> ITEMS_CODEC = Codec.withAlternative(
    Item.CODEC.listOf(), Item.CODEC.xmap(List::of, List::getFirst));
  protected static final Codec<TagKey<Item>> ITEM_TAG_CODEC = TagKey.codec(Registries.ITEM);

  protected final List<Holder<Item>> explicitItems;
  protected final @Nullable TagKey<Item> tag;

  /** Constructor letting you supply your own item stream */
  protected ItemIngredient(List<Holder<Item>> explicitItems, @Nullable TagKey<Item> tag) {
    this.explicitItems = List.copyOf(explicitItems);
    this.tag = tag;
  }

  /** Maps the list to a list of items */
  protected static List<Holder<Item>> toItems(List<? extends ItemLike> items) {
    return items.stream().map(ItemLike::asItem).map(item -> (Holder<Item>)item.builtInRegistryHolder()).toList();
  }

  protected static Optional<TagKey<Item>> optionalTag(@Nullable TagKey<Item> tag) {
    return Optional.ofNullable(tag);
  }

  @Override
  public boolean test(ItemStack stack) {
    // super is going to do list iteration, but for tag checks it's way easier to just check directly
    // also ensures we never match empty just because our lists are empty
    return !stack.isEmpty() && (explicitItems.contains(stack.typeHolder()) || tag != null && stack.is(tag));
  }

  @Override
  public Stream<Holder<Item>> items() {
    Stream<Holder<Item>> tagged = tag == null
      ? Stream.empty()
      : StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false);
    return Stream.concat(explicitItems.stream(), tagged).distinct();
  }
}
