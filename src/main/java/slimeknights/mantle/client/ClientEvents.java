package slimeknights.mantle.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.block.GaugeBlock;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.mantle.client.model.NativeMantleItemModel;
import slimeknights.mantle.client.model.NativeColoredBlockStateModel;
import slimeknights.mantle.client.model.NativeCompositeBlockStateModel;
import slimeknights.mantle.client.model.NativeRetexturedBlockStateModel;
import slimeknights.mantle.client.model.connected.NativeConnectedBlockStateModel;
import slimeknights.mantle.client.model.TextureColorHelper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.command.client.MantleClientCommand;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.mantle.fluid.texture.ClientInvertedFluidType;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.mantle.fluid.InvertedFluidType;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.registration.MantleRegistrations;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.util.OffhandCooldownTracker;
import slimeknights.mantle.util.RegistryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Mantle.modId, value = Dist.CLIENT)
public class ClientEvents {
  private static final Identifier CROSSHAIR_ATTACK_BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
  private static final Identifier CROSSHAIR_ATTACK_PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
  private static final Identifier HOTBAR_ATTACK_BACKGROUND = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
  private static final Identifier HOTBAR_ATTACK_PROGRESS = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");

  /** Called on construct to initiatlize things that need early entry */
  public static void onConstruct() {}

  @SuppressWarnings("ConstantConditions")
  @SubscribeEvent
  static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    if (MantleRegistrations.SIGN != null) {
      event.registerBlockEntityRenderer(MantleRegistrations.SIGN, StandingSignRenderer::new);
    }
    if (MantleRegistrations.HANGING_SIGN != null) {
      event.registerBlockEntityRenderer(MantleRegistrations.HANGING_SIGN, HangingSignRenderer::new);
    }
  }

  /** Registers native 26.1 item-model replacements for Mantle's dynamic layers. */
  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(Mantle.getResource("item_layer"), NativeMantleItemModel.ItemLayerUnbaked.MAP_CODEC);
    event.register(Mantle.getResource("custom_data_key"), NativeMantleItemModel.CustomDataKeyUnbaked.MAP_CODEC);
  }

  /** Registers Mantle's native connected-texture blockstate model. */
  @SubscribeEvent
  static void registerBlockStateModels(RegisterBlockStateModels event) {
    event.registerModel(Mantle.getResource("connected"), NativeConnectedBlockStateModel.Unbaked.MAP_CODEC);
    event.registerModel(Mantle.getResource("retextured"), NativeRetexturedBlockStateModel.Unbaked.MAP_CODEC);
    event.registerModel(Mantle.getResource("colored_block"), NativeColoredBlockStateModel.Unbaked.MAP_CODEC);
    event.registerModel(Mantle.getResource("composite"), NativeCompositeBlockStateModel.Unbaked.MAP_CODEC);
  }

  @SuppressWarnings("removal")
  @SubscribeEvent
  static void registerListeners(AddClientReloadListenersEvent event) {
    event.addListener(Mantle.getResource("model_helper"), ModelHelper.LISTENER);
    event.addListener(Mantle.getResource("books"), new BookLoader());
    ResourceColorManager.init(event);
    FluidTooltipHandler.init(event);
    FluidTextureManager.init(event);
    event.addListener(Mantle.getResource("fluid_cuboids"), FluidCuboid.REGISTRY);
    event.addListener(Mantle.getResource("render_items"), RenderItem.REGISTRY);
    event.addListener(Mantle.getResource("render_item_states"), RenderItem.STATE_REGISTRY);
    event.addListener(Mantle.getResource("texture_colors"), TextureColorHelper.RELOAD_LISTENER);
  }

  @SubscribeEvent
  static void registerFluidModels(RegisterFluidModelsEvent event) {
    FluidTextureManager.registerModels(event);
  }

  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    NeoForgeRegistries.FLUID_TYPES.stream().forEach(type -> {
      if (type instanceof InvertedFluidType) {
        event.registerFluidType(new ClientInvertedFluidType(type), type);
      } else if (type instanceof TextureFluidType) {
        event.registerFluidType(new ClientTextureFluidType(type), type);
      }
    });
  }

  @SubscribeEvent
  static void clientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> RegistrationHelper.forEachWoodType(Sheets::addWoodType));

    BookLoader.registerBook(Mantle.getResource("test"), new FileRepository(Mantle.getResource("books/test")));
    MantleClientCommand.init();
  }

  @SubscribeEvent
  static void commonSetup(FMLCommonSetupEvent event) {
    NeoForge.EVENT_BUS.register(new ExtraHeartRenderHandler());
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiLayerEvent.Post.class, ClientEvents::renderOffhandAttackIndicator);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiLayerEvent.Post.class, ClientEvents::renderGaugeTooltip);
  }

  // registered with FORGE bus
  private static void renderOffhandAttackIndicator(RenderGuiLayerEvent.Post event) {
    // must have a player, not be in spectator, and have the indicator enabled
    Minecraft minecraft = Minecraft.getInstance();
    Options settings = minecraft.options;
    AttackIndicatorStatus indicator = settings.attackIndicator().get();
    if (minecraft.player == null || minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR || indicator == AttackIndicatorStatus.OFF) {
      return;
    }

    // will be true for hotbar, false for crosshair
    boolean isHotbar = VanillaGuiLayers.HOTBAR.equals(event.getName());
    if (!isHotbar && !VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
      return;
    }

    // fetch the current cooldown
    OffhandCooldownTracker tracker = OffhandCooldownTracker.get(minecraft.player);
    if (tracker == null) {
      return;
    }
    float cooldown = tracker.getCooldown();
    if (cooldown >= 1.0f) {
      return;
    }

    // show attack indicator
    GuiGraphicsExtractor graphics = event.getGuiGraphics();
    switch (indicator) {
      case CROSSHAIR:
        if (!isHotbar && minecraft.options.getCameraType().isFirstPerson()) {
          if (!minecraft.getDebugOverlay().showDebugScreen() || settings.hideGui || minecraft.player.isReducedDebugInfo() || settings.reducedDebugInfo().get()) {
            int scaledHeight = graphics.guiHeight();
            // integer division makes this a pain to line up, there might be a simplier version of this formula but I cannot think of one
            int y = (scaledHeight / 2) - 14 + (2 * (scaledHeight % 2));
            int x = graphics.guiWidth() / 2 - 8;
            int width = (int)(cooldown * 17.0F);
            graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_BACKGROUND, x, y, 16, 4);
            graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_PROGRESS, 16, 4, 0, 0, x, y, width, 4);
          }
        }
        break;
      case HOTBAR:
        if (isHotbar && minecraft.getCameraEntity() == minecraft.player) {
          int centerWidth = graphics.guiWidth() / 2;
          int y = graphics.guiHeight() - 20;
          int x;
          // opposite of the vanilla hand location, extra bit to offset past the offhand slot
          if (minecraft.player.getMainArm() == HumanoidArm.RIGHT) {
            x = centerWidth - 91 - 22 - 32;
          } else {
            x = centerWidth + 91 + 6 + 32;
          }
//          RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
          int l1 = (int)(cooldown * 19.0F);
          graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_BACKGROUND, x, y, 18, 18);
          graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_PROGRESS, 18, 18, 0, 18 - l1, x, y + 18 - l1, 18, l1);
        }
        break;
    }
  }



  /** Renders the tooltip when targeting the gauge block */
  private static void renderGaugeTooltip(RenderGuiLayerEvent.Post event) {
    if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
      return;
    }
    // must not be in a screen, though chat is fine
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen != null && minecraft.screen.getClass() != ChatScreen.class) {
      return;
    }
    // must have a hit result
    if (minecraft.level == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
      return;
    }
    BlockHitResult blockHit = (BlockHitResult) minecraft.hitResult;
    BlockPos pos = blockHit.getBlockPos();

    // must be targeting a gauge
    BlockState targeted = minecraft.level.getBlockState(blockHit.getBlockPos());
    if (!targeted.is(MantleTags.Blocks.GAUGES)) {
      return;
    }
    BlockEntity gaugeContainer;
    BlockPos containerPos;
    Direction side;
    if (targeted.is(MantleTags.Blocks.ATTACHED_GAUGES)) {
      side = targeted.getValue(BlockStateProperties.FACING);
      containerPos = pos.relative(side.getOpposite());
      gaugeContainer = minecraft.level.getBlockEntity(containerPos);
    } else {
      side = blockHit.getDirection();
      containerPos = pos;
      gaugeContainer = minecraft.level.getBlockEntity(pos);
    }
    // must have a block entity behind the gauge that is not blacklisted
    if (gaugeContainer == null || RegistryHelper.contains(BuiltInRegistries.BLOCK_ENTITY_TYPE, MantleTags.BlockEntities.GAUGE_BLACKLIST, gaugeContainer.getType())) {
      return;
    }
    ResourceHandler<FluidResource> handler = minecraft.level.getCapability(Capabilities.Fluid.BLOCK, containerPos, side);
    if (handler == null || handler.size() <= 0) {
      return;
    }
    // if the fluid is empty, just render the capacity
    FluidStack fluid = FluidUtil.getStack(handler, 0);
    int capacity = handler.getCapacityAsInt(0, handler.getResource(0));
    List<Component> tooltip;
    if (fluid.isEmpty()) {
      tooltip = List.of(GaugeBlock.formatCapacity(capacity));
    } else if (RegistryHelper.contains(BuiltInRegistries.BLOCK_ENTITY_TYPE, MantleTags.BlockEntities.HIDES_GAUGE_AMOUNT, gaugeContainer.getType())) {
      // in the tag, don't show capacity
      Identifier id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
      tooltip = new ArrayList<>(3);
      tooltip.add(fluid.getHoverName());
      FluidTooltipHandler.appendAdvanced(id, tooltip);
      tooltip.add(GaugeBlock.formatCapacity(capacity).withStyle(ChatFormatting.GRAY));
      tooltip.add(FluidTooltipHandler.formatModName(id));
    } else {
      // render full fluid tooltip
      tooltip = FluidTooltipHandler.getFluidTooltip(fluid);
    }

    int x = minecraft.getWindow().getGuiScaledWidth() / 2;
    int y = minecraft.getWindow().getGuiScaledHeight() / 2;
    event.getGuiGraphics().setTooltipForNextFrame(minecraft.font, tooltip, Optional.empty(), x, y);
  }
}
