package slimeknights.mantle.fluid.texture;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;

/** Client logic for {@link slimeknights.mantle.fluid.InvertedFluidType} */
public class ClientInvertedFluidType extends ClientTextureFluidType {
  private Identifier lastFlowing;
  private Identifier invertedFlowing;
  public ClientInvertedFluidType(FluidType type) {
    super(type);
  }

  @Override
  public Identifier getFlowingTexture() {
    Identifier flowing = super.getFlowingTexture();
    if (flowing.equals(lastFlowing)) {
      return invertedFlowing;
    }
    invertedFlowing = flowing.withSuffix("_inverted");
    lastFlowing = flowing;
    return invertedFlowing;
  }
}
