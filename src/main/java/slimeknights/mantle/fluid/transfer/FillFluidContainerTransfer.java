package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.JsonOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/** Fluid transfer info that fills a fluid into an item */
@RequiredArgsConstructor
public class FillFluidContainerTransfer implements IFluidContainerTransfer {
  public static final Identifier ID = Mantle.getResource("fill_item");

  public static final slimeknights.mantle.data.loadable.record.RecordLoadable<FillFluidContainerTransfer> LOADER = slimeknights.mantle.data.loadable.record.RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", value -> value.input),
    ItemOutput.Loadable.OPTIONAL_ITEM.emptyField("result", value -> value.result),
    slimeknights.mantle.recipe.ingredient.FluidIngredient.LOADABLE.requiredField("fluid", value -> value.fluid),
    FillFluidContainerTransfer::new);
  @Override public slimeknights.mantle.data.loadable.record.RecordLoadable<? extends IFluidContainerTransfer> getLoader() { return LOADER; }

  protected final Ingredient input;
  protected final ItemOutput result;
  protected final FluidIngredient fluid;

  public FillFluidContainerTransfer(Ingredient input, ItemOutput result, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient fluid) {
    this(input, result, FluidIngredient.of(fluid));
  }

  @Override
  public void addRepresentativeItems(Consumer<Item> consumer) {
    IFluidContainerTransfer.addIngredientItems(input, consumer);
  }

  @Override
  public boolean matches(ItemStack stack, FluidStack fluid) {
    return input.test(stack) && this.fluid.test(fluid);
  }

  /** Gets the output filled with the given fluid */
  protected ItemStack getFilled(FluidStack drained) {
    return this.result.get().copy();
  }

  @Nullable
  @Override
  public TransferResult transfer(ItemStack stack, FluidStack fluid, ResourceHandler<FluidResource> handler, TransferDirection direction) {
    if (!direction.canFill()) {
      return null;
    }
    int amount = this.fluid.getAmount(fluid);
    FluidStack toDrain = fluid.copyWithAmount(amount);
    try (Transaction transaction = Transaction.openRoot()) {
      int extracted = handler.extract(FluidResource.of(toDrain), amount, transaction);
      if (extracted == amount) {
        transaction.commit();
        return new TransferResult(getFilled(toDrain), toDrain, true);
      }
    }
    return null;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = new JsonObject();
    json.addProperty("type", ID.toString());
    json.add("input", IngredientLoadable.DISALLOW_EMPTY.serialize(input, slimeknights.mantle.data.loadable.field.ContextKey.currentContext()));
    if (!result.isEmpty()) {
      json.add("result", result.serialize(false));
    }
    json.add("fluid", fluid.serialize());
    return json;
  }

  /**
   * Unique loader instance
   */
  public static final JsonDeserializer<FillFluidContainerTransfer> DESERIALIZER = new Deserializer<>(FillFluidContainerTransfer::new);

  public record Deserializer<T extends FillFluidContainerTransfer>(TriFunction<Ingredient, ItemOutput, FluidIngredient, T> factory) implements JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject json = element.getAsJsonObject();
      Ingredient input = IngredientLoadable.DISALLOW_EMPTY.convert(JsonHelper.getElement(json, "input"), "input");
      ItemOutput result = EmptyFluidContainerTransfer.getResult(json);
      FluidIngredient fluid = FluidIngredient.LOADABLE.getIfPresent(json, "fluid");
      return factory.apply(input, result, fluid);
    }
  }
}
