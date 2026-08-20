package slimeknights.mantle.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/** Fluid type adding an extra flipped texture for the in world block */
public class InvertedFluidType extends FluidType {
  public InvertedFluidType(Properties properties) {
    super(properties);
  }
}
