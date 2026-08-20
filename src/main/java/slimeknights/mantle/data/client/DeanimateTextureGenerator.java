package slimeknights.mantle.data.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Copies the first frame of the passed texture into its own texture */
public class DeanimateTextureGenerator extends GenericTextureGenerator {
  private final Map<Identifier, Identifier> deanimate = new HashMap<>();
  private final ResourceManager resourceManager;

  public DeanimateTextureGenerator(PackOutput packOutput, ResourceManager resourceManager, String folder) {
    super(packOutput, folder);
    this.resourceManager = resourceManager;
  }

  public DeanimateTextureGenerator(PackOutput packOutput, ResourceManager resourceManager) {
    this(packOutput, resourceManager, "textures");
  }

  @Deprecated
  public DeanimateTextureGenerator(PackOutput packOutput) {
    super(packOutput, "textures");
    this.resourceManager = null;
  }

  /** Requests the given texture to be deanimated */
  protected void deanimate(Identifier source, Identifier destination) {
    Identifier existing = deanimate.putIfAbsent(destination, source);
    if (existing != null && !existing.equals(source)) {
      throw new IllegalArgumentException("Multiple textures are deanimating with the same destination: original - " + existing + ", new - " + source);
    }
  }

  /** Method to override when using directly */
  protected void addTextures() {}

  @Override
  public final CompletableFuture<?> run(CachedOutput cache) {
    addTextures();
    if (deanimate.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    if (resourceManager == null) {
      return CompletableFuture.failedFuture(new IllegalStateException("DeanimateTextureGenerator requires a ResourceManager"));
    }

    List<NativeImage> openedImages = new ArrayList<>();
    try {
      return allOf(deanimate.entrySet().stream().map(entry -> {
        try (InputStream input = resourceManager.getResource(entry.getValue()).orElseThrow().open();
             NativeImage image = NativeImage.read(input)) {
        // use the width to guess the height
          NativeImage copy = new NativeImage(image.getWidth(), image.getWidth(), true);
          copy.copyFrom(image);
        // can't close the image until it saved, which is a completable future
          openedImages.add(copy);
          return saveImage(cache, entry.getKey(), copy);
        } catch (IOException e) {
          return CompletableFuture.failedFuture(e);
        }
      })).whenComplete((result, throwable) -> openedImages.forEach(NativeImage::close));
    } finally {
      if (deanimate.isEmpty()) {
        openedImages.forEach(NativeImage::close);
      }
    }
  }

  @Override
  public String getName() {
    return "Texture Deanimator";
  }
}
