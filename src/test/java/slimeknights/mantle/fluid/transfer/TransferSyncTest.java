package slimeknights.mantle.fluid.transfer;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.NamedRecipe;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class TransferSyncTest {
  @BeforeAll static void bindComponents() {
    for (var item : List.of(Items.POTION, Items.GLASS_BOTTLE))
      if (!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    if (!Fluids.WATER.builtInRegistryHolder().areComponentsBound()) Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
  }
  @Test void packetRoundTripKeepsIdsTypesAndCopyComponentRules() {
    IFluidContainerTransfer fill = new FillFluidCopyDataTransfer(Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION),
      FluidIngredient.of(Fluids.WATER, 250), List.of(DataComponents.POTION_CONTENTS));
    IFluidContainerTransfer empty = new EmptyFluidCopyDataTransfer(Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE),
      FluidOutput.fromFluid(Fluids.WATER, 250), List.of(DataComponents.POTION_CONTENTS));
    var packet = new FluidContainerTransferPacket(List.of(NamedRecipe.of(Identifier.parse("test:fill"), fill), NamedRecipe.of(Identifier.parse("test:empty"), empty)));
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    try {
      FluidContainerTransferPacket.CODEC.encode(buffer, packet);
      var decoded = FluidContainerTransferPacket.CODEC.decode(buffer);
      assertThat(buffer.readableBytes()).isZero();
      assertThat(decoded.transfers()).extracting(NamedRecipe::id).containsExactly(Identifier.parse("test:fill"), Identifier.parse("test:empty"));
      assertThat(decoded.transfers().get(0).recipe()).isInstanceOf(FillFluidCopyDataTransfer.class);
      assertThat(decoded.transfers().get(1).recipe()).isInstanceOf(EmptyFluidCopyDataTransfer.class);
      var fluid = new FluidStack(Fluids.WATER, 250); fluid.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
      var filled = ((FillFluidCopyDataTransfer)decoded.transfers().get(0).recipe()).getFilled(fluid);
      assertThat(filled.get(DataComponents.POTION_CONTENTS)).isEqualTo(new PotionContents(Potions.HEALING));
      assertThat(((EmptyFluidCopyDataTransfer)decoded.transfers().get(1).recipe()).getFluid(filled).get(DataComponents.POTION_CONTENTS))
        .isEqualTo(new PotionContents(Potions.HEALING));
    } finally { buffer.release(); }
  }
  @Test void receivingTransfersInvalidatesTheClientContainerCache() {
    var manager = FluidContainerTransferManager.INSTANCE;
    var before = manager.getTransfers();
    try {
      manager.setTransfers(List.of()); assertThat(manager.mayHaveTransfer(Items.GLASS_BOTTLE)).isFalse();
      manager.setTransfers(List.of(NamedRecipe.of(Identifier.parse("test:fill"), new FillFluidContainerTransfer(
        Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION), FluidIngredient.of(Fluids.WATER, 250)))));
      assertThat(manager.mayHaveTransfer(Items.GLASS_BOTTLE)).isTrue();
      manager.setTransfers(List.of()); assertThat(manager.mayHaveTransfer(Items.GLASS_BOTTLE)).isFalse();
    } finally { manager.setTransfers(before); }
  }
}
