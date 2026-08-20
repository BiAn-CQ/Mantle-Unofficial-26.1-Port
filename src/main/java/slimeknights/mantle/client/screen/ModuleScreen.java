package slimeknights.mantle.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** A container-backed sub-screen rendered as part of a {@link MultiModuleScreen}. */
public abstract class ModuleScreen<P extends MultiModuleScreen<?>, C extends AbstractContainerMenu> extends AbstractContainerScreen<C> {
  protected final P parent;
  protected final boolean right;
  protected final boolean bottom;

  /*
   * AbstractContainerScreen owns final 176x166 dimensions in 26.1.  A module
   * is deliberately sized by its parent layout, so keep a separate mutable
   * layout size just as MultiModuleScreen does.  The extraction hooks and
   * position calculations in this class use these fields consistently.
   */
  protected int imageWidth;
  protected int imageHeight;

  public int yOffset;
  public int xOffset;

  protected ModuleScreen(P parent, C container, Inventory inventory, Component title, boolean right, boolean bottom) {
    super(container, inventory, title);
    this.parent = parent;
    this.right = right;
    this.bottom = bottom;
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  /** Mutable layout width used by Mantle/TConstruct module screens. */
  public int getLayoutWidth() {
    return this.imageWidth;
  }

  /** Mutable layout height used by Mantle/TConstruct module screens. */
  public int getLayoutHeight() {
    return this.imageHeight;
  }

  /** Expands the module's mutable layout width. */
  public void setLayoutWidth(int width) {
    this.imageWidth = width;
  }

  /** Expands the module's mutable layout height. */
  public void setLayoutHeight(int height) {
    this.imageHeight = height;
  }

  /** Offsets the module's current GUI position after its border changes. */
  public void offsetLayoutPosition(int x, int y) {
    this.leftPos += x;
    this.topPos += y;
  }

  /**
   * Repositions a menu slot through a wrapper compatible with 26.1's final
   * Slot coordinates.  The wrapper keeps the original inventory and slot
   * index, so server clicks and item synchronization still target the same
   * logical slot.
   */
  protected void setSlotPosition(AbstractContainerMenu target, Slot slot, int x, int y) {
    this.parent.setSlotPosition(target, slot, x, y);
  }

  public int guiRight() {
    return this.leftPos + this.imageWidth;
  }

  public int guiBottom() {
    return this.topPos + this.imageHeight;
  }

  public Rect2i getArea() {
    return new Rect2i(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
  }

  @Override
  public void init() {
    this.leftPos = (this.width - this.imageWidth) / 2;
    this.topPos = (this.height - this.imageHeight) / 2;
  }

  public void updatePosition(int parentX, int parentY, int parentSizeX, int parentSizeY) {
    this.leftPos = this.right ? parentX + parentSizeX : parentX - this.imageWidth;
    this.topPos = this.bottom ? parentY + parentSizeY - this.imageHeight : parentY;
    this.leftPos += this.xOffset;
    this.topPos += this.yOffset;
  }

  public boolean shouldDrawSlot(Slot slot) {
    return true;
  }

  public boolean isMouseInModule(int mouseX, int mouseY) {
    return mouseX >= this.leftPos && mouseX < this.guiRight() && mouseY >= this.topPos && mouseY < this.guiBottom();
  }

  public boolean isMouseOverFullSlot(double mouseX, double mouseY) {
    for (Slot slot : this.menu.slots) {
      if (this.parent.isHovering(slot, mouseX, mouseY) && slot.hasItem()) {
        return true;
      }
    }
    return false;
  }

  /** Legacy-named hook retained for TConstruct's screen modules. */
  protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {}

  /** Legacy-named hook retained for TConstruct's screen modules. */
  protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

  /** Legacy-named hook retained for TConstruct's screen modules. */
  protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

  public void handleDrawGuiContainerBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
    this.renderBg(graphics, partialTicks, mouseX, mouseY);
  }

  public void handleDrawGuiContainerForegroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    this.renderLabels(graphics, mouseX, mouseY);
  }

  public void handleRenderHoveredTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    this.renderTooltip(graphics, mouseX, mouseY);
  }

  public boolean handleMouseClicked(double mouseX, double mouseY, int mouseButton) {
    return false;
  }

  public boolean handleMouseClickMove(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick) {
    return false;
  }

  public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
    return false;
  }

  public boolean handleMouseScrolled(double mouseX, double mouseY, double delta) {
    return false;
  }
}
