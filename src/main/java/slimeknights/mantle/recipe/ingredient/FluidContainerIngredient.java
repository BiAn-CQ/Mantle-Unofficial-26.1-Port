package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.JsonHelper;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Ingredient that matches a container of fluid */
@SuppressWarnings({"unused", "removal"}) // Legacy fluid handler API retained for TConstruct 26.1 compatibility.
public final class FluidContainerIngredient implements ICustomIngredient {
  private static final Codec<FluidContainerIngredient> CODEC = new JsonCodec<>() {
    @Override
    public FluidContainerIngredient deserialize(JsonElement element, DynamicOps<?> ops) {
      if (!element.isJsonObject()) {
        throw new JsonParseException("Expected fluid container ingredient to be an object");
      }
      JsonObject json = element.getAsJsonObject();
      FluidIngredient fluidIngredient;
      // if we have fluid and its not a primitive, then its nested
      if (json.has("fluid") && !json.get("fluid").isJsonPrimitive()) {
        fluidIngredient = FluidIngredient.LOADABLE.getIfPresent(json, "fluid");
      } else {
        fluidIngredient = FluidIngredient.LOADABLE.convert(json, "fluid");
      }
      Ingredient display = json.has("display") ? parseIngredient(json.get("display"), ops) : null;
      return new FluidContainerIngredient(fluidIngredient, display);
    }

    @Override
    public JsonElement serialize(FluidContainerIngredient ingredient, DynamicOps<?> ops) {
      JsonElement serializedFluid = ingredient.fluidIngredient.serialize();
      JsonObject json;
      if (serializedFluid.isJsonObject()) {
        json = serializedFluid.getAsJsonObject().deepCopy();
      } else {
        json = new JsonObject();
        json.add("fluid", serializedFluid);
      }
      if (ingredient.display != null) {
        json.add("display", encodeIngredient(ingredient.display, ops));
      }
      return json;
    }

    @Override
    public String codecError() {
      return "Mantle fluid container ingredient";
    }
  };

  public static final MapCodec<FluidContainerIngredient> MAP_CODEC = MapCodec.assumeMapUnsafe(CODEC);
  private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_INGREDIENT_STREAM_CODEC =
    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs::optional);
  public static final StreamCodec<RegistryFriendlyByteBuf, FluidContainerIngredient> STREAM_CODEC = StreamCodec.of(
    (buffer, ingredient) -> {
      FluidIngredient.LOADABLE.encode(buffer, ingredient.fluidIngredient);
      OPTIONAL_INGREDIENT_STREAM_CODEC.encode(buffer, Optional.ofNullable(ingredient.display));
    },
    buffer -> new FluidContainerIngredient(
      FluidIngredient.LOADABLE.decode(buffer), OPTIONAL_INGREDIENT_STREAM_CODEC.decode(buffer).orElse(null))
  );

  /** Ingredient to use for matching */
  private final FluidIngredient fluidIngredient;
  private final @Nullable Ingredient display;

  private FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
    this.fluidIngredient = fluidIngredient;
    this.display = display;
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static Ingredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
    return new FluidContainerIngredient(ingredient, display).toVanilla();
  }

  /** Creates an instance from a fluid ingredient with no display, not recommended */
  public static Ingredient fromIngredient(FluidIngredient ingredient) {
    return new FluidContainerIngredient(ingredient, null).toVanilla();
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static Ingredient fromFluid(FluidObject<?> fluid) {
    return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME), Ingredient.of(fluid));
  }

  @Override
  public boolean test(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    return FluidUtil.getFluidHandler(stack).flatMap(capability -> {
      // second, must contain enough fluid
      if (capability.getTanks() == 1) {
        FluidStack contained = capability.getFluidInTank(0);
        if (!contained.isEmpty()
          && fluidIngredient.getAmount(contained.getFluid()) == contained.getAmount()
          && fluidIngredient.test(contained.getFluid())) {
          return FluidUtil.getFluidHandler(stack.copyWithCount(1));
        }
      }
      return Optional.empty();
    }).filter(capability -> {
      // alright, we know it has the fluid, the question is just whether draining the fluid will give us the desired result
      Fluid fluid = capability.getFluidInTank(0).getFluid();
      int amount = fluidIngredient.getAmount(fluid);
      FluidStack drained = capability.drain(amount, IFluidHandler.FluidAction.EXECUTE);
      return drained.getFluid() == fluid && drained.getAmount() == amount
        && ItemStack.matches(capability.getContainer(), stack.getCraftingRemainder());
    }).isPresent();
  }

  @Override
  public Stream<Holder<Item>> items() {
    return display == null ? Stream.empty() : display.items();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredientTypes.FLUID_CONTAINER.get();
  }

  @Override
  public SlotDisplay display() {
    return display == null ? SlotDisplay.Empty.INSTANCE : display.display();
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof FluidContainerIngredient other
      && fluidIngredient.serialize().equals(other.fluidIngredient.serialize())
      && Objects.equals(display, other.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fluidIngredient.serialize(), display);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Ingredient parseIngredient(JsonElement element, DynamicOps<?> ops) {
    try {
      DynamicOps rawOps = ops;
      Object input = JsonOps.INSTANCE.convertTo(rawOps, element);
      return (Ingredient)Ingredient.CODEC.parse(rawOps, input).getOrThrow();
    } catch (RuntimeException exception) {
      if (element.isJsonObject() && element.getAsJsonObject().has("item")) {
        JsonElement item = element.getAsJsonObject().get("item");
        return parseIngredient(item, ops);
      }
      throw exception;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static JsonElement encodeIngredient(Ingredient ingredient, DynamicOps<?> ops) {
    DynamicOps rawOps = ops;
    Object encoded = Ingredient.CODEC.encodeStart(rawOps, ingredient).getOrThrow();
    return (JsonElement)rawOps.convertTo(JsonOps.INSTANCE, encoded);
  }
}
