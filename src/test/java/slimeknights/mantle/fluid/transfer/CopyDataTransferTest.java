package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CopyDataTransferTest {
  @BeforeAll
  static void bindItemComponents() {
    for (var item : List.of(Items.POTION, Items.GLASS_BOTTLE)) {
      if (!item.builtInRegistryHolder().areComponentsBound()) {
        item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
      }
    }
    if (!Fluids.WATER.builtInRegistryHolder().areComponentsBound()) {
      Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  @Test
  void fillingCopiesOnlyTheSelectedComponents() {
    var transfer = new FillFluidCopyDataTransfer(Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION),
      FluidIngredient.of(Fluids.WATER, 250), List.of(DataComponents.POTION_CONTENTS));
    var fluid = new FluidStack(Fluids.WATER, 250);
    var potion = new PotionContents(Potions.HEALING);
    fluid.set(DataComponents.POTION_CONTENTS, potion);
    fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Tank name"));
    var filled = transfer.getFilled(fluid);
    assertThat(filled.get(DataComponents.POTION_CONTENTS)).isEqualTo(potion);
    assertThat(filled.get(DataComponents.CUSTOM_NAME)).isNull();
  }

  @Test
  void emptyingCopiesIntoANewFluidStackWithoutMutatingTheOutput() {
    var base = new FluidStack(Fluids.WATER, 250);
    var transfer = new EmptyFluidCopyDataTransfer(Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE),
      FluidOutput.fromStack(base), List.of(DataComponents.POTION_CONTENTS));
    var item = new ItemStack(Items.POTION);
    var potion = new PotionContents(Potions.HEALING);
    item.set(DataComponents.POTION_CONTENTS, potion);
    item.set(DataComponents.CUSTOM_NAME, Component.literal("Bottle name"));
    var fluid = transfer.getFluid(item);
    assertThat(fluid.get(DataComponents.POTION_CONTENTS)).isEqualTo(potion);
    assertThat(fluid.get(DataComponents.CUSTOM_NAME)).isNull();
    assertThat(base.get(DataComponents.POTION_CONTENTS)).isNull();
  }

  @Test
  void fillingCanConsumeTheContainerWithoutProducingAnItem() {
    var transfer = new FillFluidCopyDataTransfer(Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.EMPTY,
      FluidIngredient.of(Fluids.WATER, 250), List.of(DataComponents.POTION_CONTENTS));
    var fluid = new FluidStack(Fluids.WATER, 250);
    fluid.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
    assertThat(transfer.getFilled(fluid)).isSameAs(ItemStack.EMPTY);
    assertThat(ItemStack.EMPTY.get(DataComponents.POTION_CONTENTS)).isNull();
  }

  @Test
  void anUnavailableFluidOutputStaysEmpty() {
    var transfer = new EmptyFluidCopyDataTransfer(Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE),
      FluidOutput.EMPTY, List.of(DataComponents.POTION_CONTENTS));
    var item = new ItemStack(Items.POTION);
    item.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
    assertThat(transfer.getFluid(item)).isSameAs(FluidStack.EMPTY);
    assertThat(FluidStack.EMPTY.get(DataComponents.POTION_CONTENTS)).isNull();
  }

  @Test
  void copyComponentSyntaxAcceptsACompactIdentifier() {
    var json = JsonParser.parseString("{\"input\":\"minecraft:glass_bottle\",\"result\":{\"id\":\"minecraft:potion\"},\"fluid\":{\"fluid\":\"minecraft:water\",\"amount\":250},\"copy\":\"minecraft:potion_contents\"}");
    var transfer = FillFluidCopyDataTransfer.LOADER.convert(json, "transfer");
    var encoded = transfer.serialize(null);
    assertThat(encoded.get("type").getAsString()).isEqualTo("mantle:fill_copy_data");
    assertThat(encoded.get("copy").getAsString()).isEqualTo("minecraft:potion_contents");
  }
}
