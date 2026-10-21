package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.codec.MantleCodecs;
import slimeknights.mantle.network.codec.MantleStreamCodecs;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Potion matching with native holder sets, including legacy single-item JSON. */
public class PotionIngredient implements ICustomIngredient {
  public static final MapCodec<PotionIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    MantleCodecs.POTION_SET.fieldOf("potion").forGetter(PotionIngredient::potion),
    MantleCodecs.ITEM_SET.fieldOf("items").forGetter(PotionIngredient::itemSet)
  ).apply(instance, PotionIngredient::new));
  private static final MapCodec<PotionIngredient> LEGACY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ItemIngredient.ITEMS_CODEC.optionalFieldOf("item", List.of()).forGetter(i -> i.itemSet.stream().toList()),
    ItemIngredient.ITEM_TAG_CODEC.optionalFieldOf("tag").forGetter(i -> i.itemSet.unwrapKey()),
    MantleCodecs.POTION_SET.fieldOf("potion").forGetter(PotionIngredient::potion)
  ).apply(instance, (items, tag, potion) -> new PotionIngredient(potion,
    tag.<HolderSet<Item>>map(IngredientHelper::itemSet).orElseGet(() -> HolderSet.direct(items)))));
  public static final MapCodec<PotionIngredient> MAP_CODEC = MapCodec.assumeMapUnsafe(Codec.withAlternative(CODEC.codec(), LEGACY_CODEC.codec()));
  public static final StreamCodec<RegistryFriendlyByteBuf,PotionIngredient> STREAM_CODEC = StreamCodec.composite(
    MantleStreamCodecs.POTION_SET, PotionIngredient::potion,
    MantleStreamCodecs.ITEM_SET, PotionIngredient::itemSet, PotionIngredient::new);

  private final HolderSet<Potion> potion;
  private final HolderSet<Item> itemSet;

  public PotionIngredient(HolderSet<Potion> potion, HolderSet<Item> items) {
    this.potion = potion;
    this.itemSet = items;
  }
  public HolderSet<Potion> potion() { return potion; }
  public HolderSet<Item> itemSet() { return itemSet; }
  public static Ingredient of(HolderSet<Potion> potion, HolderSet<Item> items) { return new PotionIngredient(potion, items).toVanilla(); }
  public static Ingredient of(Holder<Potion> potion, HolderSet<Item> items) { return of(HolderSet.direct(potion), items); }
  public static Ingredient of(Holder<Potion> potion, List<? extends ItemLike> items) { return of(potion, IngredientHelper.itemSet(items)); }
  public static Ingredient of(Holder<Potion> potion, ItemLike... items) { return of(potion, Arrays.asList(items)); }
  public static Ingredient of(Holder<Potion> potion, TagKey<Item> tag) { return of(potion, IngredientHelper.itemSet(tag)); }
  public static Ingredient of(Potion potion, ItemLike... items) { return of(BuiltInRegistries.POTION.wrapAsHolder(potion), items); }
  public static boolean matches(PotionContents contents, HolderSet<Potion> potions) {
    return contents.customEffects().isEmpty() && contents.potion().filter(potions::contains).isPresent();
  }
  public static boolean matches(DataComponentHolder stack, HolderSet<Potion> potions) {
    return matches(stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY), potions);
  }
  @Override public boolean test(ItemStack stack) { return !stack.isEmpty() && itemSet.contains(stack.typeHolder()) && matches(stack, potion); }
  @Override public boolean isSimple() { return false; }
  @Override public IngredientType<?> getType() { return MantleIngredientTypes.POTION.get(); }
  @Override public Stream<Holder<Item>> items() { return itemSet.stream(); }
  @Override public SlotDisplay display() {
    return new SlotDisplay.Composite(potion.stream().flatMap(holder -> itemSet.stream().map(item -> (SlotDisplay)
      new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(item, 1,
        DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(holder)).build())))).toList());
  }
  @Override public boolean equals(Object object) { return object instanceof PotionIngredient other && potion.equals(other.potion) && itemSet.equals(other.itemSet); }
  @Override public int hashCode() { return Objects.hash(potion, itemSet); }
}
