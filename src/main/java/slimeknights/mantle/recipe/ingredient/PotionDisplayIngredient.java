package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Ingredient that shows all potion variants on the displayed item list */
public final class PotionDisplayIngredient extends ItemIngredient {
  public static final MapCodec<PotionDisplayIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ITEMS_CODEC.optionalFieldOf("item", List.of()).forGetter(ingredient -> ingredient.explicitItems),
    ITEM_TAG_CODEC.optionalFieldOf("tag").forGetter(ingredient -> optionalTag(ingredient.tag))
  ).apply(instance, (items, tag) -> new PotionDisplayIngredient(items, tag.orElse(null))));

  private PotionDisplayIngredient(List<Holder<Item>> items, TagKey<Item> tag) {
    super(items, tag);
  }

  /** Creates a ingredient matching a list of items */
  public static Ingredient of(List<? extends ItemLike> items) {
    return new PotionDisplayIngredient(toItems(items), null).toVanilla();
  }

  /** Creates a ingredient matching a list of items */
  public static Ingredient of(ItemLike... items) {
    return of(Arrays.asList(items));
  }

  /** Creates a ingredient matching a tag */
  public static Ingredient of(TagKey<Item> tag) {
    return new PotionDisplayIngredient(List.of(), tag).toVanilla();
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.POTION_DISPLAY.get();
  }

  @Override
  public SlotDisplay display() {
    List<SlotDisplay> displays = items().flatMap(item -> BuiltInRegistries.POTION.listElements().map(potion -> {
      var stack = PotionContents.createItemStack(item.value(), potion);
      return (SlotDisplay)new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    })).toList();
    return new SlotDisplay.Composite(displays);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof PotionDisplayIngredient other
      && explicitItems.equals(other.explicitItems) && Objects.equals(tag, other.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(explicitItems, tag);
  }
}
