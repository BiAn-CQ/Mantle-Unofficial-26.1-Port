package slimeknights.mantle.fluid.texture;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class ClientPotionFluidType extends ClientTextureFluidType {
  public static final FluidTintSource TINT = new FluidTintSource() {
    public int color(FluidState state) { return 0xFFF800F8; }
    public int colorAsStack(FluidStack stack) {
      PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
      return contents == null ? 0xFFF800F8 : 0xFF000000 | contents.getColor();
    }
  };
  public ClientPotionFluidType(FluidType type) { super(type); }
}
