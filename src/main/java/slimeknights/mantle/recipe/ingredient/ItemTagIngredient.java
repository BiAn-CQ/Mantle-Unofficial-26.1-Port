package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Item tag ingredient whose contents are resolved through the live tag manager.
 * Unlike a construction-time HolderSet.Named, it is safe to enumerate before
 * tags have been bound and reflects the tag contents after a reload.
 */
public final class ItemTagIngredient extends ItemIngredient {
  public static final MapCodec<ItemTagIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ITEM_TAG_CODEC.fieldOf("tag").forGetter(ingredient -> ingredient.tag)
  ).apply(instance, ItemTagIngredient::new));

  private ItemTagIngredient(TagKey<Item> tag) {
    super(List.of(), tag);
  }

  /** Creates an ingredient matching the given item tag. */
  public static Ingredient of(TagKey<Item> tag) {
    return new ItemTagIngredient(tag).toVanilla();
  }

  @Override
  public Stream<Holder<Item>> items() {
    List<Holder<Item>> items = StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false).toList();
    // Ingredient placement is finalized before dynamic tags are bound. Keep a
    // non-empty display candidate so the recipe is retained; test() still uses
    // the live tag and therefore never treats the placeholder as a match.
    return items.isEmpty()
      ? Stream.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.BARRIER))
      : items.stream();
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.ITEM_TAG.get();
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof ItemTagIngredient other && Objects.equals(tag, other.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag);
  }
}
