package slimeknights.mantle.registration.object;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static slimeknights.mantle.Mantle.commonResource;

/** Object wrapper containing ingots, nuggets, and blocks */
public class MetalItemObject extends ItemObject<Block> implements MultiObject<ItemLike> {
  private final Supplier<? extends Item> ingot;
  private final Supplier<? extends Item> nugget;
  private final TagKey<Block> blockTag;
  private final TagKey<Item> blockItemTag;
  private final TagKey<Item> ingotTag;
  private final TagKey<Item> nuggetTag;

  public MetalItemObject(String tagName, ItemObject<? extends Block> block, Supplier<? extends Item> ingot, Supplier<? extends Item> nugget) {
    super(block);
    this.ingot = ingot;
    this.nugget = nugget;
    this.blockTag = BlockTags.create(commonResource("storage_blocks/" + tagName));
    this.blockItemTag = getTag("storage_blocks/" + tagName);
    this.ingotTag = getTag("ingots/" + tagName);
    this.nuggetTag = getTag("nuggets/" + tagName);
  }

  /** Gets the ingot for this object */
  public Item getIngot() {
    return ingot.get();
  }

  /** Gets the ingot for this object */
  public Item getNugget() {
    return nugget.get();
  }

  public TagKey<Block> getBlockTag() {
    return blockTag;
  }

  public TagKey<Item> getBlockItemTag() {
    return blockItemTag;
  }

  public TagKey<Item> getIngotTag() {
    return ingotTag;
  }

  public TagKey<Item> getNuggetTag() {
    return nuggetTag;
  }

  /**
   * Creates a tag for a resource
   * @param name  Tag name
   * @return  Tag
   */
  private static TagKey<Item> getTag(String name) {
    return ItemTags.create(commonResource(name));
  }

  @Override
  public List<ItemLike> values() {
    return List.of(get(), getIngot(), getIngot());
  }

  @Override
  public void forEach(Consumer<? super ItemLike> consumer) {
    consumer.accept(get());
    consumer.accept(getIngot());
    consumer.accept(getNugget());
  }
}
