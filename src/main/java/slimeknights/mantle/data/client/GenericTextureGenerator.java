package slimeknights.mantle.data.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.GenericDataProvider;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Data generator to create png image files */
public abstract class GenericTextureGenerator extends GenericDataProvider {
  /** Constructor which marks files as existing */
  protected GenericTextureGenerator(PackOutput packOutput, String folder) {
    super(packOutput, PackOutput.Target.RESOURCE_PACK, folder);
  }

  /** Saves the given image to the given location */
  protected CompletableFuture<?> saveImage(CachedOutput cache, Identifier location, NativeImage image) {
    Path path = this.pathProvider.file(location, "png");
    try {
      image.writeToFile(path);
      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }
}
