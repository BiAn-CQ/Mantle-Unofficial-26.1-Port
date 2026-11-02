package slimeknights.mantle.plugin.jei.fluid;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.holdersets.NotHolderSet;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.fluid.MantleFluids;
import slimeknights.mantle.fluid.transfer.EmptyPotionTransfer;
import slimeknights.mantle.fluid.transfer.FillFluidCopyDataTransfer;
import slimeknights.mantle.fluid.transfer.IDisplayFluidTransfer;
import slimeknights.mantle.fluid.transfer.IDisplayFluidTransfer.SlotLink;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.mantle.plugin.jei.NamedRecipeCategory;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.fluid.PotionFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Category for displaying fluid container transfer recipes in JEI */
public class FluidContainerTransferCategory extends NamedRecipeCategory<IDisplayFluidTransfer> {

  private static final Component TITLE = Mantle.makeComponent("jei", "fluid_transfer");
  private static final Identifier SCALA_SPRITE = Mantle.getResource("jei/scala");

  private final IDrawable scala;
  public FluidContainerTransferCategory(IGuiHelper guiHelper) {
    super(MantleJEIConstants.FLUID_TRANSFER, TITLE, guiHelper.createDrawableItemLike(Items.POTION), 68, 37);
    this.scala = guiHelper.drawableBuilder(Mantle.getResource("textures/gui/sprites/jei/scala.png"), 0, 0, 16, 16).setTextureSize(16, 16).build();
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, NamedRecipe<IDisplayFluidTransfer> holder, IFocusGroup focuses) {
    IDisplayFluidTransfer recipe = holder.recipe();
    // add slots
    IRecipeSlotBuilder inputItemSlot   = builder.addInputSlot(  1,  1).setStandardSlotBackground().addItemStacks(recipe.inputItems());
    IRecipeSlotBuilder resultItemSlot  = builder.addOutputSlot(51,  1).setStandardSlotBackground().addItemStacks(recipe.resultItems());
    IRecipeSlotBuilder inputFluidSlot  = builder.addInputSlot(  1, 20).setStandardSlotBackground()
      .addRichTooltipCallback(FluidTooltipCallback.UNITS)
      .setOverlay(scala, 0, 0).addIngredients(NeoForgeTypes.FLUID_STACK, recipe.inputFluids());
    IRecipeSlotBuilder resultFluidSlot = builder.addOutputSlot(51, 20).setStandardSlotBackground()
      .addRichTooltipCallback(FluidTooltipCallback.UNITS)
      .setOverlay(scala, 0, 0).addIngredients(NeoForgeTypes.FLUID_STACK, recipe.resultFluids());

    // add focus links between anything the same size
    List<SlotLink> links = recipe.linkedSlots();
    if (links.size() > 1) {
      List<IRecipeSlotBuilder> linked = new ArrayList<>(links.size());
      int size = links.getFirst().getSize(recipe);
      for (SlotLink link : links) {
        if (link.getSize(recipe) == size) {
          linked.add(switch (link) {
            case INPUT_ITEMS -> inputItemSlot;
            case RESULT_ITEMS -> resultItemSlot;
            case INPUT_FLUIDS -> inputFluidSlot;
            case RESULT_FLUIDS -> resultFluidSlot;
          });
        }
      }
      if (linked.size() > 1) {
        builder.createFocusLink(linked);
      }
    }
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, NamedRecipe<IDisplayFluidTransfer> recipe, IFocusGroup focuses) {
    builder.addRecipeArrowWidget().setPosition(23, 10);
  }


  /** Builds display recipes by simulating real 26.1 container transactions on disposable stack accesses. */
  private static net.neoforged.neoforge.transfer.access.ItemAccess containerAccess(ItemStack stack) {
    // A stack access cannot replace its Item. Bucket exchanges require a mutable slot.
    var inventory = new net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler(1);
    inventory.set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(stack), stack.getCount());
    return net.neoforged.neoforge.transfer.access.ItemAccess.forHandlerIndexStrict(inventory, 0);
  }

  public static List<NamedRecipe<IDisplayFluidTransfer>> generateRecipes() {
    List<NamedRecipe<IDisplayFluidTransfer>> recipes = new ArrayList<>();
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(MantleTags.Items.FILLED_FLUID_CONTAINERS)) {
      var access = containerAccess(new ItemStack(item));
      var handler = access.getCapability(Capabilities.Fluid.ITEM);
      if (handler == null || handler.size() == 0) continue;
      var resource = handler.getResource(0);
      int amount = (int)Math.min(Integer.MAX_VALUE, handler.getAmountAsLong(0));
      if (resource.isEmpty() || amount == 0) continue;
      int extracted;
      try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
        extracted = handler.extract(0, resource, amount, transaction);
        if (extracted > 0) transaction.commit();
      }
      if (extracted > 0) recipes.add(NamedRecipe.of(Objects.requireNonNull(item.getKey()).identifier().withPrefix("generated/draining/"), List.of(),
        IDisplayFluidTransfer.builder().inputItem(new ItemStack(item)).resultFluid(resource.toStack(extracted))
          .resultItem(access.getResource().toStack(access.getAmount())).build()));
    }
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(MantleTags.Items.EMPTY_FLUID_CONTAINERS)) {
      List<ItemStack> filled = new ArrayList<>();
      List<FluidStack> fluids = new ArrayList<>();
      for (Fluid fluid : BuiltInRegistries.FLUID) {
        if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState()) || fluid.builtInRegistryHolder().is(MantleTags.Fluids.HIDE_IN_DISPLAY)) continue;
        var access = containerAccess(new ItemStack(item));
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null || handler.size() == 0) continue;
        var resource = net.neoforged.neoforge.transfer.fluid.FluidResource.of(fluid);
        int size = (int)Math.min(Integer.MAX_VALUE, handler.getCapacityAsLong(0, resource));
        if (size <= 0) continue;
        int inserted;
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
          inserted = handler.insert(0, resource, size, transaction);
          if (inserted == size) transaction.commit();
        }
        if (inserted == size) {
          filled.add(access.getResource().toStack(access.getAmount()));
          fluids.add(resource.toStack(size));
        }
      }
      if (!filled.isEmpty()) {
        Identifier id = Objects.requireNonNull(item.getKey()).identifier().withPrefix("generated/");
        recipes.add(NamedRecipe.of(id.withSuffix("/filling"), List.of(), IDisplayFluidTransfer.builder()
          .inputItem(new ItemStack(item)).inputFluids(List.copyOf(fluids)).resultItems(List.copyOf(filled))
          .linkedSlots(SlotLink.INPUT_FLUIDS, SlotLink.RESULT_ITEMS).build()));
        recipes.add(NamedRecipe.of(id.withSuffix("/draining"), List.of(), IDisplayFluidTransfer.builder()
          .inputItems(List.copyOf(filled)).resultFluids(List.copyOf(fluids)).resultItem(new ItemStack(item))
          .linkedSlots(SlotLink.INPUT_ITEMS, SlotLink.RESULT_FLUIDS).build()));
      }
    }
    // Potion variants are generated once, independently of the number of empty-container tags.
    if (MantleFluids.potion.isBound()) {
      Identifier id = MantleFluids.potion.id().withPrefix("generated/");
      recipes.add(NamedRecipe.of(id.withSuffix("/draining"), List.of(),
        new EmptyPotionTransfer(Ingredient.of(MantleFluids.potion), ItemOutput.fromItem(Items.BUCKET), FluidType.BUCKET_VOLUME)));
      recipes.add(NamedRecipe.of(id.withSuffix("/filling"), List.of(), new FillFluidCopyDataTransfer(
        Ingredient.of(Items.BUCKET), ItemOutput.fromItem(MantleFluids.potion),
        new SizedFluidIngredient(PotionFluidIngredient.of(new NotHolderSet<>(BuiltInRegistries.POTION, HolderSet.direct(Potions.WATER)), Tags.Fluids.POTION), FluidType.BUCKET_VOLUME),
        List.of(DataComponents.POTION_CONTENTS))));
    }
    return List.copyOf(recipes);
  }
}
