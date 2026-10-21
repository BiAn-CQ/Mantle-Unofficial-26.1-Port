package slimeknights.mantle.recipe.ingredient;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;

/** NeoForge 26.1 custom ingredient type registrations. */
public final class MantleIngredientTypes {
  private static final DeferredRegister<IngredientType<?>> TYPES =
    DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Mantle.modId);

  private static final com.mojang.serialization.MapCodec<net.neoforged.neoforge.common.crafting.ICustomIngredient> FLUID_CONTAINER_CODEC =
    com.mojang.serialization.MapCodec.assumeMapUnsafe(com.mojang.serialization.Codec.either(
      slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient.CODEC.codec(), FluidContainerIngredient.MAP_CODEC.codec())
      .xmap(either -> either.map(value -> value, value -> value), value ->
        value instanceof slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient modern
          ? com.mojang.datafixers.util.Either.left(modern) : com.mojang.datafixers.util.Either.right((FluidContainerIngredient)value)));
  private static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,net.neoforged.neoforge.common.crafting.ICustomIngredient> FLUID_CONTAINER_STREAM_CODEC =
    net.minecraft.network.codec.StreamCodec.of((buffer, value) -> {
      boolean modern = value instanceof slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient;
      buffer.writeBoolean(modern);
      if (modern) slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient.STREAM_CODEC.encode(buffer, (slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient)value);
      else FluidContainerIngredient.STREAM_CODEC.encode(buffer, (FluidContainerIngredient)value);
    }, buffer -> buffer.readBoolean()
      ? slimeknights.mantle.recipe.ingredient.item.FluidContainerIngredient.STREAM_CODEC.decode(buffer)
      : FluidContainerIngredient.STREAM_CODEC.decode(buffer));
  public static final DeferredHolder<IngredientType<?>, IngredientType<net.neoforged.neoforge.common.crafting.ICustomIngredient>> FLUID_CONTAINER =
    TYPES.register("fluid_container", () -> new IngredientType<>(FLUID_CONTAINER_CODEC, FLUID_CONTAINER_STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<PotionIngredient>> POTION =
    TYPES.register("potion", () -> new IngredientType<>(PotionIngredient.MAP_CODEC, PotionIngredient.STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<PotionDisplayIngredient>> POTION_DISPLAY =
    TYPES.register("potion_display", () -> new IngredientType<>(PotionDisplayIngredient.MAP_CODEC, PotionDisplayIngredient.STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<ItemTagIngredient>> ITEM_TAG =
    TYPES.register("item_tag", () -> new IngredientType<>(ItemTagIngredient.MAP_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<ItemNameIngredient>> ITEM_NAME =
    TYPES.register("item_name", () -> new IngredientType<>(ItemNameIngredient.MAP_CODEC, ItemNameIngredient.STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<OrIngredient>> OR =
    TYPES.register("or", () -> new IngredientType<>(OrIngredient.MAP_CODEC));

  private static final DeferredRegister<net.neoforged.neoforge.fluids.crafting.FluidIngredientType<?>> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_INGREDIENT_TYPES, Mantle.modId);
  public static final DeferredHolder<net.neoforged.neoforge.fluids.crafting.FluidIngredientType<?>,net.neoforged.neoforge.fluids.crafting.FluidIngredientType<slimeknights.mantle.recipe.ingredient.fluid.PotionFluidIngredient>> POTION_FLUID = FLUID_TYPES.register("potion", () -> new net.neoforged.neoforge.fluids.crafting.FluidIngredientType<>(slimeknights.mantle.recipe.ingredient.fluid.PotionFluidIngredient.CODEC, slimeknights.mantle.recipe.ingredient.fluid.PotionFluidIngredient.STREAM_CODEC));
  private MantleIngredientTypes() {}

  public static void init(IEventBus bus) {
    TYPES.register(bus);
    FLUID_TYPES.register(bus);
  }
}
