package slimeknights.mantle.command.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSignRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiSignRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.apache.commons.lang3.text.WordUtils;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.command.GeneratePackHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Exports one page at a time using the 26.1 GUI renderer and asynchronous GPU readback. */
final class BookExporter {
  private static BookExporter active;
  private final Minecraft minecraft = Minecraft.getInstance();
  private final List<Identifier> books;
  private final int scale;
  private final boolean html;
  private final String key;
  private final boolean domain;
  private final GuiRenderState renderState = new GuiRenderState();
  private GuiRenderer renderer;
  private GlobalSettingsUniform uniforms;
  private BookScreen screen;
  private int bookIndex;
  private Path screenshotDir;
  private Path htmlDir;
  private String bookKey;
  private String title;
  private String modName;

  private BookExporter(List<Identifier> books, int scale, boolean html, String key, boolean domain) {
    this.books = List.copyOf(books);
    this.scale = scale;
    this.html = html;
    this.key = key;
    this.domain = domain;
  }

  static void export(List<Identifier> books, int scale, boolean html, String key, boolean domain) {
    if (active != null) {
      Minecraft.getInstance().player.sendSystemMessage(Component.translatable("command.mantle.book.export.busy"));
      return;
    }
    active = new BookExporter(books, scale, html, key, domain);
    Minecraft.getInstance().schedule(active::start);
  }

  private void start() {
    try {
      uniforms = new GlobalSettingsUniform();
      renderer = new GuiRenderer(renderState, minecraft.renderBuffers().bufferSource(),
        minecraft.gameRenderer.getSubmitNodeStorage(), minecraft.gameRenderer.getFeatureRenderDispatcher(),
        ClientHooks.gatherPictureInPictureRenderers(List.of(
          new PictureInPictureRendererRegistration<>(GuiEntityRenderState.class, buffers -> new GuiEntityRenderer(buffers, minecraft.getEntityRenderDispatcher())),
          new PictureInPictureRendererRegistration<>(GuiSkinRenderState.class, GuiSkinRenderer::new),
          new PictureInPictureRendererRegistration<>(GuiBookModelRenderState.class, GuiBookModelRenderer::new),
          new PictureInPictureRendererRegistration<>(GuiBannerResultRenderState.class, buffers -> new GuiBannerResultRenderer(buffers, minecraft.getAtlasManager())),
          new PictureInPictureRendererRegistration<>(GuiSignRenderState.class, buffers -> new GuiSignRenderer(buffers, minecraft.getAtlasManager())),
          new PictureInPictureRendererRegistration<>(GuiProfilerChartRenderState.class, GuiProfilerChartRenderer::new)
        )));
      beginBook();
    } catch (Exception exception) {
      fail(exception);
    }
  }

  private void beginBook() throws IOException {
    Identifier id = books.get(bookIndex);
    BookData data = Objects.requireNonNull(BookLoader.getBook(id), "Book no longer registered: " + id);
    data.load();
    screenshotDir = minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("mantle_book")
      .resolve(id.getNamespace()).resolve(id.getPath());
    htmlDir = minecraft.gameDirectory.toPath().resolve("mantle_book").resolve(id.getNamespace()).resolve(id.getPath().replace('_', '-'));
    Files.createDirectories(screenshotDir);
    if (html) {
      Files.createDirectories(htmlDir);
    }
    bookKey = BookCommand.bookKey(id, key, domain);
    title = data.appearance.exportTitle;
    if (title.isEmpty()) {
      title = data.appearance.title;
      if (title.isEmpty()) {
        title = WordUtils.capitalize(id.getPath().replace('_', ' '));
      }
    }
    modName = ModList.get().getModContainerById(id.getNamespace())
      .map(container -> container.getModInfo().getDisplayName()).orElse(id.getNamespace());
    screen = new BookScreen(Component.literal("Book"), data, "", null, null);
    screen.drawArrows = false;
    screen.mouseInput = false;
    screen.enableAnimations = false;
    screen.init(BookScreen.PAGE_WIDTH_UNSCALED * 2, BookScreen.PAGE_HEIGHT_UNSCALED);
    screen.openCover();
    renderPage();
  }

  private void renderPage() {
    if (minecraft.player == null || minecraft.level == null) {
      finish();
      return;
    }
    RenderTarget target = minecraft.getMainRenderTarget();
    WindowRenderState window = minecraft.gameRenderer.getGameRenderState().windowRenderState;
    int originalWidth = target.width;
    int originalHeight = target.height;
    int originalWindowWidth = window.width;
    int originalWindowHeight = window.height;
    int originalScale = window.guiScale;
    int width = BookScreen.PAGE_WIDTH_UNSCALED * 2 * scale;
    int height = BookScreen.PAGE_HEIGHT_UNSCALED * scale;
    int page = screen.getPage_();
    GpuBuffer originalUniforms = RenderSystem.getGlobalSettingsUniform();
    RenderSystem.backupProjectionMatrix();
    try {
      // GuiRenderer draws to Minecraft's main target. Resize it only during this render,
      // restoring both its dimensions and the window render state before the next frame.
      target.resize(width, height);
      window.width = width;
      window.height = height;
      window.guiScale = scale;
      var gameState = minecraft.gameRenderer.getGameRenderState();
      uniforms.update(width, height, gameState.optionsRenderState.glintStrength, minecraft.level.getGameTime(), minecraft.getDeltaTracker(),
        gameState.optionsRenderState.menuBackgroundBlurriness, gameState.levelRenderState.cameraRenderState.pos,
        gameState.optionsRenderState.textureFiltering == TextureFilteringMethod.RGSS);
      RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1);
      minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
      screen.drawText = !html || page < 0;
      screen.tick();
      screen.extractRenderState(new GuiGraphicsExtractor(minecraft, renderState, 0, 0), 0, 0, 0);
      GpuBufferSlice fog = Objects.requireNonNull(RenderSystem.getShaderFog(), "GUI fog buffer");
      renderer.render(fog);
      renderer.endFrame();
      if (html) {
        String contents = page < 0 ? screen.coverToHtml(bookKey, title, BookCommand.VERSION_FULL, modName)
          : screen.pageToHtml(bookKey, title, BookCommand.VERSION_FULL, modName);
        Files.writeString(htmlDir.resolve(page < 0 ? "index.html" : "page-" + page + ".html"), contents, StandardCharsets.UTF_8);
      }
      capture(target, page);
    } catch (Exception exception) {
      fail(exception);
    } finally {
      window.width = originalWindowWidth;
      window.height = originalWindowHeight;
      window.guiScale = originalScale;
      target.resize(originalWidth, originalHeight);
      RenderSystem.setGlobalSettingsUniform(originalUniforms);
      RenderSystem.restoreProjectionMatrix();
    }
  }

  /** Screenshot's stock readback forces opaque alpha; book exports preserve the transparent background. */
  private void capture(RenderTarget target, int page) {
    GpuTexture texture = Objects.requireNonNull(target.getColorTexture());
    int width = target.width;
    int height = target.height;
    int pixelSize = texture.getFormat().pixelSize();
    GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Mantle book screenshot", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
      (long) width * height * pixelSize);
    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
    try {
      encoder.copyTextureToBuffer(texture, buffer, 0, () -> {
        try (buffer; GpuBuffer.MappedView mapped = encoder.mapBuffer(buffer, true, false);
             NativeImage image = new NativeImage(page < 0 ? width / 2 : width, height, false)) {
          int left = page < 0 ? width / 4 : 0;
          for (int y = 0; y < height; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
              image.setPixelABGR(x, height - y - 1, mapped.data().getInt(((left + x) + y * width) * pixelSize));
            }
          }
          image.writeToFile(screenshotDir.resolve((page < 0 ? "cover" : (html ? "clean_" : "page_") + page) + ".png"));
          minecraft.schedule(this::advance);
        } catch (Exception exception) {
          minecraft.schedule(() -> fail(exception));
        }
      }, 0);
    } catch (RuntimeException exception) {
      buffer.close();
      throw exception;
    }
  }

  private void advance() {
    if (active != this) {
      return;
    }
    try {
      if (screen.nextPage()) {
        renderPage();
        return;
      }
      if (html) {
        Files.writeString(htmlDir.resolve("gallery.html"), galleryHtml(bookKey, title, modName), StandardCharsets.UTF_8);
      }
      if (minecraft.player != null) {
        Component images = GeneratePackHelper.getOutputComponent(screenshotDir);
        minecraft.player.sendSystemMessage(html
          ? Component.translatable("command.mantle.book.export.html.success", images, GeneratePackHelper.getOutputComponent(htmlDir))
          : Component.translatable("command.mantle.book.export.success", images));
      }
      if (++bookIndex < books.size()) {
        beginBook();
      } else {
        finish();
      }
    } catch (Exception exception) {
      fail(exception);
    }
  }

  static String galleryHtml(String bookKey, String title, String modName) {
    return "---\nlayout: book-gallery\ntitle: " + title + " (" + BookCommand.VERSION_FULL + ") - Gallery\n"
      + "breadcrumb: Gallery\ndescription: Gallery of all pages for " + title + " from " + modName + " in Minecraft "
      + BookCommand.VERSION_FULL + ".\nbook: " + bookKey + "\nlink_prefix: ../\nlink_suffix: /gallery\n---\n\n";
  }

  private void fail(Exception exception) {
    Mantle.logger.error("Failed to export book", exception);
    if (minecraft.player != null) {
      minecraft.player.sendSystemMessage(Component.translatable("command.mantle.book.export.error_generic").withStyle(ChatFormatting.RED));
    }
    finish();
  }

  private void finish() {
    if (renderer != null) {
      renderer.close();
      renderer = null;
    }
    if (uniforms != null) {
      uniforms.close();
      uniforms = null;
    }
    if (minecraft.player != null) {
      ClientAdvancements.Listener listener = minecraft.screen instanceof BookScreen open ? open.advancementCache
        : minecraft.screen instanceof ClientAdvancements.Listener current ? current : null;
      minecraft.player.connection.getAdvancements().setListener(listener);
    }
    if (active == this) {
      active = null;
    }
  }
}
