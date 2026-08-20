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

  public static final DeferredHolder<IngredientType<?>, IngredientType<FluidContainerIngredient>> FLUID_CONTAINER =
    TYPES.register("fluid_container", () -> new IngredientType<>(FluidContainerIngredient.MAP_CODEC, FluidContainerIngredient.STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<PotionIngredient>> POTION =
    TYPES.register("potion", () -> new IngredientType<>(PotionIngredient.MAP_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<PotionDisplayIngredient>> POTION_DISPLAY =
    TYPES.register("potion_display", () -> new IngredientType<>(PotionDisplayIngredient.MAP_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<ItemTagIngredient>> ITEM_TAG =
    TYPES.register("item_tag", () -> new IngredientType<>(ItemTagIngredient.MAP_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<ItemNameIngredient>> ITEM_NAME =
    TYPES.register("item_name", () -> new IngredientType<>(ItemNameIngredient.MAP_CODEC, ItemNameIngredient.STREAM_CODEC));
  public static final DeferredHolder<IngredientType<?>, IngredientType<OrIngredient>> OR =
    TYPES.register("or", () -> new IngredientType<>(OrIngredient.MAP_CODEC));

  private MantleIngredientTypes() {}

  public static void init(IEventBus bus) {
    TYPES.register(bus);
  }
}
