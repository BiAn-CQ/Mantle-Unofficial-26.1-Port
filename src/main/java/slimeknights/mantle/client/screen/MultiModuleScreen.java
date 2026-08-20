package slimeknights.mantle.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.inventory.WrapperSlot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Container screen which composes several independently positioned modules. */
public class MultiModuleScreen<CONTAINER extends MultiModuleContainerMenu<?>> extends AbstractContainerScreen<CONTAINER> {
  protected final List<ModuleScreen<?,?>> modules = new ArrayList<>();

  public int cornerX;
  public int cornerY;
  public int realWidth;
  public int realHeight;

  /*
   * 26.1 makes AbstractContainerScreen's dimensions final. Keep the old
   * mutable view for TConstruct modules while all extraction hooks below use
   * these values consistently.
   */
  protected int imageWidth;
  protected int imageHeight;

  public MultiModuleScreen(CONTAINER container, Inventory inventory, Component title) {
    this(container, inventory, title, 176, 166);
  }

  /**
   * Creates a composed container screen with the real 26.1 background size.
   * AbstractContainerScreen keeps these values final, so callers with a
   * non-standard background must provide them to its constructor instead of
   * changing only Mantle's compatibility fields after construction.
   */
  protected MultiModuleScreen(CONTAINER container, Inventory inventory, Component title, int imageWidth, int imageHeight) {
    super(container, inventory, title, imageWidth, imageHeight);
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.realWidth = -1;
    this.realHeight = -1;
  }

  protected void addModule(ModuleScreen<?,?> module) {
    this.modules.add(module);
  }

  /** Repositions a slot without mutating 26.1's final Slot coordinates. */
  protected void setSlotPosition(AbstractContainerMenu target, Slot slot, int x, int y) {
    int listIndex = target.slots.indexOf(slot);
    if (listIndex < 0 || listIndex >= target.slots.size()) {
      return;
    }
    Slot current = target.slots.get(listIndex);
    if (current.x != x || current.y != y) {
      target.slots.set(listIndex, WrapperSlot.positioned(current, x, y));
    }
    if (target != this.menu) {
      this.menu.setSubContainerSlotPosition(target, listIndex, x, y);
    }
  }

  public List<Rect2i> getModuleAreas() {
    // AbstractContainerScreen's dimensions are final in 26.1, so JEI cannot
    // infer the central panel from the compatibility layout fields. Expose it
    // alongside the modules to keep overlays out of the complete composed GUI.
    List<Rect2i> areas = new ArrayList<>(this.modules.size() + 1);
    areas.add(new Rect2i(this.cornerX, this.cornerY, this.realWidth, this.realHeight));
    for (ModuleScreen<?,?> module : this.modules) {
      areas.add(module.getArea());
    }
    return areas;
  }

  @Override
  protected void init() {
    if (this.realWidth > -1) {
      this.imageWidth = this.realWidth;
      this.imageHeight = this.realHeight;
    }
    super.init();
    this.cornerX = (this.width - this.imageWidth) / 2;
    this.cornerY = (this.height - this.imageHeight) / 2;
    this.leftPos = this.cornerX;
    this.topPos = this.cornerY;
    this.realWidth = this.imageWidth;
    this.realHeight = this.imageHeight;

    for (ModuleScreen<?,?> module : this.modules) {
      this.updateSubmodule(module);
    }
    for (ModuleScreen<?,?> module : this.modules) {
      module.init();
      this.updateSubmodule(module);
    }
  }

  /** Legacy-named background hook used by TConstruct screens. */
  protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
    for (ModuleScreen<?,?> module : this.modules) {
      module.handleDrawGuiContainerBackgroundLayer(graphics, partialTicks, mouseX, mouseY);
    }
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBg(graphics, partialTicks, mouseX, mouseY);
  }

  /**
   * AbstractContainerScreen translates slot and label extraction by
   * {@code leftPos/topPos}.  The 26.1 port keeps an expanded position there
   * when side modules extend beyond the main panel, while the actual
   * TConstruct background and slot coordinates are still relative to the
   * central panel.  Use the central panel as the extraction origin, matching
   * the old Mantle render path without mutating 26.1's final image dimensions.
   */
  @Override
  public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    int oldLeft = this.leftPos;
    int oldTop = this.topPos;
    this.leftPos = this.cornerX;
    this.topPos = this.cornerY;
    try {
      super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    } finally {
      this.leftPos = oldLeft;
      this.topPos = oldTop;
    }
  }

  /** Legacy-named foreground hook used by TConstruct screens. */
  protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    this.drawContainerName(graphics);
    this.drawPlayerInventoryName(graphics);

    for (ModuleScreen<?,?> module : this.modules) {
      graphics.pose().pushMatrix();
      graphics.pose().translate(module.getLeftPos() - this.leftPos, module.getTopPos() - this.topPos);
      module.handleDrawGuiContainerForegroundLayer(graphics, mouseX, mouseY);
      graphics.pose().popMatrix();
    }
  }

  @Override
  protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    this.renderLabels(graphics, mouseX, mouseY);
  }

  /** Legacy-named tooltip hook used by TConstruct screens. */
  protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    for (ModuleScreen<?,?> module : this.modules) {
      module.handleRenderHoveredTooltip(graphics, mouseX, mouseY);
    }
  }

  @Override
  protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    super.extractTooltip(graphics, mouseX, mouseY);
    this.renderTooltip(graphics, mouseX, mouseY);
  }

  protected void drawBackground(GuiGraphicsExtractor graphics, net.minecraft.resources.Identifier background) {
    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, background,
      this.cornerX, this.cornerY, 0, 0, this.realWidth, this.realHeight, 256, 256);
  }

  protected void drawContainerName(GuiGraphicsExtractor graphics) {
    graphics.text(this.font, this.getTitle(), 8, 6, 0x404040, false);
  }

  protected void drawPlayerInventoryName(GuiGraphicsExtractor graphics) {
    if (this.minecraft != null && this.minecraft.player != null) {
      graphics.text(this.font, this.minecraft.player.getInventory().getDisplayName(), 8, this.imageHeight - 96 + 2, 0x404040, false);
    }
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    for (ModuleScreen<?,?> module : this.modules) {
      this.updateSubmodule(module);
    }
  }

  @Override
  protected boolean isHovering(int left, int top, int right, int bottom, double pointX, double pointY) {
    pointX -= this.cornerX;
    pointY -= this.cornerY;
    return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
  }

  /** Compatibility overload used by module code when testing a slot. */
  public boolean isHovering(Slot slot, double mouseX, double mouseY) {
    return this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
  }

  @Override
  protected void extractSlot(GuiGraphicsExtractor graphics, Slot slotIn, int mouseX, int mouseY) {
    // Slot.index belongs to the backing inventory. The module lookup needs
    // the flattened menu-list index instead.
    ModuleScreen<?,?> module = this.getModuleForSlot(this.getMenu().slots.indexOf(slotIn));
    if (module != null) {
      Slot slot = slotIn;
      if (slotIn instanceof WrapperSlot wrapper) {
        slot = wrapper.parent;
      }
      if (!module.shouldDrawSlot(slot)) {
        return;
      }
    }
    super.extractSlot(graphics, slotIn, mouseX, mouseY);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());
    if (module != null && module.handleMouseClicked(event.x(), event.y(), event.button())) {
      return true;
    }
    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());
    if (module != null && module.handleMouseClickMove(event.x(), event.y(), event.button(), dragX)) {
      return true;
    }
    return super.mouseDragged(event, dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);
    if (module != null && module.handleMouseScrolled(mouseX, mouseY, scrollY)) {
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());
    if (module != null && module.handleMouseReleased(event.x(), event.y(), event.button())) {
      return true;
    }
    return super.mouseReleased(event);
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForPoint(double x, double y) {
    for (ModuleScreen<?,?> module : this.modules) {
      // Mouse events are already in screen coordinates.  The previous port
      // treated both the module bounds and the event as local coordinates,
      // adding cornerX/cornerY twice and shifting every module hitbox.
      if (module.isMouseInModule((int) x, (int) y)) {
        return module;
      }
    }
    return null;
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForSlot(int slotNumber) {
    return this.getModuleForContainer(this.getMenu().getSlotContainer(slotNumber));
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForContainer(AbstractContainerMenu container) {
    for (ModuleScreen<?,?> module : this.modules) {
      if (module.getMenu() == container) {
        return module;
      }
    }
    return null;
  }

  protected void updateSubmodule(ModuleScreen<?,?> module) {
    module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

    if (module.getLeftPos() < this.leftPos) {
      this.imageWidth += this.leftPos - module.getLeftPos();
      this.leftPos = module.getLeftPos();
    }
    if (module.getTopPos() < this.topPos) {
      this.imageHeight += this.topPos - module.getTopPos();
      this.topPos = module.getTopPos();
    }
    if (module.guiRight() > this.leftPos + this.imageWidth) {
      this.imageWidth = module.guiRight() - this.leftPos;
    }
    if (module.guiBottom() > this.topPos + this.imageHeight) {
      this.imageHeight = module.guiBottom() - this.topPos;
    }
  }
}
