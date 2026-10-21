package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Data gen for fluid transfer logic */
@SuppressWarnings("unused")
public abstract class AbstractFluidContainerTransferProvider extends GenericDataProvider {
  private final Map<Identifier,TransferJson> allTransfers = new HashMap<>();
  private final String modId;
  private final CompletableFuture<HolderLookup.Provider> registries;

  public AbstractFluidContainerTransferProvider(PackOutput packOutput, String modId) {
    this(packOutput, modId, CompletableFuture.completedFuture(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
  }

  public AbstractFluidContainerTransferProvider(PackOutput packOutput, String modId, CompletableFuture<HolderLookup.Provider> registries) {
    super(packOutput, Target.DATA_PACK, FluidContainerTransferManager.FOLDER, FluidContainerTransferManager.GSON);
    this.modId = modId;
    this.registries = registries;
  }

  /** Function to add all relevant transfers */
  protected void addTransfers() {}
  protected void addTransfers(HolderLookup.Provider lookup) { addTransfers(); }

  /** Adds a transfer to be saved */
  protected void addTransfer(Identifier id, IFluidContainerTransfer transfer, ICondition... conditions) {
    TransferJson previous = allTransfers.putIfAbsent(id, new TransferJson(transfer, conditions));
    if (previous != null) {
      throw new IllegalArgumentException("Duplicate fluid container transfer " + id);
    }
  }

  /** Adds a transfer to be saved */
  protected void addTransfer(String name, IFluidContainerTransfer transfer, ICondition... conditions) {
    addTransfer(Identifier.fromNamespaceAndPath(modId, name), transfer, conditions);
  }

  /** Adds generic fill and empty for a container */
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, FluidOutput fill, FluidIngredient drain, boolean nbt, ICondition... conditions) {
    if (nbt) {
      addTransfer(prefix + "empty", new EmptyFluidWithNBTTransfer(Ingredient.of(item), ItemOutput.fromItem(container), fill), conditions);
      addTransfer(prefix + "fill", new FillFluidWithNBTTransfer(Ingredient.of(container), ItemOutput.fromItem(item), drain), conditions);
    } else {
      addTransfer(prefix + "empty", new EmptyFluidContainerTransfer(Ingredient.of(item), ItemOutput.fromItem(container), fill), conditions);
      addTransfer(prefix + "fill", new FillFluidContainerTransfer(Ingredient.of(container), ItemOutput.fromItem(item), drain), conditions);
    }
  }

  /** Adds generic fill and empty for a container */
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, Fluid fluid, TagKey<Fluid> tag, int amount, boolean nbt, ICondition... conditions) {
    addFillEmpty(prefix, item, container, FluidOutput.fromFluid(fluid, amount), FluidIngredient.of(tag, amount), nbt, conditions);
  }

  /** Adds generic fill and empty for a container */
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, TagKey<Fluid> tag, int amount, boolean nbt, ICondition... conditions) {
    addFillEmpty(prefix, item, container, FluidOutput.fromTag(tag, amount), FluidIngredient.of(tag, amount), nbt, conditions);
  }

  /** Adds generic fill and empty for a container */
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, FluidObject<?> fluid, int amount, boolean nbt, ICondition... conditions) {
    addFillEmpty(prefix, item, container, fluid.result(amount), fluid.ingredient(amount), nbt, conditions);
  }

  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, FluidOutput fill,
      net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient drain, java.util.List<net.minecraft.core.component.DataComponentType<?>> copy, ICondition... conditions) {
    if (copy.isEmpty()) {
      addTransfer(prefix + "empty",new EmptyFluidContainerTransfer(Ingredient.of(item),ItemOutput.fromItem(container),fill),conditions);
      addTransfer(prefix + "fill",new FillFluidContainerTransfer(Ingredient.of(container),ItemOutput.fromItem(item),drain),conditions);
    } else {
      addTransfer(prefix + "empty",new EmptyFluidCopyDataTransfer(Ingredient.of(item),ItemOutput.fromItem(container),fill,copy),conditions);
      addTransfer(prefix + "fill",new FillFluidCopyDataTransfer(Ingredient.of(container),ItemOutput.fromItem(item),drain,copy),conditions);
    }
  }
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, Fluid fluid, TagKey<Fluid> tag, int amount,
      java.util.List<net.minecraft.core.component.DataComponentType<?>> copy, ICondition... conditions) {
    var nativeFluid=net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(slimeknights.mantle.recipe.ingredient.IngredientHelper.fluidSet(tag));
    addFillEmpty(prefix,item,container,FluidOutput.fromFluid(fluid,amount),new net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient(nativeFluid,amount),copy,conditions);
  }
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, TagKey<Fluid> tag, int amount,
      java.util.List<net.minecraft.core.component.DataComponentType<?>> copy, ICondition... conditions) {
    var nativeFluid=net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(slimeknights.mantle.recipe.ingredient.IngredientHelper.fluidSet(tag));
    addFillEmpty(prefix,item,container,FluidOutput.fromTag(tag,amount),new net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient(nativeFluid,amount),copy,conditions);
  }
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, FluidObject<?> fluid, int amount,
      java.util.List<net.minecraft.core.component.DataComponentType<?>> copy, ICondition... conditions) {
    addFillEmpty(prefix,item,container,fluid.result(amount),fluid.sizedIngredient(amount),copy,conditions);
  }

  /** @deprecated use {@link #addFillEmpty(String, ItemLike, ItemLike, Fluid, TagKey, int, boolean, ICondition...)} */
  @Deprecated(forRemoval = true)
  protected void addFillEmpty(String prefix, ItemLike item, ItemLike container, Fluid fluid, TagKey<Fluid> tag, int amount, ICondition... conditions) {
    addFillEmpty(prefix, item, container, fluid, tag, amount, false, conditions);
  }

  /** @deprecated use {@link #addFillEmpty(String, ItemLike, ItemLike, Fluid, TagKey, int, boolean, ICondition...)} */
  @Deprecated(forRemoval = true)
  protected void addFillEmptyNBT(String prefix, ItemLike item, ItemLike container, Fluid fluid, TagKey<Fluid> tag, int amount, ICondition... conditions) {
    addFillEmpty(prefix, item, container, fluid, tag, amount, true, conditions);
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return registries.thenCompose(lookup -> {
      try (IngredientLoadable.Scope ignored = IngredientLoadable.pushOps(lookup.createSerializationContext(JsonOps.INSTANCE))) {
        addTransfers(lookup);
        return allOf(allTransfers.entrySet().stream().map(entry -> saveJson(cache, entry.getKey(), entry.getValue().toJson())));
      }
    });
  }

  /** Json with transfer and condition */
  private record TransferJson(IFluidContainerTransfer transfer, ICondition[] conditions) {
    /** Serializes this to JSON */
    public JsonElement toJson() {
      JsonElement element = IFluidContainerTransfer.LOADER.serialize(transfer, slimeknights.mantle.data.loadable.field.ContextKey.currentContext());
      assert element.isJsonObject();
      if (conditions.length != 0) {
        JsonArray array = new JsonArray();
        for (ICondition condition : conditions) {
          array.add(JsonHelper.serializeCondition(condition));
        }
        element.getAsJsonObject().add("conditions", array);
      }
      return element;
    }
  }
}
