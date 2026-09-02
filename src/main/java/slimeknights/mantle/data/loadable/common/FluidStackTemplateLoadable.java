package slimeknights.mantle.data.loadable.common;

import net.neoforged.neoforge.fluids.FluidStackTemplate;
import slimeknights.mantle.data.loadable.Loadable;

/** Native NeoForge 26.1 fluid stack template loadable. */
public final class FluidStackTemplateLoadable {
  private FluidStackTemplateLoadable() {}

  public static final Loadable<FluidStackTemplate> STACK = new RegistryCodecLoadable<>(
    FluidStackTemplate.CODEC, FluidStackTemplate.STREAM_CODEC);
}
