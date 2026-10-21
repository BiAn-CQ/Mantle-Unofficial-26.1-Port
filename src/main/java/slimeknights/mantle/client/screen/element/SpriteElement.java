package slimeknights.mantle.client.screen.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Repositionable element represented by a screen sprite. */
public record SpriteElement(Identifier sprite, int width, int height) implements ScreenElement {
  @Override
  public void draw(GuiGraphicsExtractor graphics, int xPos, int yPos, int blitOffset) {
    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.sprite, xPos, yPos, this.width, this.height);
  }

  @Override
  public void drawInternal(GuiGraphicsExtractor graphics, int xPos, int yPos, int blitOffset, int uOffset, int vOffset, int width, int height) {
    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.sprite, this.width, this.height, uOffset, vOffset, xPos, yPos, width, height);
  }
}
