package slimeknights.mantle.fluid.texture;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.client.model.TextureColorHelper;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Objects;

public final class FluidTexture {
  private final Identifier still;
  private final Identifier flowing;
  @Nullable
  private final Identifier overlay;
  @Nullable
  private final Identifier camera;
  private final float cameraOpacity;
  private final int color;
  // fog
  private int fogColor;
  private final boolean calculateFogColor;
  private final boolean overrideFogDistance;
  private final float fogStart;
  private final float fogEnd;

  public FluidTexture(Identifier still, Identifier flowing, @Nullable Identifier overlay, @Nullable Identifier camera,
                      float cameraOpacity, int color, int fogColor, boolean calculateFogColor,
                      boolean overrideFogDistance, float fogStart, float fogEnd) {
    this.still = still;
    this.flowing = flowing;
    this.overlay = overlay;
    this.camera = camera;
    this.cameraOpacity = cameraOpacity;
    this.color = color;
    this.fogColor = fogColor;
    this.calculateFogColor = calculateFogColor;
    this.overrideFogDistance = overrideFogDistance;
    this.fogStart = fogStart;
    this.fogEnd = fogEnd;
  }

  /** @deprecated use {@link #FluidTexture(ResourceLocation, ResourceLocation, ResourceLocation, ResourceLocation, float, int, int, boolean, FogShape, float, float)} */
  @Deprecated(forRemoval = true)
  public FluidTexture(Identifier still, Identifier flowing, @Nullable Identifier overlay, @Nullable Identifier camera, int color) {
    this(still, flowing, overlay, camera, 0.1f, color, -1, false, false, 0.25f, 1);
  }

  public Identifier still() { return still; }
  public Identifier flowing() { return flowing; }
  @Nullable public Identifier overlay() { return overlay; }
  @Nullable public Identifier camera() { return camera; }
  public float cameraOpacity() { return cameraOpacity; }
  public int color() { return color; }
  public boolean overrideFogDistance() { return overrideFogDistance; }
  public float fogStart() { return fogStart; }
  public float fogEnd() { return fogEnd; }

  /** Gets the fog color for this fluid */
  public int fogColor() {
    if (calculateFogColor && fogColor == -1) {
      fogColor = TextureColorHelper.getAverageColor(still);
    }
    return fogColor;
  }

  /** Serializes this to JSON */
  public JsonObject serialize() {
    JsonObject json = new JsonObject();
    json.addProperty("still", still.toString());
    json.addProperty("flowing", flowing.toString());
    if (overlay != null) {
      json.addProperty("overlay", overlay.toString());
    }
    // during datagen, we just write the texture directly, we will include the needed prefix/suffix on read
    if (camera != null) {
      if (cameraOpacity <= 0 || cameraOpacity > 1) {
        throw new IllegalStateException("Camera opacity must be between 0 (exclusive) and 1 (inclusive)");
      }
      json.addProperty("camera", camera.toString());
      json.addProperty("camera_opacity", cameraOpacity);
    }
    json.add("color", ColorLoadable.ALPHA.serialize(color));
    JsonObject fog = new JsonObject();
    if (fogColor != -1) {
      fog.add("color", ColorLoadable.NO_ALPHA.serialize(fogColor));
    } else if (calculateFogColor) {
      fog.addProperty("calculate_color", true);
    }
    if (overrideFogDistance) {
      fog.addProperty("start", fogStart);
      fog.addProperty("end", fogEnd);
    }
    if (!fog.keySet().isEmpty()) {
      json.add("fog", fog);
    }

    return json;
  }

  /** Deserializes this from JSON */
  public static FluidTexture deserialize(JsonObject json) {
    Identifier still = JsonHelper.getIdentifier(json, "still");
    Identifier flowing = JsonHelper.getIdentifier(json, "flowing");
    Identifier overlay = JsonHelper.getIdentifier(json, "overlay", null);
    Identifier camera = null;
    float cameraOpacity = 0;
    if (json.has("camera")) {
      camera = JsonHelper.wrap(JsonHelper.getIdentifier(json, "camera"), "textures/", ".png");
      cameraOpacity = GsonHelper.getAsFloat(json, "camera_opacity");
      if (cameraOpacity <= 0 || cameraOpacity > 1) {
        throw new JsonSyntaxException("Camera opacity must be between 0 (exclusive) and 1 (inclusive)");
      }
    }
    int color = ColorLoadable.ALPHA.getOrWhite(json, "color");
    int fogColor = color | 0xFF000000; // default fog color to opaque variant of fluid color. If no tint this will end up as -1
    boolean calculateFogColor = false;
    boolean overrideFogDistance = false;
    float fogStart = 0.25f;
    float fogEnd = 1;
    if (json.has("fog")) {
      JsonObject fog = GsonHelper.getAsJsonObject(json, "fog");
      if (fog.has("color")) {
        fogColor = ColorLoadable.NO_ALPHA.getIfPresent(fog, "color");
      } else if (color == -1) {
        calculateFogColor = GsonHelper.getAsBoolean(fog, "calculate_color", false);
      }
      overrideFogDistance = fog.has("shape") || fog.has("start") || fog.has("end");
      if (overrideFogDistance) {
        fogStart = GsonHelper.getAsFloat(fog, "start", 0.25f);
        fogEnd = GsonHelper.getAsFloat(fog, "end", 1);
      }
    }
    return new FluidTexture(still, flowing, overlay, camera, cameraOpacity, color, fogColor, calculateFogColor, overrideFogDistance, fogStart, fogEnd);
  }


  /**
   * Builder for this object
   */
  @SuppressWarnings("unused") // API
  public static class Builder {

    private final FluidType fluid;
    private Identifier root;
    private Identifier still;
    private Identifier flowing;
    @Nullable
    private Identifier overlay = null;
    @Nullable
    private Identifier camera = null;
    private float cameraOpacity = 0.1f;
    private int color = -1;
    private int fogColor = -1;
    private boolean calculateFogColor = false;
    private boolean overrideFogDistance = false;
    private float fogStart = 0.25f;
    private float fogEnd = 1;

    public Builder(FluidType fluid) {
      this.fluid = fluid;
    }

    public Builder root(Identifier root) { this.root = root; return this; }
    public Builder still(Identifier still) { this.still = still; return this; }
    public Builder flowing(Identifier flowing) { this.flowing = flowing; return this; }
    public Builder overlay(@Nullable Identifier overlay) { this.overlay = overlay; return this; }
    public Builder camera(@Nullable Identifier camera) { this.camera = camera; return this; }
    public Builder cameraOpacity(float cameraOpacity) { this.cameraOpacity = cameraOpacity; return this; }
    public Builder color(int color) { this.color = color; return this; }
    public Builder fogColor(int fogColor) { this.fogColor = fogColor; return this; }
    public Builder calculateFogColor(boolean calculateFogColor) { this.calculateFogColor = calculateFogColor; return this; }
    public Builder fogStart(float fogStart) { this.fogStart = fogStart; this.overrideFogDistance = true; return this; }
    public Builder fogEnd(float fogEnd) { this.fogEnd = fogEnd; this.overrideFogDistance = true; return this; }

    /**
     * Adds textures using the fluid registry ID
     *
     * @param prefix  Prefix for where to place textures
     * @param suffix  Suffix for placing textures, included before "still" or "flowing". Typically will want "/" or "_".
     * @param overlay If true, include an overlay texture
     * @param camera  If true, include a camera texture
     * @return Builder instance
     */
    public Builder wrapId(String prefix, String suffix, boolean overlay, boolean camera) {
      return textures(JsonHelper.wrap(Objects.requireNonNull(NeoForgeRegistries.FLUID_TYPES.getKey(fluid)), prefix, suffix), overlay, camera);
    }

    /**
     * Sets the still texture from {@link #root}
     */
    public Builder still() {
      if (root == null) {
        throw new IllegalStateException("Automatic still texture requires root to be set");
      }
      return still(root.withSuffix("still"));
    }

    /**
     * Sets the flowing texture from {@link #root}
     */
    public Builder flowing() {
      if (root == null) {
        throw new IllegalStateException("Automatic flowing texture requires root to be set");
      }
      return flowing(root.withSuffix("flowing"));
    }

    /**
     * Sets the overlay texture from {@link #root}
     */
    public Builder overlay() {
      if (root == null) {
        throw new IllegalStateException("Automatic overlay texture requires root to be set");
      }
      return overlay(root.withSuffix("overlay"));
    }

    /**
     * Sets the camera texture from {@link #root}
     */
    public Builder camera() {
      if (root == null) {
        throw new IllegalStateException("Automatic camera texture requires root to be set");
      }
      return camera(root.withSuffix("camera"));
    }

    /**
     * Sets all textures by suffixing the given path
     *
     * @param path    Base path, make sure to include the trailing "_" or "/"
     * @param overlay If true, include an overlay texture
     * @param camera  If true, include a camera texture
     * @return Builder instance
     * @deprecated use {@link #root(ResourceLocation)}, {@link #still()}, {@link #flowing()}, {@link #camera()}, and {@link #overlay()}
     */
    @Deprecated
    public Builder textures(Identifier path, boolean overlay, boolean camera) {
      root(path).still().flowing();
      if (overlay) {
        overlay();
      }
      if (camera) {
        camera();
      }
      return this;
    }

    /** Sets all 3 fog properties */
    public Builder fog(float start, float end) {
      this.overrideFogDistance = true;
      this.fogStart = start;
      this.fogEnd = end;
      return this;
    }

    /**
     * Builds the fluid texture instance
     */
    public FluidTexture build() {
      if (still == null || flowing == null) {
        throw new IllegalStateException("Must set both still and flowing");
      }
      return new FluidTexture(still, flowing, overlay, camera, cameraOpacity, color, fogColor, fogColor == -1 && calculateFogColor, overrideFogDistance, fogStart, fogEnd);
    }

    /* Getters for other datagen */

    /**
     * Gets the still texture for the builder
     */
    public Identifier getStill() {
      return Objects.requireNonNull(still, "Still must be set");
    }

    /**
     * Gets the flowing texture for the builder
     */
    public Identifier getFlowing() {
      return Objects.requireNonNull(flowing, "Flowing must be set");
    }

    /**
     * Gets the camera texture for the builder
     */
    @Nullable
    public Identifier getCamera() {
      return camera;
    }

    /**
     * Gets the overlay texture for the builder
     */
    @Nullable
    public Identifier getOverlay() {
      return overlay;
    }
  }
}
