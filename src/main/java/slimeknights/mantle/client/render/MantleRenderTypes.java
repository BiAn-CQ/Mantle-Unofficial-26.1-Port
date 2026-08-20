package slimeknights.mantle.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.texture.TextureAtlas;
import slimeknights.mantle.Mantle;

/**
 * Class for render types defined by Mantle
 */
public final class MantleRenderTypes {
  private MantleRenderTypes() {}

  /**
   * Render type used for the fluid renderer.
   * TODO 1.21: can we replace this with {@link RenderType#ENTITY_TRANSLUCENT_CULL}? Would require including normals in our vertex format.
   */
  public static final RenderType FLUID = RenderType.create(
    Mantle.modId + ":fluid",
    blockAtlasSetup(RenderPipelines.TRANSLUCENT_BLOCK, true, false));

  /** Render type used for the structure renderer. */
  public static final RenderPipeline TRANSLUCENT_FULLBRIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
    .withLocation(Mantle.getResource("pipeline/translucent_fullbright"))
    .withVertexShader(Mantle.getResource("core/block_fullbright"))
    .withShaderDefine("ALPHA_CUTOUT", 0.01f)
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .build();

  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderType.create(
    Mantle.modId + ":translucent_fullbright",
    blockAtlasSetup(TRANSLUCENT_FULLBRIGHT_PIPELINE, true, true));

  private static RenderSetup blockAtlasSetup(RenderPipeline pipeline, boolean sort, boolean affectsCrumbling) {
    RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
      .withTexture(
        "Sampler0",
        TextureAtlas.LOCATION_BLOCKS,
        () -> RenderSystem.getSamplerCache().getSampler(
          AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
          FilterMode.LINEAR, FilterMode.NEAREST, true))
      // BLOCK_SNIPPET declares Sampler2 and reads the packed UV2 coordinates
      // emitted by FluidRenderer. Without the lightmap binding the geometry is
      // submitted, but the 26.1 block shader cannot produce a visible result.
      .useLightmap()
      .bufferSize(256);
    if (sort) {
      builder.sortOnUpload();
      // Match vanilla's translucent moving-block setup. Deferred block entity
      // geometry is composited through this target instead of the main target.
      builder.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET);
    }
    if (affectsCrumbling) {
      builder.affectsCrumbling();
      builder.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
    }
    return builder.createRenderSetup();
  }
}
