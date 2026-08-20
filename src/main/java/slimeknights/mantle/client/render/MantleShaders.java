package slimeknights.mantle.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import slimeknights.mantle.Mantle;

/** Handles any custom shaders registered by Mantle. */
@EventBusSubscriber(modid = Mantle.modId, value = Dist.CLIENT)
public final class MantleShaders {
  private MantleShaders() {}

  /** Registers the custom render pipelines used by Mantle. */
  @SubscribeEvent
  static void registerPipelines(RegisterRenderPipelinesEvent event) {
    event.registerPipeline(MantleRenderTypes.TRANSLUCENT_FULLBRIGHT_PIPELINE);
  }
}
