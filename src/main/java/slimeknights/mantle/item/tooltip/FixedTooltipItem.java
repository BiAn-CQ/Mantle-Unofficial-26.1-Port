package slimeknights.mantle.item.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Item with automatic mandatory tooltip support.
 * @see OptionalTooltipItem
 * @see StackTooltipItem
 * @see FixedTooltipBlockItem
 */
public class FixedTooltipItem extends Item {

  private final List<Component> tooltip;
  public FixedTooltipItem(List<Component> tooltip, Properties properties) {
    super(properties);
    this.tooltip = tooltip;
  }

  /** Makes a component for a tooltip with the proper styling */
  public static Component makeComponent(String key) {
    return Component.translatable(key).withStyle(ChatFormatting.GRAY);
  }

  /** Makes a component for a tooltip from the given ID with the proper styling */
  public static Component makeComponent(Identifier id) {
    return makeComponent(Util.makeDescriptionId("item", id) + ".tooltip");
  }

  /** Creates a new block item from the given components */
  public static FixedTooltipItem fromComponents(Properties properties, Component... tooltip) {
    return new FixedTooltipItem(List.of(tooltip), properties);
  }

  /** Creates a new item from the given translation key for the tooltip */
  public static FixedTooltipItem fromKey(String key, Properties properties) {
    return fromComponents(properties, makeComponent(key));
  }

  /** Creates a new item from the given registry ID */
  public static FixedTooltipItem fromId(Identifier id, Properties properties) {
    return fromComponents(properties, makeComponent(id));
  }

  /** Creates a new item from the given registry ID */
  public static FixedTooltipItem fromSuffixes(Properties properties, Identifier id, String... suffixes) {
    String root = Util.makeDescriptionId("item", id) + '.';
    return new FixedTooltipItem(Arrays.stream(suffixes).map(suffix -> makeComponent(root + suffix)).toList(), properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    this.tooltip.forEach(tooltip);
  }
}
