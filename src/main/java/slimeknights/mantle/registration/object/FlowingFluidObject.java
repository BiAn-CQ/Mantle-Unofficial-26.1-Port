package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing registry entries for a fluid with a flowing form.
 * @param <F>  Fluid class
 */
@SuppressWarnings("WeakerAccess")
public class FlowingFluidObject<F extends FlowingFluid> extends FluidObject<F> {
  /** Tag in the mod namespace, exact match */
  @Getter @Nonnull
  private final TagKey<Fluid> localTag;
  private final Supplier<? extends F> flowing;
  @Nullable
  private final Supplier<? extends LiquidBlock> block;

  /** Main constructor */
  public FlowingFluidObject(Identifier id, @Nullable String tagName, Supplier<? extends FluidType> type, Supplier<? extends F> still, Supplier<? extends F> flowing, @Nullable Supplier<? extends LiquidBlock> block) {
    super(id, tagName, type, still);
    this.localTag = FluidTags.create(id);
    this.flowing = flowing;
    this.block = block;
  }

  /**
   * Gets the still form of this fluid. Alias for {@link #get()} for code readability.
   * @return  Still form
   * @see #get()
   */
  public F getStill() {
    return get();
  }

  /**
   * Gets the flowing form of this fluid
   * @return  flowing form
   */
  public F getFlowing() {
    return Objects.requireNonNull(flowing.get(), "Fluid object missing flowing fluid");
  }

  @SuppressWarnings("unchecked")
  public net.minecraft.core.Holder<Fluid> getFlowingHolder() {
    return flowing instanceof net.minecraft.core.Holder<?> holder ? (net.minecraft.core.Holder<Fluid>)holder : getFlowing().builtInRegistryHolder();
  }

  @Override public net.minecraft.core.HolderSet<Fluid> holderSet() { return net.minecraft.core.HolderSet.direct(getHolder(), getFlowingHolder()); }

  /**
   * Gets the block form of this fluid
   * @return  Block form
   */
  @Nullable
  public LiquidBlock getBlock() {
    if (block == null) {
      return null;
    }
    return block.get();
  }


  /* Datagen helpers */

  /** Gets the primary tag used for recipes with this object */
  public TagKey<Fluid> getTag() {
    return commonTag != null ? commonTag : localTag;
  }

  @Override
  public FluidIngredient ingredient(int amount) {
    return FluidIngredient.of(getTag(), amount);
  }
}
