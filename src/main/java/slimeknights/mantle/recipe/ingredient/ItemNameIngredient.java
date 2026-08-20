package slimeknights.mantle.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Ingredient used by datagen for items supplied by another mod.
 *
 * <p>The item is deliberately stored as an identifier instead of a registry
 * holder. That is important for compatibility recipes: the optional item may
 * not be registered in this instance, but the recipe still needs to decode so
 * NeoForge can apply its {@code item_exists} condition.</p>
 */
public final class ItemNameIngredient implements ICustomIngredient {
  private static final Codec<List<Identifier>> NAMES_CODEC = Codec.withAlternative(
    Identifier.CODEC.listOf(),
    Identifier.CODEC.xmap(List::of, List::getFirst));

  public static final MapCodec<ItemNameIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    NAMES_CODEC.fieldOf("item").forGetter(ingredient -> ingredient.names)
  ).apply(instance, ItemNameIngredient::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, ItemNameIngredient> STREAM_CODEC = StreamCodec.composite(
    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), ingredient -> ingredient.names,
    ItemNameIngredient::new
  );

  private final List<Identifier> names;

  private ItemNameIngredient(List<Identifier> names) {
    if (names.isEmpty()) {
      throw new IllegalArgumentException("Item name ingredient cannot be empty");
    }
    this.names = List.copyOf(names);
  }

  /** Creates a name ingredient for an item supplied by another mod. */
  public static Ingredient from(List<Identifier> names) {
    return new ItemNameIngredient(names).toVanilla();
  }

  /** Creates a name ingredient for one or more items supplied by another mod. */
  public static Ingredient from(Identifier... names) {
    return from(Arrays.asList(names));
  }

  @Override
  public boolean test(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return names.contains(itemId);
  }

  @Override
  public Stream<Holder<Item>> items() {
    // Optional compatibility items are absent until their mod is installed;
    // only expose holders that actually exist for recipe displays and sync.
    return names.stream()
      .map(BuiltInRegistries.ITEM::getOptional)
      .flatMap(Optional::stream)
      .map(Item::builtInRegistryHolder);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.ITEM_NAME.get();
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof ItemNameIngredient other && names.equals(other.names);
  }

  @Override
  public int hashCode() {
    return Objects.hash(names);
  }
}
