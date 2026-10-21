package slimeknights.mantle.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import slimeknights.mantle.registration.object.BlockItemTagKey;

import java.util.concurrent.CompletableFuture;

/** Native block tag provider supporting paired block/item tag keys. */
public abstract class BlockTagProviderExtension extends BlockTagsProvider {
  public BlockTagProviderExtension(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, String modId) { super(output,lookup,modId); }
  @Override protected BlockItemTagAppender tag(TagKey<Block> tag) { return new BlockItemTagAppender(super.tag(tag)); }
  protected BlockItemTagAppender tag(BlockItemTagKey tag) { return tag(tag.block()); }

  public static class BlockItemTagAppender implements TagAppender<Block,Block> {
    private final TagAppender<Block,Block> delegate;
    protected BlockItemTagAppender(TagAppender<Block,Block> delegate) { this.delegate=delegate; }
    public BlockItemTagAppender addTag(BlockItemTagKey tag) { return addTag(tag.block()); }
    @Override public BlockItemTagAppender add(Block block) { delegate.add(block); return this; }
    @Override public BlockItemTagAppender addOptional(Block block) { delegate.addOptional(block); return this; }
    @Override public BlockItemTagAppender addTag(TagKey<Block> tag) { delegate.addTag(tag); return this; }
    @Override public BlockItemTagAppender addOptionalTag(TagKey<Block> tag) { delegate.addOptionalTag(tag); return this; }
    @Override public BlockItemTagAppender add(TagEntry entry) { delegate.add(entry); return this; }
    @Override public BlockItemTagAppender replace(boolean replace) { delegate.replace(replace); return this; }
    @Override public BlockItemTagAppender remove(Block block) { delegate.remove(block); return this; }
    @Override public BlockItemTagAppender remove(TagKey<Block> tag) { delegate.remove(tag); return this; }
  }
}
