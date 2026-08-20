package slimeknights.mantle.client.screen;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** A container screen that draws one fixed background texture. */
public class BackgroundContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
  protected final Identifier background;

  public BackgroundContainerScreen(T container, Inventory inventory, Component name, int height, Identifier background) {
    super(container, inventory, name, 176, height);
    this.background = background;
    this.inventoryLabelY = height - 94;
  }

  @Override
  protected void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos, this.topPos,
      0, 0, this.imageWidth, this.imageHeight, 256, 256);
  }

  @RequiredArgsConstructor(staticName = "of")
  public static class Factory<T extends AbstractContainerMenu> implements ScreenConstructor<T,BackgroundContainerScreen<T>> {
    private final Identifier background;
    private final int height;

    public static <T extends AbstractContainerMenu> Factory<T> ofName(int height, Identifier name) {
      return of(Identifier.fromNamespaceAndPath(name.getNamespace(), "textures/gui/" + name.getPath() + ".png"), height);
    }

    @Override
    public BackgroundContainerScreen<T> create(T menu, Inventory inventory, Component title) {
      return new BackgroundContainerScreen<>(menu, inventory, title, height, background);
    }
  }
}
