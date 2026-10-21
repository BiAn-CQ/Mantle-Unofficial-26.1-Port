package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.IAmLoadable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.RegistryCodecLoadable;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple displayable ingredient type for fluids.
 */
@SuppressWarnings({"unused", "ClassEscapesDefinedScope"})  // Loadable API is much easier if the methods return the right types
public abstract class FluidIngredient implements IAmLoadable {
  /** Empty fluid ingredient, matching empty stacks. This ingredient does not parse from JSON, use use defaulting methods if you wish to use it */
  public static final FluidMatch EMPTY = new FluidMatch(Fluids.EMPTY, 0);


  /* Loadables */

  /** Creates a builder with set and tag */
  private static EitherLoadable.TypedBuilder<FluidIngredient> loadableBuilder() {
    return EitherLoadable.<FluidIngredient>typed().key("fluid", FLUID_MATCH).key("tag", TAG_MATCH);
  }
  /** Loadable for network writing of fluids, using optional stacks so empty ingredients can be synchronized. */
  private static final Loadable<FluidIngredient> NETWORK = new RegistryCodecLoadable<>(FluidStack.OPTIONAL_CODEC, FluidStack.OPTIONAL_STREAM_CODEC)
    .list(0).flatXmap(fluids -> FluidIngredient.of(fluids.stream().map(FluidIngredient::of).toList()), FluidIngredient::getAllFluids);
  /** Loadable for fluid matches */
  private static final RecordLoadable<FluidMatch> FLUID_MATCH = RecordLoadable.create(Loadables.FLUID.requiredField("fluid", i -> i.fluid), IntLoadable.FROM_ONE.requiredField("amount", i -> i.amount), FluidIngredient::of);
  /** Loadable for tag matches */
  private static final RecordLoadable<TagMatch> TAG_MATCH = RecordLoadable.create(Loadables.FLUID_TAG.requiredField("tag", i -> i.tag), IntLoadable.FROM_ONE.requiredField("amount", i -> i.amount), FluidIngredient::of);
  /** Loadable for tag matches */
  private static final Loadable<Compound> COMPOUND = loadableBuilder().build(NETWORK).list(2).flatXmap(Compound::new, c -> c.ingredients);
  /** Loadable for any fluid ingredient */
  private static final Loadable<FluidIngredient> LEGACY_LOADABLE = loadableBuilder().array(COMPOUND).build(NETWORK);
  private static final Loadable<net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient> NATIVE_LOADABLE =
    new RegistryCodecLoadable<>(net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient.CODEC,
      net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient.STREAM_CODEC);
  /** Accepts both the old Mantle format and native sized ingredients without dropping component predicates. */
  public static final Loadable<FluidIngredient> LOADABLE = new Loadable<>() {
    @Override
    public FluidIngredient convert(JsonElement element, String key, slimeknights.mantle.util.typed.TypedMap context) {
      if (element.isJsonArray()) {
        if (element.getAsJsonArray().size() < 2) throw new com.google.gson.JsonSyntaxException("Fluid ingredient array must contain at least two entries");
        return of(element.getAsJsonArray().asList().stream().map(child -> convert(child,key,context)).toList());
      }
      if (element.isJsonObject() && element.getAsJsonObject().has("ingredient")) {
        return of(NATIVE_LOADABLE.convert(element, key, context));
      }
      return LEGACY_LOADABLE.convert(element, key, context);
    }

    @Override
    public JsonElement serialize(FluidIngredient value, slimeknights.mantle.util.typed.TypedMap context) {
      return value instanceof Native nativeIngredient
        ? NATIVE_LOADABLE.serialize(nativeIngredient.ingredient, context)
        : LEGACY_LOADABLE.serialize(value, context);
    }

    @Override
    public FluidIngredient decode(FriendlyByteBuf buffer, slimeknights.mantle.util.typed.TypedMap context) {
      return switch (buffer.readByte()) {
        case 0 -> NETWORK.decode(buffer, context);
        case 1 -> of(NATIVE_LOADABLE.decode(buffer, context));
        case 2 -> of(buffer.readList(input -> decode(input, context)));
        default -> throw new io.netty.handler.codec.DecoderException("Unknown fluid ingredient format");
      };
    }

    @Override
    public void encode(FriendlyByteBuf buffer, FluidIngredient value, slimeknights.mantle.util.typed.TypedMap context) {
      if (value instanceof Native nativeIngredient) {
        buffer.writeByte(1);
        NATIVE_LOADABLE.encode(buffer, nativeIngredient.ingredient, context);
      } else if (value instanceof Compound compound) {
        buffer.writeByte(2);
        buffer.writeCollection(compound.ingredients, (output, child) -> encode(output, child, context));
      } else {
        buffer.writeByte(0);
        NETWORK.encode(buffer, value, context);
      }
    }
  };

  /** Wraps a native sized ingredient, retaining custom fluid and component matching. */
  public static FluidIngredient of(net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient ingredient) {
    return new Native(ingredient);
  }

  private static final class Native extends FluidIngredient {
    private final net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient ingredient;

    private Native(net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient ingredient) {
      this.ingredient = ingredient;
    }

    @Override public boolean test(Fluid fluid) { return ingredient.ingredient().fluids().contains(fluid.builtInRegistryHolder()); }
    @Override public boolean test(FluidStack stack) { return ingredient.test(stack); }
    @Override public int getAmount(Fluid fluid) { return ingredient.amount(); }
    @Override protected List<FluidStack> getAllFluids() {
      return IngredientHelper.displayFluids(ingredient.ingredient()).map(stack -> stack.copyWithAmount(ingredient.amount())).toList();
    }
    @Override public Loadable<FluidIngredient> loadable() { return LOADABLE; }
  }


  /* Constructors */

  /**
   * Creates a new ingredient using the given fluid and amount
   * @param fluid   Fluid to check
   * @param amount  Minimum fluid amount
   * @return  Fluid ingredient for this fluid
   */
  public static FluidMatch of(Fluid fluid, int amount) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      return EMPTY;
    }
    return new FluidMatch(fluid, amount);
  }

  /**
   * Creates a new ingredient using the given fluidstack
   * @param stack  Fluid stack
   * @return  Fluid ingredient for this fluid stack
   */
  public static FluidIngredient of(FluidStack stack) {
    return of(stack.getFluid(), stack.getAmount());
  }

  /**
   * Creates a new fluid ingredient from the given tag
   * @param fluid   Fluid tag
   * @param amount  Minimum fluid amount
   * @return  Fluid ingredient from a tag
   */
  public static TagMatch of(TagKey<Fluid> fluid, int amount) {
    return new TagMatch(fluid, amount);
  }

  /**
   * Creates a new compound ingredient from the given list of ingredients
   * @param ingredients  Ingredient list
   * @return  Compound ingredient
   */
  public static FluidIngredient of(FluidIngredient... ingredients) {
    return of(List.of(ingredients));
  }

  /**
   * Creates a new compound ingredient from the given list of ingredients
   * @param ingredients  Ingredient list
   * @return  Compound ingredient
   */
  public static FluidIngredient of(List<FluidIngredient> ingredients) {
    if (ingredients.size() == 1) {
      return ingredients.get(0);
    }
    return new Compound(ingredients);
  }


  /** Cached list of display fluids */
  private List<FluidStack> displayFluids;

  /**
   * Checks if the given fluid matches this ingredient
   * @param fluid  Fluid to check
   * @return  True if the fluid matches
   */
  public abstract boolean test(Fluid fluid);

  /**
   * Gets the amount of the given fluid needed for the recipe
   * @param fluid  Fluid to check
   * @return  Amount of the fluid needed
   */
  public abstract int getAmount(Fluid fluid);

  /** Gets the amount for the matching stack, including native component predicates. */
  public int getAmount(FluidStack stack) { return getAmount(stack.getFluid()); }

  /**
   * Checks if the given fluid stack argument matches this ingredient
   * @param stack  Fluid stack to check
   * @return  True if the fluid matches this ingredient and the amount is equal or greater than this
   */
  public boolean test(FluidStack stack) {
    Fluid fluid = stack.getFluid();
    return stack.getAmount() >= getAmount(fluid) && test(stack.getFluid());
  }

  /**
   * Gets a list of fluid stacks contained in this ingredient for display
   * @return  List of fluid stacks for this ingredient
   */
  public List<FluidStack> getFluids() {
    if (displayFluids == null) {
      displayFluids = getAllFluids().stream().filter(stack -> {
        Fluid fluid = stack.getFluid();
        return fluid.isSource(fluid.defaultFluidState());
      }).collect(Collectors.toList());
    }
    return displayFluids;
  }

  /**
   * Gets a list of fluid stacks contained in this ingredient for networking, may include flowing fluids
   * @return  List of fluid stacks for this ingredient
   */
  protected abstract List<FluidStack> getAllFluids();

  /**
   * Serializes the Fluid Ingredient into JSON
   * @return  FluidIngredient JSON
   */
  public JsonElement serialize() {
    return LOADABLE.serialize(this);
  }

  /** @deprecated use {@link #LOADABLE} with {@link Loadable#getIfPresent(JsonObject, String)} */
  @Deprecated(forRemoval = true)
  public static FluidIngredient deserialize(JsonObject parent, String key) {
    return LOADABLE.getIfPresent(parent, key);
  }

  /** @deprecated use {@link #LOADABLE} with {@link Loadable#convert(JsonElement, String)} */
  @Deprecated(forRemoval = true)
  public static FluidIngredient deserialize(JsonElement element, String key) {
    return LOADABLE.convert(element, key);
  }

  /** @deprecated use {@link #LOADABLE} with {@link Loadable#encode(FriendlyByteBuf, Object)} */
  @Deprecated(forRemoval = true)
  public void write(FriendlyByteBuf buffer) {
    LOADABLE.encode(buffer, this);
  }

  /** @deprecated use {@link #LOADABLE} with {@link Loadable#decode(FriendlyByteBuf)}*/
  @Deprecated(forRemoval = true)
  public static FluidIngredient read(FriendlyByteBuf buffer) {
    return LOADABLE.decode(buffer);
  }


  /**
   * Fluid ingredient that matches a single fluid
   */
  private static class FluidMatch extends FluidIngredient {

    private final Fluid fluid;
    private final int amount;

    private FluidMatch(Fluid fluid, int amount) {
      this.fluid = fluid;
      this.amount = amount;
    }

    @Override
    public Loadable<FluidMatch> loadable() {
      return FLUID_MATCH;
    }

    @Override
    public boolean test(Fluid fluid) {
      return fluid == this.fluid;
    }

    @Override
    public int getAmount(Fluid fluid) {
      return amount;
    }

    @Override
    public List<FluidStack> getAllFluids() {
      return Collections.singletonList(new FluidStack(fluid, amount));
    }
  }

  /**
   * Fluid ingredient that matches a tag
   */
  private static class TagMatch extends FluidIngredient {
    private final TagKey<Fluid> tag;
    private final int amount;

    private TagMatch(TagKey<Fluid> tag, int amount) {
      this.tag = tag;
      this.amount = amount;
    }

    @Override
    public Loadable<TagMatch> loadable() {
      return TAG_MATCH;
    }

    @SuppressWarnings("deprecation")  // its a perfectly reasonable method to use mojang
    @Override
    public boolean test(Fluid fluid) {
      return fluid.is(tag);
    }

    @Override
    public int getAmount(Fluid fluid) {
      return amount;
    }

    @SuppressWarnings("deprecation")  // let me get tags Forge
    @Override
    public List<FluidStack> getAllFluids() {
      return RegistryHelper.getTagValueStream(BuiltInRegistries.FLUID, tag)
                          .map(fluid -> new FluidStack(fluid, amount))
                          .toList();
    }
  }

  /**
   * Fluid ingredient that matches a list of ingredients
   */
  private static class Compound extends FluidIngredient {
    private final List<FluidIngredient> ingredients;

    private Compound(List<FluidIngredient> ingredients) {
      this.ingredients = List.copyOf(ingredients);
    }

    @Override
    public Loadable<Compound> loadable() {
      return COMPOUND;
    }

    @Override
    public boolean test(Fluid fluid) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(fluid)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean test(FluidStack stack) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(stack)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public int getAmount(Fluid fluid) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(fluid)) {
          return ingredient.getAmount(fluid);
        }
      }
      return 0;
    }

    @Override
    public int getAmount(FluidStack stack) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(stack)) return ingredient.getAmount(stack);
      }
      return 0;
    }

    @Override
    public List<FluidStack> getAllFluids() {
      return ingredients.stream()
                        .flatMap(ingredient -> ingredient.getFluids().stream())
                        .collect(Collectors.toList());
    }
  }
}
