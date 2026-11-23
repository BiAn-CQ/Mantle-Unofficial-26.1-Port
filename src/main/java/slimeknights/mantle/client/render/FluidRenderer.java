package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import slimeknights.mantle.client.render.FluidCuboid.FluidFace;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FluidRenderer {
  /**
   * Gets a block sprite from the given location
   * @param sprite  Sprite name
   * @return  Sprite location
   */
  public static TextureAtlasSprite getBlockSprite(Identifier sprite) {
    return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, sprite));
  }

  /**
   * Takes the larger light value between combinedLight and the passed block light
   * @param combinedLight  Sky light/block light lightmap value
   * @param blockLight     New 0-15 block light value
   * @return  Updated packed light including the new light value
   */
  public static int withBlockLight(int combinedLight, int blockLight) {
    // skylight from the combined plus larger block light between combined and parameter
    // not using methods from LightTexture to reduce number of operations
    return (combinedLight & 0xFFFF0000) | Math.max(blockLight << 4, combinedLight & 0xFFFF);
  }

  /** Selects native entity pipelines, including lightmap, overlay and normals. */
  public static net.minecraft.client.renderer.rendertype.RenderType getRenderType(boolean transparent, boolean cull) {
    if (transparent) return cull ? RenderTypes.entityTranslucentCullItemTarget(TextureAtlas.LOCATION_BLOCKS) : MantleRenderTypes.ENTITY_TRANSLUCENT_NO_CULL;
    return cull ? RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS) : MantleRenderTypes.ENTITY_CUTOUT_NO_CULL;
  }
  public static net.minecraft.client.renderer.rendertype.RenderType getRenderType(net.minecraft.world.level.material.Fluid fluid, boolean cull) {
    var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    return getRenderType(model.layer() == net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT, cull);
  }
  public static net.minecraft.client.renderer.rendertype.RenderType getRenderType(net.minecraft.world.level.material.Fluid fluid) { return getRenderType(fluid, true); }

  /* Fluid cuboids */

  /**
   * Forces the UV to be between 0 and 1
   * @param value  Original value
   * @param upper  If true, this is the larger UV. Needed to enforce integer values end up at 1
   * @return  UV mapped between 0 and 1
   */
  private static float boundUV(float value, boolean upper) {
    value -= (int)value;
    if (value == 0) {
      // if it lands exactly on the 0 bound, map that to 1 instead for the larger UV
      return upper ? 1 : 0;
    }
    // modulo returns a negative result if the input is negative, so add 1 to account for that
    return value < 0 ? (value + 1) : value;
  }

  /**
   * Adds a quad to the renderer
   * @param renderer    Renderer instnace
   * @param matrix      Render matrix
   * @param sprite      Sprite to render
   * @param from        Quad start
   * @param to          Quad end
   * @param face        Face to render
   * @param color       Color to use in rendering
   * @param brightness  Face brightness
   * @param flowing     If true, half texture coordinates
   */
  public static void putTexturedQuad(VertexConsumer renderer, Matrix4f matrix, TextureAtlasSprite sprite, Vector3f from, Vector3f to, Direction face, int color, int brightness, int rotation, boolean flowing) {
    // start with texture coordinates
    float x1 = from.x(), y1 = from.y(), z1 = from.z();
    float x2 = to.x(), y2 = to.y(), z2 = to.z();
    // choose UV based on the directions, some need to negate UV due to the direction
    // note that we use -UV instead of 1-UV as its slightly simpler and the later logic deals with negatives
    float u1, u2, v1, v2;
    switch (face) {
      default -> { // DOWN
        u1 = x1; u2 = x2;
        v1 = z2; v2 = z1;
      }
      case UP -> {
        u1 = x1; u2 = x2;
        v1 = -z1; v2 = -z2;
      }
      case NORTH -> {
        u1 = -x1; u2 = -x2;
        v1 = y1; v2 = y2;
      }
      case SOUTH -> {
        u1 = x2; u2 = x1;
        v1 = y1; v2 = y2;
      }
      case WEST -> {
        u1 = z2; u2 = z1;
        v1 = y1; v2 = y2;
      }
      case EAST -> {
        u1 = -z1; u2 = -z2;
        v1 = y1; v2 = y2;
      }
    }

    // flip V when relevant
    if (rotation == 0 || rotation == 270) {
      float temp = v1;
      v1 = -v2;
      v2 = -temp;
    }
    // flip U when relevant
    if (rotation >= 180) {
      float temp = u1;
      u1 = -u2;
      u2 = -temp;
    }

    // bound UV to be between 0 and 1
    boolean reverse = u1 > u2;
    u1 = boundUV(u1, reverse);
    u2 = boundUV(u2, !reverse);
    reverse = v1 > v2;
    v1 = boundUV(v1, reverse);
    v2 = boundUV(v2, !reverse);

    // if rotating by 90 or 270, swap U and V
    float minU, maxU, minV, maxV;
    // TextureAtlasSprite#getU/getV used to accept coordinates in the 0-16
    // sprite-pixel range. In 26.1 they accept normalized 0-1 offsets instead.
    // Flowing faces historically sampled half of the sprite (8/16), while
    // still faces sampled the full sprite (16/16).
    float textureScale = flowing ? 0.5f : 1f;
    if (rotation == 90 || rotation == 270) {
      minU = sprite.getU(v1 * textureScale);
      maxU = sprite.getU(v2 * textureScale);
      minV = sprite.getV(u1 * textureScale);
      maxV = sprite.getV(u2 * textureScale);
    } else {
      minU = sprite.getU(u1 * textureScale);
      maxU = sprite.getU(u2 * textureScale);
      minV = sprite.getV(v1 * textureScale);
      maxV = sprite.getV(v2 * textureScale);
    }
    // based on rotation, put coords into place
    float u3, u4, v3, v4;
    switch(rotation) {
      default -> { // 0
        u1 = minU; v1 = maxV;
        u2 = minU; v2 = minV;
        u3 = maxU; v3 = minV;
        u4 = maxU; v4 = maxV;
      }
      case 90 -> {
        u1 = minU; v1 = minV;
        u2 = maxU; v2 = minV;
        u3 = maxU; v3 = maxV;
        u4 = minU; v4 = maxV;
      }
      case 180 -> {
        u1 = maxU; v1 = minV;
        u2 = maxU; v2 = maxV;
        u3 = minU; v3 = maxV;
        u4 = minU; v4 = minV;
      }
      case 270 -> {
        u1 = maxU; v1 = maxV;
        u2 = minU; v2 = maxV;
        u3 = minU; v3 = minV;
        u4 = maxU; v4 = minV;
      }
    }
    // Consumers may require normals even when the base block format does not.
    // Use the inverse transpose so lighting follows rotated and scaled geometry.
    Vector3f normal = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
    matrix.normal(new Matrix3f()).transform(normal).normalize();
    float normalX = normal.x(), normalY = normal.y(), normalZ = normal.z();
    // add quads
    int light1 = brightness & 0xFFFF;
    int light2 = brightness >> 0x10 & 0xFFFF;
    int a = color >> 24 & 0xFF;
    int r = color >> 16 & 0xFF;
    int g = color >> 8 & 0xFF;
    int b = color & 0xFF;
    switch (face) {
      case DOWN -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
      case UP -> {
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
      case NORTH -> {
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
      case SOUTH -> {
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
      case WEST -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
      case EAST -> {
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(normalX, normalY, normalZ);
      }
    }
  }

  /** Renders a full cuboid with explicit bounds. */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light) {
    renderCuboid(matrices.last(), buffer, cube, still, flowing, from, to, color, light);
  }

  /** Renders a full cuboid with its model bounds. */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light);
  }

  /** @deprecated gas now uses the face rotation defined by the model. */
  @Deprecated(forRemoval = true)
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    renderCuboid(matrices, buffer, cube, still, flowing, from, to, color, light);
  }

  /** @deprecated gas now uses the face rotation defined by the model. */
  @Deprecated(forRemoval = true)
  public static void renderCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    renderCuboid(pose, buffer, cube, still, flowing, from, to, color, light);
  }

  /** Renders a cuboid from a deferred geometry submission. */
  public static void renderCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light) {
    Matrix4f matrix = pose.pose();
    for (Direction direction : Direction.values()) {
      FluidFace face = cube.getFace(direction);
      if (face != null) {
        boolean isFlowing = face.isFlowing();
        putTexturedQuad(buffer, matrix, isFlowing ? flowing : still, from, to, direction, color, light, face.rotation(), isFlowing);
      }
    }
  }

  /**
   * Renders a list of fluid cuboids
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer instance
   * @param cubes     List of cubes to render
   * @param fluid     Fluid to use in rendering
   * @param light     Light level from TER
   */
  public static void renderCuboids(PoseStack matrices, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light) {
    renderCuboids(matrices.last(), buffer, cubes, fluid, light);
  }

  /** Renders fluid cuboids from a 26.1 submit callback pose. */
  public static void renderCuboids(PoseStack.Pose pose, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light) {
    renderCuboids(pose, buffer, cubes, fluid, light, 0xFF);
  }

  /** @deprecated use the overload without the gas flag. */
  @Deprecated(forRemoval = true)
  public static void renderCuboids(PoseStack.Pose pose, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light, boolean isGas) {
    renderCuboids(pose, buffer, cubes, fluid, light);
  }

  /** Renders cuboids with an additional 0-255 opacity multiplier. */
  public static void renderCuboids(PoseStack.Pose pose, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light, int opacity) {
    if (fluid.isEmpty()) {
      return;
    }

    net.minecraft.client.renderer.block.FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    TextureAtlasSprite still = fluidModel.stillMaterial().sprite();
    TextureAtlasSprite flowing = fluidModel.flowingMaterial().sprite();
    int color = fluidModel.fluidTintSource() == null ? -1 : fluidModel.fluidTintSource().colorAsStack(fluid);
    if (opacity < 0xFF) {
      color = ARGB.multiplyAlpha(color, Math.max(0, opacity) / 255f);
    }
    FluidType type = fluid.getFluid().getFluidType();
    light = withBlockLight(light, type.getLightLevel(fluid));

    // render all given cuboids
    for (FluidCuboid cube : cubes) {
      renderCuboid(pose, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light);
    }
  }

  /** @deprecated use the overload without a gas flag and translate the pose stack. */
  @Deprecated(forRemoval = true)
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float yOffset, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    if (yOffset == 0) {
      renderCuboid(matrices, buffer, cube, still, flowing, color, light);
      return;
    }
    matrices.pushPose();
    matrices.translate(0, yOffset, 0);
    renderCuboid(matrices, buffer, cube, still, flowing, color, light);
    matrices.popPose();
  }

  /** A group of model cuboids at the same distance below a faucet. */
  public record FaucetLayer(int offset, List<FluidCuboid> cuboids) {}

  /** Collects the fluid geometry below a faucet before it is submitted for rendering. */
  public static List<FaucetLayer> getFaucetLayers(LevelAccessor level, BlockPos pos, Direction direction) {
    List<FaucetLayer> layers = new ArrayList<>();
    int offset = 0;
    FaucetFluid data;
    do {
      data = FaucetFluid.REGISTRY.get(level.getBlockState(pos.below(++offset)));
      List<FluidCuboid> cuboids = data.getFluids(direction);
      if (!cuboids.isEmpty()) {
        layers.add(new FaucetLayer(offset, List.copyOf(cuboids)));
      }
    } while (data.isContinued());
    return List.copyOf(layers);
  }

  /** Renders the stream below a faucet for callers using a vertex consumer directly. */
  public static void renderFaucetFluids(LevelAccessor world, BlockPos pos, Direction direction, PoseStack matrices, VertexConsumer buffer, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    for (FaucetLayer layer : getFaucetLayers(world, pos, direction)) {
      matrices.pushPose();
      matrices.translate(0, -layer.offset(), 0);
      for (FluidCuboid cube : layer.cuboids()) {
        renderCuboid(matrices, buffer, cube, still, flowing, color, light);
      }
      matrices.popPose();
    }
  }

  /**
   * Renders a fluid cuboid with partial height based on the capacity
   * @param matrices  Matrix stack instance
   * @param buffer    Render type buffer instance
   * @param fluid     Fluid to render
   * @param offset    Fluid amount offset, used to animate transitions
   * @param capacity  Fluid tank capacity, must be above 0
   * @param light     Quad lighting from TER
   * @param cube      Fluid cuboid instance
   * @param flipGas   If true, flips gas cubes
   */
  public static void renderScaledCuboid(PoseStack matrices, MultiBufferSource buffer, FluidCuboid cube, FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    renderScaledCuboid(matrices.last(), buffer.getBuffer(getRenderType(fluid.getFluid())), cube, fluid, offset, capacity, light, flipGas);
  }

  /**
   * Renders a partially filled fluid cuboid from a 26.1 submit callback.
   *
   * <p>The callback supplies an immutable pose and a render-type-specific
   * vertex consumer, so this overload deliberately contains no buffer lookup
   * or mutable pose-stack operations.</p>
   */
  public static void renderScaledCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    // nothing to render
    if (fluid.isEmpty() || capacity <= 0) {
      return;
    }

    net.minecraft.client.renderer.block.FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    TextureAtlasSprite still = fluidModel.stillMaterial().sprite();
    TextureAtlasSprite flowing = fluidModel.flowingMaterial().sprite();
    FluidType type = fluid.getFluid().getFluidType();
    int color = fluidModel.fluidTintSource() == null ? -1 : fluidModel.fluidTintSource().colorAsStack(fluid);
    light = withBlockLight(light, type.getLightLevel(fluid));

    float minY = cube.getFromScaled().y();
    float maxY = cube.getToScaled().y();
    float height = (fluid.getAmount() - offset) / capacity;
    boolean flip = flipGas && type.isLighterThanAir();
    float scaledY = flip ? maxY + height * (minY - maxY) : minY + height * (maxY - minY);
    renderScaledCuboid(pose, buffer, cube, scaledY, flip, still, flowing, color, light);
  }

  /** Clips a cuboid against one shared liquid surface instead of scaling each cube. */
  public static void renderScaledCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, float scaledY, boolean flip,
      TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    Vector3f from = cube.getFromScaled();
    Vector3f to = cube.getToScaled();
    if (flip) {
      if (scaledY >= to.y()) return;
      if (scaledY > from.y()) from = new Vector3f(from.x(), scaledY, from.z());
    } else {
      if (scaledY <= from.y()) return;
      if (scaledY < to.y()) to = new Vector3f(to.x(), scaledY, to.z());
    }
    renderCuboid(pose, buffer, cube, still, flowing, from, to, color, light);
  }

  public static void renderScaledCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float scaledY, boolean flip,
      TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    renderScaledCuboid(matrices.last(), buffer, cube, scaledY, flip, still, flowing, color, light);
  }

  /** @deprecated use the overload without the gas flag. */
  @Deprecated(forRemoval = true)
  public static void renderScaledCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float scaledY, boolean flip,
      TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    renderScaledCuboid(matrices, buffer, cube, scaledY, flip, still, flowing, color, light);
  }

  /** @deprecated use the overload without the gas flag. */
  @Deprecated(forRemoval = true)
  public static void renderScaledCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, float scaledY, boolean flip,
      TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    renderScaledCuboid(pose, buffer, cube, scaledY, flip, still, flowing, color, light);
  }

  /** Renders every cube against the same height across the full model bounds. */
  public static void renderScaledModel(PoseStack matrices, MultiBufferSource buffer, boolean cull, FluidModel model,
      FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    if (fluid.isEmpty() || capacity <= 0 || model.isEmpty()) return;
    renderScaledModel(matrices.last(), buffer.getBuffer(getRenderType(fluid.getFluid(), cull)), model, fluid, offset, capacity, light, flipGas);
  }

  /** Submit-renderer overload without a mutable pose stack or buffer lookup. */
  public static void renderScaledModel(PoseStack.Pose pose, VertexConsumer buffer, FluidModel model,
      FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    if (fluid.isEmpty() || capacity <= 0 || model.isEmpty()) return;
    var baked = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    TextureAtlasSprite still = baked.stillMaterial().sprite();
    TextureAtlasSprite flowing = baked.flowingMaterial().sprite();
    FluidType type = fluid.getFluid().getFluidType();
    int color = baked.fluidTintSource() == null ? -1 : baked.fluidTintSource().colorAsStack(fluid);
    light = withBlockLight(light, type.getLightLevel(fluid));
    float minY = model.getMinScaled().y();
    float maxY = model.getMaxScaled().y();
    float height = (fluid.getAmount() - offset) / capacity;
    boolean flip = flipGas && type.isLighterThanAir();
    float scaledY = flip ? maxY + height * (minY - maxY) : minY + height * (maxY - minY);
    for (FluidCuboid cube : model.getFluids()) renderScaledCuboid(pose, buffer, cube, scaledY, flip, still, flowing, color, light);
  }

  /** Same as {@link net.minecraft.client.renderer.ScreenEffectRenderer#renderFluid(Minecraft, PoseStack, ResourceLocation)} but with opacity and color control */
  public static void renderCamera(Minecraft minecraft, PoseStack poseStack, MultiBufferSource buffers, Identifier texture, float opacity, int color) {
    assert minecraft.player != null;
    BlockPos pos = BlockPos.containing(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ());
    Level level = minecraft.player.level();
    float brightness = Lightmap.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(pos));
    int baseColor = color == -1 ? -1 : color;
    int drawColor = ARGB.multiplyAlpha(ARGB.scaleRGB(baseColor, brightness), opacity);
    float yRot = -minecraft.player.getYRot() / 64;
    float xRot = minecraft.player.getXRot() / 64;
    Matrix4f matrix = poseStack.last().pose();
    VertexConsumer buffer = buffers.getBuffer(RenderTypes.blockScreenEffect(texture));
    buffer.addVertex(matrix, -1, -1, -0.5f).setUv(4 + yRot, 4 + xRot).setColor(drawColor);
    buffer.addVertex(matrix,  1, -1, -0.5f).setUv(0 + yRot, 4 + xRot).setColor(drawColor);
    buffer.addVertex(matrix,  1,  1, -0.5f).setUv(0 + yRot, 0 + xRot).setColor(drawColor);
    buffer.addVertex(matrix, -1,  1, -0.5f).setUv(4 + yRot, 0 + xRot).setColor(drawColor);
  }
}
