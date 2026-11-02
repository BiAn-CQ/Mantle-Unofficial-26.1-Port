package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing registry entries for a fluid with no flowing form.
 * @param <F>  Fluid class
 * @see FlowingFluidObject
 */
@SuppressWarnings("WeakerAccess")
public class FluidObject<F extends Fluid> extends HolderWrapper<Fluid,F> implements ItemLike, IdAwareObject {
  /** Fluid name, used for tag creation */
  @Getter @Nonnull
  protected final Identifier id;

  /** Tag in the forge namespace, crafting equivalence */
  @Getter @Nullable
  protected final TagKey<Fluid> commonTag;
  private final Supplier<? extends FluidType> type;

  /** Main constructor */
  public FluidObject(Identifier id, @Nullable String tagName, Supplier<? extends FluidType> type, net.minecraft.core.Holder<Fluid> still) {
    super(still);
    this.id = id;
    this.commonTag = tagName == null ? null : FluidTags.create(Mantle.commonResource(tagName));
    this.type = type;
  }

  public net.minecraft.core.HolderSet<Fluid> holderSet() { return net.minecraft.core.HolderSet.direct(getHolder()); }

  /** Keeps the fluid-typed method available to already compiled dependents. */
  @Override
  public F get() { return super.get(); }

  /** Registry ID, also available before the holder is bound. */
  public Identifier id() { return id; }

  /** Gets the fluid type for this object */
  public FluidType getType() {
    return type.get();
  }

  /**
   * Gets the bucket form of this fluid.
   * @return  Bucket form, or null if no bucket
   * @see #asItem()
   */
  @Nullable
  public Item getBucket() {
    Item bucket = get().getBucket();
    if (bucket == Items.AIR) {
      return null;
    }
    return bucket;
  }

  /**
   * Gets the bucket form of this fluid
   * @return  Bucket form, or air if no bucket
   * @see #getBucket()
   */
  @Override
  public Item asItem() {
    return get().getBucket();
  }

  /**
   * Creates an ingredient from this object
   * @param amount     Ingredient amount
   * @return  Ingredient instance
   */
  public net.neoforged.neoforge.fluids.crafting.FluidIngredient ingredient() {
    return net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(commonTag != null
      ? slimeknights.mantle.recipe.ingredient.IngredientHelper.fluidSet(commonTag) : holderSet());
  }

  public net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient sizedIngredient(int amount) {
    return new net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient(ingredient(), amount);
  }

  public FluidIngredient ingredient(int amount) {
    if (commonTag != null) {
      return FluidIngredient.of(commonTag, amount);
    }
    return FluidIngredient.of(get(), amount);
  }

  /**
   * Creates a recipe result from this object
   * @param amount     Result amount
   * @return  Result instance
   */
  public FluidOutput result(int amount) {
    if (commonTag != null) {
      return FluidOutput.fromTag(commonTag, amount);
    }
    return FluidOutput.fromFluid(get(), amount);
  }
}
