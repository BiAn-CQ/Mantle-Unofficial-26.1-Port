package slimeknights.mantle.client.screen.book.element;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.book.structure.StructureInfo;
import slimeknights.mantle.client.book.structure.level.TemplateLevel;
import slimeknights.mantle.client.render.BlockStructureGuiRenderState;
import slimeknights.mantle.client.screen.book.BookScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class StructureElement extends SizedBookElement {

  public boolean canTick = false;

  public float scale = 50f;
  public float transX = 0;
  public float transY = 0;
  public Transformation additionalTransform;
  public final StructureInfo renderInfo;
  public final TemplateLevel structureWorld;

  public long lastStep = -1;
  public long lastPrintedErrorTimeMs = -1;

  public StructureElement(int x, int y, int width, int height, StructureTemplate template, List<StructureTemplate.StructureBlockInfo> structure) {
    super(x, y, width, height);

    int[] size = {template.getSize().getX(), template.getSize().getY(), template.getSize().getZ()};

    this.scale = 100f / (float) IntStream.of(size).max().getAsInt();

    float sx = (float) width / (float) BookScreen.PAGE_WIDTH;
    float sy = (float) height / (float) BookScreen.PAGE_HEIGHT;

    this.scale *= Math.min(sx, sy);

    this.renderInfo = new StructureInfo(structure);

    this.structureWorld = new TemplateLevel(structure, renderInfo);

    this.transX = x + width / 2F;
    this.transY = y + height / 2F;

    this.additionalTransform = new Transformation(null, new Quaternionf().rotateYXZ(0, (float)(25 * Math.PI / 180f), 0), null, new Quaternionf().rotateYXZ((float)(-45 * Math.PI / 180f), 0, 0));
  }

  @Override
  public void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, Font fontRenderer) {
    try {
      long currentTime = System.currentTimeMillis();
      if (this.lastStep < 0) {
        this.lastStep = currentTime;
      } else if (this.canTick && currentTime - this.lastStep > 200) {
        this.renderInfo.step();
        this.lastStep = currentTime;
      }

      if (!this.canTick) {
        this.renderInfo.reset();
      }

      int structureLength = this.renderInfo.structureLength;
      int structureWidth = this.renderInfo.structureWidth;
      int structureHeight = this.renderInfo.structureHeight;

      Matrix4f transform = new Matrix4f();
      transform.translate(this.transX, this.transY, 0);
      transform.scale(this.scale, -this.scale, 1);
      transform.mul(this.additionalTransform.getMatrix());
      transform.translate(structureLength / -2f, structureHeight / -2f, structureWidth / -2f);

      BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
      List<BlockStateModelPart> parts = new ArrayList<>();
      List<BlockStructureGuiRenderState.Quad> quads = new ArrayList<>();

      for (int h = 0; h < structureHeight; h++) {
        for (int l = 0; l < structureLength; l++) {
          for (int w = 0; w < structureWidth; w++) {
            BlockPos pos = new BlockPos(l, h, w);
            BlockState state = this.structureWorld.getBlockState(pos);
            if (state.isAir()) {
              continue;
            }

            Matrix4f blockTransform = new Matrix4f(transform).translate(l, h, w);
              // TODO: verify that we should be using all types here
            BlockStateModel model = modelSet.get(state);
            parts.clear();
            model.collectParts(this.structureWorld, pos, state, RandomSource.create(state.getSeed(pos)), parts);
            for (BlockStateModelPart part : parts) {
              for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                  quads.add(project(quad, blockTransform));
                }
              }
              for (BakedQuad quad : part.getQuads(null)) {
                quads.add(project(quad, blockTransform));
              }
            }
          }
        }
      }

      if (!quads.isEmpty()) {
        AbstractTexture blockAtlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        TextureSetup textureSetup = TextureSetup.singleTexture(blockAtlas.getTextureView(), blockAtlas.getSampler());
        graphics.submitGuiElementRenderState(new BlockStructureGuiRenderState(
          graphics.pose(), quads, textureSetup, new ScreenRectangle(this.x, this.y, this.width, this.height)));
      }
    } catch (Exception e) {
      long now = System.currentTimeMillis();
      if (now > this.lastPrintedErrorTimeMs + 1000) {
        e.printStackTrace();
        this.lastPrintedErrorTimeMs = now;
      }
    }
  }

  private static BlockStructureGuiRenderState.Quad project(BakedQuad quad, Matrix4f transform) {
    float[] x = new float[4];
    float[] y = new float[4];
    float[] u = new float[4];
    float[] v = new float[4];
    for (int vertex = 0; vertex < 4; vertex++) {
      Vector3f pos = transform.transformPosition(quad.position(vertex), new Vector3f());
      x[vertex] = pos.x();
      y[vertex] = pos.y();
      u[vertex] = UVPair.unpackU(quad.packedUV(vertex));
      v[vertex] = UVPair.unpackV(quad.packedUV(vertex));
    }

    float shade = switch (quad.direction()) {
      case UP -> 1.0f;
      case DOWN -> 0.5f;
      case NORTH, SOUTH -> 0.8f;
      case EAST, WEST -> 0.6f;
    };
    int channel = (int)(255 * shade);
    return new BlockStructureGuiRenderState.Quad(x, y, u, v, ARGB.color(255, channel, channel, channel));
  }

  @Override
  public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void mouseDragged(double clickX, double clickY, double mouseX, double mouseY, double lastX, double lastY, int button) {
    double dx = mouseX - lastX;
    double dy = mouseY - lastY;
    this.additionalTransform = forRotation(dx * 80D / 104, dy * 0.8).compose(this.additionalTransform);
  }

  @Override
  public void mouseReleased(double mouseX, double mouseY, int clickedMouseButton) {
    super.mouseReleased(mouseX, mouseY, clickedMouseButton);
  }

  private Transformation forRotation(double rX, double rY) {
    Vector3f axis = new Vector3f((float) rY, (float) rX, 0);
    float dot = axis.dot(axis);
    if (dot < Float.MIN_NORMAL) {
      return Transformation.IDENTITY;
    }

    float angle = (float) (Math.sqrt(axis.dot(axis)) * Math.PI / 180f);
    axis.normalize();
    return new Transformation(null, new Quaternionf(new AxisAngle4f(angle, axis)), null, null);
  }
}
