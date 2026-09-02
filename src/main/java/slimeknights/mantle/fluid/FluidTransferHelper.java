package slimeknights.mantle.fluid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;

import javax.annotation.Nullable;

import static slimeknights.mantle.util.TranslationHelper.COMMA_FORMAT;

/**
 * Fluid transfer helpers with Mantle's JSON-based container fallback and UI result handling.
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidTransferHelper {
  private static final String KEY_FILLED = Mantle.makeDescriptionId("block", "tank.filled");
  private static final String KEY_DRAINED = Mantle.makeDescriptionId("block", "tank.drained");

  /** Gets the given sound from the fluid */
  public static SoundEvent getSound(FluidStack fluid, SoundAction action, SoundEvent fallback) {
    SoundEvent event = fluid.getFluid().getFluidType().getSound(fluid, action);
    if (event == null) {
      return fallback;
    }
    return event;
  }

  /** Gets the empty sound for a fluid */
  public static SoundEvent getEmptySound(FluidStack fluid) {
    return getSound(fluid, SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
  }

  /** Gets the fill sound for a fluid */
  public static SoundEvent getFillSound(FluidStack fluid) {
    return getSound(fluid, SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL);
  }

  /**
   * Attempts to transfer fluid
   * @param input    Fluid source
   * @param output   Fluid destination
   * @param maxFill  Maximum to transfer
   * @return  True if transfer succeeded
   */
  public static FluidStack tryTransfer(ResourceHandler<FluidResource> input, ResourceHandler<FluidResource> output, int maxFill) {
    try (Transaction transaction = Transaction.openRoot()) {
      ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(input, output, resource -> true, maxFill, transaction);
      if (moved != null) {
        transaction.commit();
        return moved.resource().toStack(moved.amount());
      }
    }
    return FluidStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid
   * @param input    Fluid source
   * @param output   Fluid destination
   * @param fluid    Fluid to transfer, will not be modified. Precondition is it must be valid to drain from the input.
   * @return  True if transfer succeeded
   */
  public static FluidStack tryTransfer(ResourceHandler<FluidResource> input, ResourceHandler<FluidResource> output, FluidStack fluid) {
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource requested = FluidResource.of(fluid);
    try (Transaction transaction = Transaction.openRoot()) {
      ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(input, output, requested::equals, fluid.getAmount(), transaction);
      if (moved != null) {
        transaction.commit();
        return moved.resource().toStack(moved.amount());
      }
    }
    return FluidStack.EMPTY;
  }

  /** Returns the first extractable fluid without changing the handler. */
  private static FluidStack getFirstExtractable(ResourceHandler<FluidResource> handler) {
    try (Transaction transaction = Transaction.openRoot()) {
      ResourceStack<FluidResource> extracted = ResourceHandlerUtil.extractFirst(handler, resource -> true, Integer.MAX_VALUE, transaction);
      return extracted == null ? FluidStack.EMPTY : extracted.resource().toStack(extracted.amount());
    }
  }

  /** Return options for interaction methods */
  public enum FluidInteractionResult {
    /** Indicates fluid filled the item stack, draining the block entity */
    FILLED_STACK,
    /** Indicates fluid drained the stack, filling the block entity */
    DRAINED_STACK,
    /** Indicates there was a fluid container, but no fluid was transferred. Note that client side will never attempt transfer */
    CONTAINER,
    /** Indicates there was no block entity or the player was not holding a fluid container */
    MISSING;

    /** Returns true if fluid did move */
    public boolean didTransfer() {
      return this == FILLED_STACK || this == DRAINED_STACK;
    }

    /** Returns true if a container is present */
    public boolean hasContainer() {
      return this != MISSING;
    }
  }

  /**
   * Attempts to interact with a flilled bucket on a fluid tank. This is unique as it handles fish buckets, which don't expose fluid capabilities
   * @param world     World instance
   * @param pos       Block position
   * @param handler   Fluid handler in the block entity
   * @param player    Player
   * @param hand      Hand
   * @param offset    Direction to place fish
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   */
  public static FluidInteractionResult interactWithFilledBucket(Level world, BlockPos pos, ResourceHandler<FluidResource> handler, Player player, InteractionHand hand, Direction offset) {
    ItemStack held = player.getItemInHand(hand);
    if (held.getItem() instanceof BucketItem bucket) {
      Fluid fluid = bucket.content;
      if (fluid != Fluids.EMPTY) {
        if (!world.isClientSide()) {
          FluidStack fluidStack = new FluidStack(bucket.content, FluidType.BUCKET_VOLUME);
          try (Transaction transaction = Transaction.openRoot()) {
            // Fish buckets must be inserted completely before their extra content is spawned.
            if (handler.insert(FluidResource.of(fluidStack), FluidType.BUCKET_VOLUME, transaction) == FluidType.BUCKET_VOLUME) {
              transaction.commit();
              SoundEvent sound = getEmptySound(fluidStack);
              bucket.checkExtraContent(player, world, held, pos.relative(offset));
              world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
              player.sendOverlayMessage(Component.translatable(KEY_FILLED, COMMA_FORMAT.format(FluidType.BUCKET_VOLUME), fluidStack.getHoverName()));
              if (!player.isCreative()) {
                var remainder = held.getCraftingRemainder();
                player.setItemInHand(hand, remainder == null ? ItemStack.EMPTY : remainder.create());
              }
              return FluidInteractionResult.DRAINED_STACK;
            }
          }
        }
        return FluidInteractionResult.CONTAINER;
      }
    }
    return FluidInteractionResult.MISSING;
  }

  /** Plays the sound from filling a TE */
  public static void playEmptySound(Level world, BlockPos pos, Player player, FluidStack transferred) {
    world.playSound(null, pos, getEmptySound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    player.sendOverlayMessage(Component.translatable(KEY_FILLED, COMMA_FORMAT.format(transferred.getAmount()), transferred.getHoverName()));
  }

  /** Plays the sound from draining a TE */
  public static void playFillSound(Level world, BlockPos pos, Player player, FluidStack transferred) {
    world.playSound(null, pos, getFillSound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    player.sendOverlayMessage(Component.translatable(KEY_DRAINED, COMMA_FORMAT.format(transferred.getAmount()), transferred.getHoverName()));
  }

  /**
   * Base logic to interact with a tank by fetching it from the block entity.
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit position
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   */
  public static FluidInteractionResult interactWithContainer(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (!player.getItemInHand(hand).isEmpty()) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te != null) {
        ResourceHandler<FluidResource> handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, hit.getDirection());
        if (handler != null) {
          return interactWithContainer(world, pos, handler, player, hand);
        }
      }
    }
    return FluidInteractionResult.MISSING;
  }

  /**
   * Base logic to interact with a tank within a block entity.
   * @param world     World instance
   * @param pos       Tank position
   * @param teHandler Fluid handler in the block entity
   * @param player    Player instance
   * @param hand      Hand used
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   * @see #interactWithContainer(Level, BlockPos, ResourceHandler, Player, InteractionHand)
   */
  public static FluidInteractionResult interactWithContainer(Level world, BlockPos pos, ResourceHandler<FluidResource> teHandler, Player player, InteractionHand hand) {
    // fallback to JSON based transfer
    ItemStack stack = player.getItemInHand(hand);
    if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
      // only actually transfer on the serverside, client just has items
      if (!world.isClientSide()) {
        FluidStack currentFluid = getFirstExtractable(teHandler);
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, currentFluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, currentFluid, teHandler, TransferDirection.AUTO);
          if (result != null) {
            if (result.didFill()) {
              playFillSound(world, pos, player, result.fluid());
            } else {
              playEmptySound(world, pos, player, result.fluid());
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result.stack()));
            return result.didFill() ? FluidInteractionResult.FILLED_STACK : FluidInteractionResult.DRAINED_STACK;
          }
        }
      }
      return FluidInteractionResult.CONTAINER;
    }

    // if the item has a capability, do a direct transfer
    ItemAccess itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
    ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
    if (itemHandler != null) {
      FluidInteractionResult result = FluidInteractionResult.CONTAINER;
      if (!world.isClientSide()) {
        // first, try filling the TE from the item
        FluidStack transferred = tryTransfer(itemHandler, teHandler, Integer.MAX_VALUE);
        if (!transferred.isEmpty()) {
          playEmptySound(world, pos, player, transferred);
          result = FluidInteractionResult.DRAINED_STACK;
        } else {
          // if that failed, try filling the item handler from the TE
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
          if (!transferred.isEmpty()) {
            playFillSound(world, pos, player, transferred);
            result = FluidInteractionResult.FILLED_STACK;
          }
        }
      }
      return result;
    }
    return FluidInteractionResult.MISSING;
  }

  /**
   * Utility to try fluid item then bucket.
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit position
   * @return  True if interacted
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, Direction, Direction) 
   * @see #interactWithContainer(Level, BlockPos, Player, InteractionHand, BlockHitResult) 
   */
  public static boolean interactWithTank(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    Direction direction = hit.getDirection();
    return interactWithTank(world, pos, player, hand, direction, direction);
  }

  /**
   * Utility to try fluid item then bucket
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit direction
   * @param offset  Offset to spawn the mob in the bucket, if present
   * @return  True if interacted
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, BlockHitResult) 
   * @see #interactWithContainer(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   */
  public static boolean interactWithTank(Level world, BlockPos pos, Player player, InteractionHand hand, Direction hit, Direction offset) {
    if (!player.getItemInHand(hand).isEmpty()) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te != null) {
        ResourceHandler<FluidResource> handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, hit);
        if (handler != null) {
          return interactWithContainer(world, pos, handler, player, hand).hasContainer()
            || interactWithFilledBucket(world, pos, handler, player, hand, offset).hasContainer();
        }
      }
    }
    return false;
  }

  /**
   * Attempts to transfer fluid from the passed stack into a tank.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param direction  Determines whether we may empty the item, fill, or both
   * @return  Resulting stack after transfer
   */
  public static ItemStack interactWithTankSlot(ResourceHandler<FluidResource> teHandler, ItemStack stack, TransferDirection direction) {
    TransferResult result = interactWithStack(teHandler, stack, direction);
    return result != null ? result.stack() : ItemStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid from the passed stack into a tank.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param direction  Determines whether we may empty the item, fill, or both
   * @return  What was transferred and the resulting stack, or null if no transfer happened.
   */
  @Nullable
  public static TransferResult interactWithStack(ResourceHandler<FluidResource> teHandler, ItemStack stack, TransferDirection direction) {
    if (!stack.isEmpty()) {
      // fallback to JSON based transfer
      if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
        // only actually transfer on the serverside, client just has items
        FluidStack currentFluid = getFirstExtractable(teHandler);
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, currentFluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, currentFluid, teHandler, direction);
          if (result != null) {
            stack.shrink(1);
            return result;
          }
        }
      }

      // if the item has a capability, do a direct transfer
      ItemStack copy = stack.copyWithCount(1);
      SimpleContainer container = new SimpleContainer(copy);
      ItemAccess itemAccess = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(container), 0);
      ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      if (itemHandler != null) {
        // first, try filling the TE from the item
        FluidStack transferred = FluidStack.EMPTY;
        // reverse means try TE to item first
        boolean didFill = true;
        if (direction == TransferDirection.REVERSE) {
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
        }
        // if not reverse or reverse failed, try filling TE from item
        if (direction.canEmpty() && transferred.isEmpty()) {
          transferred = tryTransfer(itemHandler, teHandler, Integer.MAX_VALUE);
          if (!transferred.isEmpty()) {
            didFill = false;
          }
        }
        // if that failed, try filling the item handler from the TE
        if (direction != TransferDirection.REVERSE && direction.canFill() && transferred.isEmpty()) {
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
        }
        // if either worked, update the player's inventory
        if (!transferred.isEmpty()) {
          stack.shrink(1);
          return new TransferResult(container.getItem(0), transferred, didFill);
        }
      }
    }
    return null;
  }

  /**
   * Attempts to transfer fluid into the passed stack from the given handler.
   * Similar to {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} except filtered and unable to set direction.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param fluid      Determines the fluid used to fill the item
   * @return  Resulting stack after transfer
   */
  public static ItemStack fillFromTankSlot(ResourceHandler<FluidResource> teHandler, ItemStack stack, FluidStack fluid) {
    TransferResult result = fillStack(teHandler, stack, fluid);
    return result != null ? result.stack() : ItemStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid into the passed stack from the given handler.
   * Similar to {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} except filtered and unable to set direction.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param fluid      Determines the fluid used to fill the item
   * @return  Resulting stack after transfer
   */
  @Nullable
  public static TransferResult fillStack(ResourceHandler<FluidResource> teHandler, ItemStack stack, FluidStack fluid) {
    if (!stack.isEmpty()) {
      // fallback to JSON based transfer
      if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
        // only actually transfer on the serverside, client just has items
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, fluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, fluid, teHandler, TransferDirection.FILL_ITEM);
          if (result != null) {
            stack.shrink(1);
            return result;
          }
        }
      }

      // if the item has a capability, do a direct transfer
      ItemStack copy = stack.copyWithCount(1);
      SimpleContainer container = new SimpleContainer(copy);
      ItemAccess itemAccess = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(container), 0);
      ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      if (itemHandler != null) {
        // first, try filling the TE from the item
        FluidStack transferred = tryTransfer(teHandler, itemHandler, fluid.copy());
        if (!transferred.isEmpty()) {
          stack.shrink(1);
          return new TransferResult(container.getItem(0), transferred, true);
        }
      }
    }
    return null;
  }
  
  /**
   * Same as {@link net.minecraft.world.item.ItemUtils#createFilledResult(ItemStack, Player, ItemStack)} but doesn't shrink results or check creative.
   * Useful in UIs along {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} or {@link #fillFromTankSlot(ResourceHandler, ItemStack, FluidStack)}
   */
  public static ItemStack getOrTransferFilled(Player player, ItemStack emptyStack, ItemStack filledStack) {
    // if no more helpd
    if (emptyStack.isEmpty()) {
      return filledStack;
    }
    if (!player.getInventory().add(filledStack)) {
      player.drop(filledStack, false);
    }
    return emptyStack;
  }

  /** Plays sound only to the targeted player. Works by sending a targeted packet to server players, or a local packet to client. */
  @SuppressWarnings("deprecation")
  public static void playUISound(Player player, SoundEvent sound) {
    if (player.level().isClientSide()) {
      player.playSound(sound);
    } else if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), player.getSoundSource(), player.getX(), player.getY(), player.getZ(), 1, 1, player.getRandom().nextLong()));
    }
  }

  /**
   * Combination of {@link #getOrTransferFilled(Player, ItemStack, ItemStack)} and {@link #playUISound(Player, SoundEvent)}.
   * For working with {@link #interactWithStack(ResourceHandler, ItemStack, TransferDirection)} and {@link #fillStack(ResourceHandler, ItemStack, FluidStack)} in UIs.
   */
  public static ItemStack handleUIResult(Player player, ItemStack emptyStack, @Nullable TransferResult result) {
    if (result == null) {
      return emptyStack;
    }
    playUISound(player, result.getSound());
    return getOrTransferFilled(player, emptyStack, result.stack());
  }
}
