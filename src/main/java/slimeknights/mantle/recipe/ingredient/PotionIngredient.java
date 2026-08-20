package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Simple ingredient checking for an item with a specific potion */
public final class PotionIngredient extends ItemIngredient {
  public static final MapCodec<PotionIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ITEMS_CODEC.optionalFieldOf("item", List.of()).forGetter(ingredient -> ingredient.explicitItems),
    ITEM_TAG_CODEC.optionalFieldOf("tag").forGetter(ingredient -> optionalTag(ingredient.tag)),
    Potion.CODEC.fieldOf("potion").forGetter(ingredient -> ingredient.potion)
  ).apply(instance, (items, tag, potion) -> new PotionIngredient(items, tag.orElse(null), potion)));

  private final Holder<Potion> potion;

  private PotionIngredient(List<Holder<Item>> items, TagKey<Item> itemTag, Holder<Potion> potion) {
    // potion is added in directly to the parent value stream
    super(items, itemTag);
    this.potion = potion;
  }

  public static Ingredient of(Holder<Potion> potion, List<? extends ItemLike> items) {
    return new PotionIngredient(toItems(items), null, potion).toVanilla();
  }

  /** Creates a potion ingredient matching a list of items */
  public static Ingredient of(Holder<Potion> potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  /** Creates a potion ingredient matching a tag */
  public static Ingredient of(Holder<Potion> potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion).toVanilla();
  }

  /** Creates a potion ingredient matching a list of items */
  public static Ingredient of(Potion potion, ItemLike... items) {
    return of(BuiltInRegistries.POTION.wrapAsHolder(potion), items);
  }

  @Override
  public boolean test(ItemStack stack) {
    return super.test(stack) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(potion);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.POTION.get();
  }

  @Override
  public SlotDisplay display() {
    return new SlotDisplay.Composite(items().map(item -> {
      ItemStack stack = PotionContents.createItemStack(item.value(), potion);
      return (SlotDisplay)new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }).toList());
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof PotionIngredient other
      && explicitItems.equals(other.explicitItems) && Objects.equals(tag, other.tag) && potion.equals(other.potion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(explicitItems, tag, potion);
  }
}
