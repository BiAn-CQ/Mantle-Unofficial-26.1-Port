package slimeknights.mantle.recipe.ingredient.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.data.codec.MantleCodecs;
import slimeknights.mantle.registration.object.FluidObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Exact fluid container ingredient using the native fluid predicate and transaction API. */
public class FluidContainerIngredient implements net.neoforged.neoforge.common.crafting.ICustomIngredient {
  public static final MapCodec<FluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    FluidIngredient.CODEC.fieldOf("fluid").forGetter(FluidContainerIngredient::fluid),
    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidContainerIngredient::amount),
    MantleCodecs.compactList(ItemStackTemplate.CODEC).optionalFieldOf("display", List.of()).forGetter(i -> i.display),
    ItemStackTemplate.CODEC.optionalFieldOf("container").forGetter(i -> i.container)
  ).apply(instance, FluidContainerIngredient::new));
  public static final StreamCodec<RegistryFriendlyByteBuf,FluidContainerIngredient> STREAM_CODEC = StreamCodec.composite(
    FluidIngredient.STREAM_CODEC, FluidContainerIngredient::fluid,
    ByteBufCodecs.VAR_INT, FluidContainerIngredient::amount,
    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), i -> i.display,
    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional), i -> i.container,
    FluidContainerIngredient::new);

  private final FluidIngredient fluid;
  private final int amount;
  private final List<ItemStackTemplate> display;
  private final Optional<ItemStackTemplate> container;
  private FluidContainerIngredient(FluidIngredient fluid, int amount, List<ItemStackTemplate> display, Optional<ItemStackTemplate> container) {
    if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
    this.fluid = fluid;
    this.amount = amount;
    this.display = List.copyOf(display);
    this.container = container;
  }
  public FluidContainerIngredient(FluidIngredient fluid, int amount, List<ItemStack> display, ItemStack container) {
    this(fluid, amount, display.stream().filter(stack -> !stack.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).toList(),
      container.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(container)));
  }
  public FluidIngredient fluid() { return fluid; }
  public int amount() { return amount; }
  public List<ItemStack> displayStacks() { return display.stream().map(ItemStackTemplate::create).toList(); }
  public ItemStack container() { return container.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }
  public static FluidContainerIngredient withDisplay(FluidIngredient fluid, int amount, ItemStack... display) {
    return new FluidContainerIngredient(fluid, amount, List.of(display), ItemStack.EMPTY);
  }
  public static FluidContainerIngredient withDisplay(FluidIngredient fluid, int amount, ItemLike... display) {
    return new FluidContainerIngredient(fluid, amount, Arrays.stream(display).map(item -> new ItemStackTemplate(item.asItem())).toList(), Optional.empty());
  }
  public static FluidContainerIngredient forContainer(FluidIngredient fluid, int amount, ItemStack container) {
    return new FluidContainerIngredient(fluid, amount, List.of(), container);
  }
  public static FluidContainerIngredient forContainer(FluidIngredient fluid, int amount, ItemLike container) {
    return new FluidContainerIngredient(fluid, amount, List.of(), Optional.of(new ItemStackTemplate(container.asItem())));
  }
  public static FluidContainerIngredient fromFluid(FluidObject<?> fluid) {
    return withDisplay(fluid.ingredient(), FluidType.BUCKET_VOLUME, fluid);
  }
  @Override public boolean isSimple() { return false; }
  @Override public net.neoforged.neoforge.common.crafting.IngredientType<?> getType() {
    return slimeknights.mantle.recipe.ingredient.MantleIngredientTypes.FLUID_CONTAINER.get();
  }
  @Override public boolean test(ItemStack stack) {
    if (stack.isEmpty()) return false;
    ItemStackTemplate remainder = stack.getCraftingRemainder();
    if (remainder == null) return false;
    SimpleContainer copy = new SimpleContainer(stack.copyWithCount(1));
    ResourceHandler<FluidResource> handler = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(copy), 0).getCapability(Capabilities.Fluid.ITEM);
    if (handler == null || handler.size() != 1) return false;
    FluidStack contents = FluidUtil.getStack(handler, 0);
    if (contents.isEmpty() || contents.getAmount() != amount || !fluid.test(contents)) return false;
    try (Transaction transaction = Transaction.openRoot()) {
      if (handler.extract(0, FluidResource.of(contents), amount, transaction) != amount) return false;
      transaction.commit();
    }
    return ItemStack.matches(copy.getItem(0), remainder);
  }
  private Stream<ItemStack> getDisplayStacks() {
    Stream<ItemStack> direct = display.stream().map(ItemStackTemplate::create);
    if (container.isEmpty()) return direct;
    Stream<ItemStack> filled = slimeknights.mantle.recipe.ingredient.IngredientHelper.displayFluids(fluid).map(contents -> {
      SimpleContainer copy = new SimpleContainer(container.get().create());
      ResourceHandler<FluidResource> handler = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(copy), 0).getCapability(Capabilities.Fluid.ITEM);
      if (handler == null) return ItemStack.EMPTY;
      try (Transaction transaction = Transaction.openRoot()) {
        if (handler.insert(FluidResource.of(contents), amount, transaction) != amount) return ItemStack.EMPTY;
        transaction.commit();
      }
      return copy.getItem(0);
    }).filter(stack -> !stack.isEmpty());
    return Stream.concat(direct, filled);
  }
  @Override public Stream<Holder<Item>> items() {
    // Direct display templates can be inspected without constructing their stacks.
    return Stream.concat(display.stream().map(ItemStackTemplate::item), container.isPresent() ? getDisplayStacks().map(ItemStack::typeHolder) : Stream.empty()).distinct();
  }
  @Override public SlotDisplay display() {
    return new SlotDisplay.Composite(getDisplayStacks().map(stack -> (SlotDisplay)new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack))).toList());
  }
  @Override public boolean equals(Object other) {
    return other instanceof FluidContainerIngredient i && amount == i.amount && fluid.equals(i.fluid) && display.equals(i.display) && container.equals(i.container);
  }
  @Override public int hashCode() { return java.util.Objects.hash(fluid, amount, display, container); }
}
