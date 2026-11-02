package slimeknights.mantle.plugin.jei.fluid;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;

/** Distinguishes all potion components, including custom effects and custom colors. */
public record PotionSubtypeInterpreter<T extends DataComponentHolder>() implements ISubtypeInterpreter<T> {
  @Override @Nullable
  public Object getSubtypeData(T stack, UidContext context) {
    PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    return contents.equals(PotionContents.EMPTY) ? null : contents;
  }
}
