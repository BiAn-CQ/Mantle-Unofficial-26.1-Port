package slimeknights.mantle.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.registration.object.BlockItemTagKey;

import java.util.concurrent.CompletableFuture;

/** Native item tag provider retaining block-to-item tag copying. */
public abstract class ItemTagProviderExtension extends IntrinsicHolderTagsProvider<Item> {
  private final CompletableFuture<TagLookup<Block>> blockTags;
  private TagLookup<Block> resolvedBlockTags;
  public ItemTagProviderExtension(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
      CompletableFuture<TagLookup<Block>> blockTags, String modId) {
    this(output,lookup,CompletableFuture.completedFuture(TagLookup.empty()),blockTags,modId);
  }
  public ItemTagProviderExtension(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
      CompletableFuture<TagLookup<Item>> parent, CompletableFuture<TagLookup<Block>> blockTags, String modId) {
    super(output,Registries.ITEM,lookup,parent,item -> item.builtInRegistryHolder().key(),modId);
    this.blockTags=blockTags;
  }
  @Override protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
    return blockTags.thenCompose(tags -> { resolvedBlockTags=tags; return super.createContentsProvider(); });
  }
  protected void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
    var target=getOrCreateRawBuilder(itemTag);
    var source=resolvedBlockTags.apply(blockTag).orElseThrow(() -> new IllegalArgumentException("Missing block tag " + blockTag.location()));
    for (TagEntry entry:source.build()) {
      if (entry.isTag()) { target.add(entry); continue; }
      var block=BuiltInRegistries.BLOCK.get(entry.getId());
      if (block.isEmpty()) { target.add(entry); continue; }
      Item item=block.get().value().asItem();
      if (item!=Items.AIR) {
        var id=BuiltInRegistries.ITEM.getKey(item);
        if (entry.isRequired()) { target.addElement(id); } else { target.addOptionalElement(id); }
      }
    }
  }
  protected void copy(BlockItemTagKey tags) { copy(tags.block(),tags.item()); }
  protected void copy(BlockItemTagKey... tags) { for (var pair:tags) { copy(pair); } }
  @Override protected BlockItemTagAppender tag(TagKey<Item> tag) { return new BlockItemTagAppender(super.tag(tag)); }
  protected BlockItemTagAppender tag(BlockItemTagKey tag) { return tag(tag.item()); }

  public static class BlockItemTagAppender implements net.minecraft.data.tags.TagAppender<Item,Item> {
    private final net.minecraft.data.tags.TagAppender<Item,Item> delegate;
    protected BlockItemTagAppender(net.minecraft.data.tags.TagAppender<Item,Item> delegate) { this.delegate = delegate; }
    public BlockItemTagAppender addTag(BlockItemTagKey tag) { return addTag(tag.item()); }
    public BlockItemTagAppender addTags(BlockItemTagKey... tags) { for (var tag : tags) addTag(tag); return this; }
    @Override public BlockItemTagAppender add(Item item) { delegate.add(item); return this; }
    @Override public BlockItemTagAppender addOptional(Item item) { delegate.addOptional(item); return this; }
    @Override public BlockItemTagAppender addTag(TagKey<Item> tag) { delegate.addTag(tag); return this; }
    @Override public BlockItemTagAppender addOptionalTag(TagKey<Item> tag) { delegate.addOptionalTag(tag); return this; }
    @Override public BlockItemTagAppender add(TagEntry entry) { delegate.add(entry); return this; }
    @Override public BlockItemTagAppender replace(boolean replace) { delegate.replace(replace); return this; }
    @Override public BlockItemTagAppender remove(Item item) { delegate.remove(item); return this; }
    @Override public BlockItemTagAppender remove(TagKey<Item> tag) { delegate.remove(tag); return this; }
  }

}
