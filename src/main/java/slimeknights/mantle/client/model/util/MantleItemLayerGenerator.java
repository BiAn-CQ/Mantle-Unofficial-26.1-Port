package slimeknights.mantle.client.model.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.util.ItemLayerPixels;

import java.util.BitSet;
import java.util.EnumMap;

/**
 * Generates extruded item sprite layers using Mantle's legacy edge-merging
 * rules. Minecraft 26.1 emits a separate quad for every boundary pixel; that
 * is visually different from the established renderer when a thin side is
 * viewed at an angle, and it cannot reproduce cross-layer pixel suppression
 * without leaving seams between adjacent quads.
 */
public final class MantleItemLayerGenerator {
  private static final float MIN_Z = 7.5f;
  private static final float MAX_Z = 8.5f;
  private static final float UV_EPSILON = 0.01f;
  /** Alpha values at or below 10% are treated as transparent. */
  private static final int ALPHA_CUTOFF = 25;
  private static final Direction[] HORIZONTAL_FACES = {Direction.UP, Direction.DOWN};
  private static final Direction[] VERTICAL_FACES = {Direction.WEST, Direction.EAST};

  private MantleItemLayerGenerator() {}

  /**
   * Bakes one sprite layer and records its opaque pixels for the next lower
   * layer. Call this method from highest layer to lowest when sharing a pixel
   * map, then add the returned collections to the model in the opposite order.
   */
  public static QuadCollection bake(ModelBaker baker, Material.Baked material, ModelState modelState,
                                    int tintIndex, ExtraFaceData faceData,
                                    @Nullable ItemLayerPixels usedPixels) {
    SpriteContents sprite = material.sprite().contents();
    int width = sprite.width();
    int height = sprite.height();
    EnumMap<Direction, BitSet> faces = findSideFaces(sprite, width, height);

    Transparency transparency = material.forceTranslucent()
      ? Transparency.TRANSLUCENT : material.sprite().transparency();
    ModelBaker.Interner interner = baker.interner();
    BakedQuad.MaterialInfo materialInfo = interner.materialInfo(BakedQuad.MaterialInfo.of(
      material, transparency, tintIndex, false, faceData.lightEmission(), faceData.ambientOcclusion()));
    QuadCollection.Builder quads = new QuadCollection.Builder();

    Vector3f from = new Vector3f(0, 0, MIN_Z);
    Vector3f to = new Vector3f(16, 16, MAX_Z);
    quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to,
      new CuboidFace.UVs(16, 0, 0, 16), Quadrant.R0, materialInfo,
      Direction.NORTH, modelState, null, faceData));
    quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to,
      new CuboidFace.UVs(0, 0, 16, 16), Quadrant.R0, materialInfo,
      Direction.SOUTH, modelState, null, faceData));

    boolean translucent = sprite.transparency().hasTranslucent();
    addHorizontalFaces(quads, interner, modelState, materialInfo, faceData,
      faces, usedPixels, width, height, translucent);
    addVerticalFaces(quads, interner, modelState, materialInfo, faceData,
      faces, usedPixels, width, height, translucent);
    markOpaquePixels(sprite, usedPixels, width, height);
    return quads.build();
  }

  private static EnumMap<Direction, BitSet> findSideFaces(SpriteContents sprite, int width, int height) {
    EnumMap<Direction, BitSet> faces = new EnumMap<>(Direction.class);
    for (Direction direction : HORIZONTAL_FACES) {
      faces.put(direction, new BitSet(width * height));
    }
    for (Direction direction : VERTICAL_FACES) {
      faces.put(direction, new BitSet(width * height));
    }

    sprite.getUniqueFrames().forEach(frame -> {
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (!isTransparent(sprite, frame, x, y)) {
            setIfTransparent(faces, Direction.UP, sprite, frame, x, y, x, y - 1, width, height);
            setIfTransparent(faces, Direction.DOWN, sprite, frame, x, y, x, y + 1, width, height);
            setIfTransparent(faces, Direction.WEST, sprite, frame, x, y, x - 1, y, width, height);
            setIfTransparent(faces, Direction.EAST, sprite, frame, x, y, x + 1, y, width, height);
          }
        }
      }
    });
    return faces;
  }

  private static void setIfTransparent(EnumMap<Direction, BitSet> faces, Direction direction,
                                       SpriteContents sprite, int frame, int x, int y,
                                       int adjacentX, int adjacentY, int width, int height) {
    if (adjacentX < 0 || adjacentY < 0 || adjacentX >= width || adjacentY >= height
        || isTransparent(sprite, frame, adjacentX, adjacentY)) {
      faces.get(direction).set(y * width + x);
    }
  }

  /**
   * Checks the source frame directly because Minecraft's 26.1 helper only
   * treats fully transparent pixels as empty, while Mantle historically used
   * a 10% alpha cutoff for both side faces and cross-layer suppression.
   */
  private static boolean isTransparent(SpriteContents sprite, int frame, int x, int y) {
    NativeImage image = sprite.originalImage;
    int frameX = 0;
    int frameY = 0;
    if (sprite.isAnimated()) {
      int frameRowSize = image.getWidth() / sprite.width();
      frameX = frame % frameRowSize;
      frameY = frame / frameRowSize;
    }
    int pixel = image.getPixel(frameX * sprite.width() + x, frameY * sprite.height() + y);
    return ARGB.alpha(pixel) <= ALPHA_CUTOFF;
  }

  private static void addHorizontalFaces(QuadCollection.Builder quads, ModelBaker.Interner interner,
                                         ModelState modelState, BakedQuad.MaterialInfo materialInfo,
                                         ExtraFaceData faceData, EnumMap<Direction, BitSet> faces,
                                         @Nullable ItemLayerPixels usedPixels,
                                         int width, int height, boolean translucent) {
    for (Direction direction : HORIZONTAL_FACES) {
      BitSet facePixels = faces.get(direction);
      for (int y = 0; y < height; y++) {
        int start = 0;
        int end = width;
        boolean building = false;
        for (int x = 0; x < width; x++) {
          boolean canDraw = usedPixels == null || !usedPixels.get(x, y, width, height);
          boolean hasFace = canDraw && facePixels.get(y * width + x);
          if (hasFace) {
            end = x + 1;
            if (!building) {
              building = true;
              start = x;
            }
          } else if (building && (!canDraw || translucent)) {
            addHorizontalQuad(quads, interner, modelState, materialInfo, faceData,
              direction, start, end, y, width, height);
            building = false;
          }
        }
        if (building) {
          addHorizontalQuad(quads, interner, modelState, materialInfo, faceData,
            direction, start, end, y, width, height);
        }
      }
    }
  }

  private static void addHorizontalQuad(QuadCollection.Builder quads, ModelBaker.Interner interner,
                                        ModelState modelState, BakedQuad.MaterialInfo materialInfo,
                                        ExtraFaceData faceData, Direction direction,
                                        int start, int end, int y, int width, int height) {
    float xScale = 16f / width;
    float yScale = 16f / height;
    float modelY = direction == Direction.UP ? 16f - y * yScale : 16f - (y + 1) * yScale;
    float sampleY = direction == Direction.UP ? y + UV_EPSILON : y + 1 - UV_EPSILON;
    Vector3f from = new Vector3f(start * xScale, modelY, MIN_Z);
    Vector3f to = new Vector3f(end * xScale, modelY, MAX_Z);
    CuboidFace.UVs uvs = new CuboidFace.UVs(start * xScale, sampleY * yScale,
      end * xScale, sampleY * yScale);
    quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, uvs, Quadrant.R0,
      materialInfo, direction, modelState, null, faceData));
  }

  private static void addVerticalFaces(QuadCollection.Builder quads, ModelBaker.Interner interner,
                                       ModelState modelState, BakedQuad.MaterialInfo materialInfo,
                                       ExtraFaceData faceData, EnumMap<Direction, BitSet> faces,
                                       @Nullable ItemLayerPixels usedPixels,
                                       int width, int height, boolean translucent) {
    for (Direction direction : VERTICAL_FACES) {
      BitSet facePixels = faces.get(direction);
      for (int x = 0; x < width; x++) {
        int start = 0;
        int end = height;
        boolean building = false;
        for (int y = 0; y < height; y++) {
          boolean canDraw = usedPixels == null || !usedPixels.get(x, y, width, height);
          boolean hasFace = canDraw && facePixels.get(y * width + x);
          if (hasFace) {
            end = y + 1;
            if (!building) {
              building = true;
              start = y;
            }
          } else if (building && (!canDraw || translucent)) {
            addVerticalQuad(quads, interner, modelState, materialInfo, faceData,
              direction, x, start, end, width, height);
            building = false;
          }
        }
        if (building) {
          addVerticalQuad(quads, interner, modelState, materialInfo, faceData,
            direction, x, start, end, width, height);
        }
      }
    }
  }

  private static void addVerticalQuad(QuadCollection.Builder quads, ModelBaker.Interner interner,
                                      ModelState modelState, BakedQuad.MaterialInfo materialInfo,
                                      ExtraFaceData faceData, Direction direction,
                                      int x, int start, int end, int width, int height) {
    float xScale = 16f / width;
    float yScale = 16f / height;
    float modelX = direction == Direction.WEST ? x * xScale : (x + 1) * xScale;
    float sampleX = direction == Direction.WEST ? x + UV_EPSILON : x + 1 - UV_EPSILON;
    Vector3f from = new Vector3f(modelX, 16f - end * yScale, MIN_Z);
    Vector3f to = new Vector3f(modelX, 16f - start * yScale, MAX_Z);
    CuboidFace.UVs uvs = new CuboidFace.UVs(sampleX * xScale, start * yScale,
      sampleX * xScale, end * yScale);
    quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, uvs, Quadrant.R0,
      materialInfo, direction, modelState, null, faceData));
  }

  private static void markOpaquePixels(SpriteContents sprite, @Nullable ItemLayerPixels usedPixels,
                                       int width, int height) {
    if (usedPixels == null || sprite.getUniqueFrames().isEmpty()) {
      return;
    }
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (!isTransparent(sprite, 0, x, y)) {
          usedPixels.set(x, y, width, height);
        }
      }
    }
  }
}
