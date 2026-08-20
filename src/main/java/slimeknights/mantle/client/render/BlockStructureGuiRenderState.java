package slimeknights.mantle.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Custom {@link GuiElementRenderState} for drawing already-projected block model quads into a GUI.
 *
 * <p>26.1 no longer exposes a direct block-model submission path from {@code GuiGraphicsExtractor}, but the
 * NeoForge {@code submitGuiElementRenderState} hook accepts arbitrary 2D GUI geometry. {@code StructureElement}
 * projects block quads itself and submits them through this state.
 */
public class BlockStructureGuiRenderState implements GuiElementRenderState {
  private final Matrix3x2fc pose;
  private final List<Quad> quads;
  private final TextureSetup textureSetup;
  private final ScreenRectangle bounds;

  public BlockStructureGuiRenderState(Matrix3x2fc pose, List<Quad> quads, TextureSetup textureSetup, ScreenRectangle bounds) {
    // The GUI pose stack is mutable and gets popped after extractRenderState; snapshot it for deferred rendering.
    this.pose = new Matrix3x2f(pose);
    this.quads = quads;
    this.textureSetup = textureSetup;
    this.bounds = bounds;
  }

  @Override
  public void buildVertices(VertexConsumer vertexConsumer) {
    for (Quad quad : quads) {
      for (int i = 0; i < 4; i++) {
        vertexConsumer.addVertexWith2DPose(pose, quad.x[i], quad.y[i]).setUv(quad.u[i], quad.v[i]).setColor(quad.color);
      }
    }
  }

  @Override
  public RenderPipeline pipeline() {
    return RenderPipelines.GUI_TEXTURED;
  }

  @Override
  public TextureSetup textureSetup() {
    return textureSetup;
  }

  @Override
  public @Nullable ScreenRectangle scissorArea() {
    return null;
  }

  @Override
  public @Nullable ScreenRectangle bounds() {
    return bounds;
  }

  /** One projected quad. Coordinates are in the local page space of the originating GUI. */
  public record Quad(float[] x, float[] y, float[] u, float[] v, int color) {
    public Quad {
      if (x.length != 4 || y.length != 4 || u.length != 4 || v.length != 4) {
        throw new IllegalArgumentException("Quad arrays must have exactly 4 vertices");
      }
    }
  }
}
