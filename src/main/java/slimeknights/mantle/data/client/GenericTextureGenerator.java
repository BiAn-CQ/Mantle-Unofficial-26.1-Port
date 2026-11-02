package slimeknights.mantle.data.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.google.common.hash.Hashing;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.GenericDataProvider;

import java.nio.file.Path;
import java.nio.file.Files;
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
      Files.createDirectories(path.getParent());
      image.writeToFile(path);
      byte[] bytes = Files.readAllBytes(path);
      cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }
}
