package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.codec.MantleCodecs;
import slimeknights.mantle.network.codec.MantleStreamCodecs;

import java.util.List;
import java.util.stream.Stream;

/** Matches potion items while displaying their registered potion variants. */
public class PotionDisplayIngredient implements ICustomIngredient {
  public static final MapCodec<PotionDisplayIngredient> CODEC = MantleCodecs.ITEM_SET.fieldOf("items")
    .xmap(PotionDisplayIngredient::new, PotionDisplayIngredient::itemSet);
  private static final MapCodec<PotionDisplayIngredient> LEGACY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ItemIngredient.ITEMS_CODEC.optionalFieldOf("item", List.of()).forGetter(i -> i.itemSet.stream().toList()),
    ItemIngredient.ITEM_TAG_CODEC.optionalFieldOf("tag").forGetter(i -> i.itemSet.unwrapKey())
  ).apply(instance, (items, tag) -> new PotionDisplayIngredient(tag.<HolderSet<Item>>map(IngredientHelper::itemSet).orElseGet(() -> HolderSet.direct(items)))));
  public static final MapCodec<PotionDisplayIngredient> MAP_CODEC = MapCodec.assumeMapUnsafe(Codec.withAlternative(CODEC.codec(), LEGACY_CODEC.codec()));
  public static final StreamCodec<RegistryFriendlyByteBuf,PotionDisplayIngredient> STREAM_CODEC =
    MantleStreamCodecs.ITEM_SET.map(PotionDisplayIngredient::new, PotionDisplayIngredient::itemSet);

  private final HolderSet<Item> itemSet;
  public PotionDisplayIngredient(HolderSet<Item> items) { this.itemSet = items; }
  public HolderSet<Item> itemSet() { return itemSet; }
  public static Ingredient of(HolderSet<Item> items) { return new PotionDisplayIngredient(items).toVanilla(); }
  public static Ingredient of(List<? extends ItemLike> items) { return of(IngredientHelper.itemSet(items)); }
  public static Ingredient of(ItemLike... items) { return of(List.of(items)); }
  public static Ingredient of(TagKey<Item> tag) { return of(IngredientHelper.itemSet(tag)); }
  @Override public boolean test(ItemStack stack) { return !stack.isEmpty() && itemSet.contains(stack.typeHolder()); }
  @Override public Stream<Holder<Item>> items() { return itemSet.stream(); }
  @Override public boolean isSimple() { return false; }
  @Override public IngredientType<?> getType() { return MantleIngredientTypes.POTION_DISPLAY.get(); }
  @Override public SlotDisplay display() {
    return new SlotDisplay.Composite(BuiltInRegistries.POTION.listElements().flatMap(potion -> {
      DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build();
      return itemSet.stream().map(item -> (SlotDisplay)new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(item, components)));
    }).toList());
  }
  @Override public boolean equals(Object other) { return other instanceof PotionDisplayIngredient ingredient && itemSet.equals(ingredient.itemSet); }
  @Override public int hashCode() { return itemSet.hashCode(); }
}
