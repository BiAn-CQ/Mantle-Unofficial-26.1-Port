package slimeknights.mantle.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type whose color and textures are determined by the model.
 * Just implements {@link ClientTextureFluidType} in initializeClient as the Forge API is dumb and does not let me do that in a client place.
 */
public class TextureFluidType extends FluidType {
  public TextureFluidType(Properties properties) {
    super(properties);
  }

}
