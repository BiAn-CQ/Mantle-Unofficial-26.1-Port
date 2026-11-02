package slimeknights.mantle.datagen.tag;

import slimeknights.mantle.datagen.MantleTags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.Mantle;

import java.util.concurrent.CompletableFuture;

import static net.neoforged.neoforge.common.Tags.Fluids.BEETROOT_SOUP;
import static net.neoforged.neoforge.common.Tags.Fluids.LAVA;
import static net.neoforged.neoforge.common.Tags.Fluids.MUSHROOM_STEW;
import static net.neoforged.neoforge.common.Tags.Fluids.RABBIT_STEW;
import static slimeknights.mantle.datagen.MantleTags.Fluids.SOUP;
import static net.neoforged.neoforge.common.Tags.Fluids.WATER;

/** Provider for tags added by mantle, generally not useful for other mods */
@Internal
public class MantleFluidTagProvider extends FluidTagsProvider {
  public MantleFluidTagProvider(PackOutput output, CompletableFuture<Provider> holders) {
    super(output, holders, Mantle.modId);
  }

  @Override
  protected void addTags(Provider pProvider) {
    getOrCreateRawBuilder(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, Mantle.getResource("water")))
      .addTag(net.neoforged.neoforge.common.Tags.Fluids.WATER.location());
    getOrCreateRawBuilder(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, Mantle.getResource("lava")))
      .addTag(net.neoforged.neoforge.common.Tags.Fluids.LAVA.location());
    this.tag(SOUP)
      .addOptionalTag(BEETROOT_SOUP)
      .addOptionalTag(MUSHROOM_STEW)
      .addOptionalTag(RABBIT_STEW);
    fluid(slimeknights.mantle.fluid.MantleFluids.potion);
    fluid(slimeknights.mantle.fluid.MantleFluids.powderSnow);
    fluid(slimeknights.mantle.fluid.MantleFluids.honey);
    fluid(slimeknights.mantle.fluid.MantleFluids.beetrootSoup);
    fluid(slimeknights.mantle.fluid.MantleFluids.mushroomStew);
    fluid(slimeknights.mantle.fluid.MantleFluids.rabbitStew);
    tag(MantleTags.Fluids.HIDE_IN_DISPLAY).addOptionalTag(net.neoforged.neoforge.common.Tags.Fluids.POTION)
      .addOptionalTag(net.neoforged.neoforge.common.Tags.Fluids.HIDDEN_FROM_RECIPE_VIEWERS);
  }

  private void fluid(slimeknights.mantle.registration.object.FluidObject<?> fluid) {
    getOrCreateRawBuilder(java.util.Objects.requireNonNull(fluid.getCommonTag())).addOptionalElement(fluid.id());
  }
  private void fluid(slimeknights.mantle.registration.object.FlowingFluidObject<?> fluid) {
    tag(java.util.Objects.requireNonNull(fluid.getCommonTag())).addOptionalTag(fluid.getLocalTag());
    getOrCreateRawBuilder(fluid.getLocalTag()).addOptionalElement(fluid.id()).addOptionalElement(fluid.id().withPrefix("flowing_"));
  }

  @Override
  public String getName() {
    return "Mantle Fluid Tag Provider";
  }
}
