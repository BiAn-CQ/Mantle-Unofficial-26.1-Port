package slimeknights.mantle.fluid.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Single entry for text options
 */
@SuppressWarnings("ClassCanBeRecord") // needed in GSON
public class FluidUnit {

  private final String key;
  private final int needed;

  public FluidUnit(String key, int needed) {
    this.key = key;
    this.needed = needed;
  }

  /**
   * Gets the display text for this fluid entry
   * @return Display text
   */
  public int getText(List<Component> tooltip, int amount) {
    int full = amount / needed;
    if (full > 0) {
      tooltip.add(Component.translatable(key, full).withStyle(ChatFormatting.GRAY));
    }
    return amount % needed;
  }
}
