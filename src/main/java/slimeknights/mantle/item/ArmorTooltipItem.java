package slimeknights.mantle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import slimeknights.mantle.util.TranslationHelper;

import java.util.function.Consumer;

public class ArmorTooltipItem extends Item {
  public ArmorTooltipItem(Properties builder) {
    super(builder);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flagIn) {
    TranslationHelper.addOptionalTooltip(stack, tooltip);
    super.appendHoverText(stack, context, tooltipDisplay, tooltip, flagIn);
  }
}
