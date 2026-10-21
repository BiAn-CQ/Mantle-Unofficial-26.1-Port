package slimeknights.mantle.client.screen.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Repositionable element represented by a texture with position internally. */
public record TextureElement(Identifier texture, int u, int v, int width, int height, int texWidth, int texHeight) implements ScreenElement {
  public TextureElement(Identifier texture, int u, int v, int width, int height) {
    this(texture, u, v, width, height, width, height);
  }

  /** Creates a new element from this texture with the X, Y, width, and height */
  public TextureElement move(int u, int v, int width, int height) {
    return new TextureElement(this.texture, u, v, width, height, this.texWidth, this.texHeight);
  }

  /** Creates a new element by offsetting this element by the given amount */
  public TextureElement shift(int uOffset, int vOffset) {
    return move(u + uOffset, v + vOffset, this.width, this.height);
  }

  @Override
  public void draw(GuiGraphicsExtractor graphics, int xPos, int yPos, int blitOffset) {
    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, xPos, yPos, this.u, this.v, this.width, this.height, this.width, this.height, this.texWidth, this.texHeight, -1);
  }

  @Override
  public void drawInternal(GuiGraphicsExtractor graphics, int xPos, int yPos, int blitOffset, int uOffset, int vOffset, int width, int height) {
    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, xPos, yPos, this.u + uOffset, this.v + vOffset, width, height, width, height, this.texWidth, this.texHeight, -1);
  }
}
