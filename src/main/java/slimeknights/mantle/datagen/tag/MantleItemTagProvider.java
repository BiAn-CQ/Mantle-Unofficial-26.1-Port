package slimeknights.mantle.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.fluid.MantleFluids;
import java.util.concurrent.CompletableFuture;

public class MantleItemTagProvider extends BuiltinRegistryTagProvider<Item> {
  public MantleItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
    super(output, BuiltInRegistries.ITEM, lookup, Mantle.modId);
  }
  protected void addTags(HolderLookup.Provider lookup) {
    tag(MantleTags.Items.EMPTY_FLUID_CONTAINERS).add(Items.BUCKET);
    tag(MantleTags.Items.FILLED_FLUID_CONTAINERS);
    getOrCreateRawBuilder(MantleTags.Items.POTION_BUCKETS).addOptionalElement(MantleFluids.potion.id().withSuffix("_bucket"));
    tag(net.neoforged.neoforge.common.Tags.Items.POTIONS).addOptionalTag(MantleTags.Items.POTION_BUCKETS);
  }
}
