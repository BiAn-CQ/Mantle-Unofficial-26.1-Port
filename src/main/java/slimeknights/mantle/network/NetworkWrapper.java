package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.ISimplePacket;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A small network implementation/wrapper using AbstractPackets instead of IMessages.
 * Instantiate in your mod class and register your packets accordingly.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  private static final List<NetworkWrapper> WRAPPERS = new ArrayList<>();
  private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new ConcurrentHashMap<>();

  private final Identifier channelName;
  private final String version;
  private final List<Registration<?>> registrations = new ArrayList<>();
  private int id;

  /**
   * Creates a new network wrapper
   * @param channelName  Unique packet channel name
   * @deprecated Give your channel a version number.
   */
  @Deprecated
  /** Network instance */
  public final Sender network = new Sender();
  /* Sending packets */

  @Deprecated
  public NetworkWrapper(Identifier channelName) {
    this(channelName, "1");
  }

  public NetworkWrapper(Identifier channelName, String version) {
    this.channelName = channelName;
    this.version = version;
    synchronized (WRAPPERS) {
      WRAPPERS.add(this);
    }
  }

  /**
   * Registers a new {@link ISimplePacket}
   * @param clazz    Packet class
   * @param decoder  Packet decoder, typically the constructor
   * @param <MSG>  Packet class type
   */
  public <MSG extends ISimplePacket> void registerPacket(Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder, @Nullable PacketFlow direction) {
    registerPacket(clazz, ISimplePacket::encode, decoder, ISimplePacket::handle, direction);
  }

  public <MSG extends ISimplePacket> void registerPacket(
    Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
    BiConsumer<MSG, IPayloadContext> consumer, @Nullable PacketFlow direction) {
    registerPacketNoLogger(clazz, wrapLogger(clazz, encoder), wrapLogger(clazz, decoder), consumer, direction);
  }

  public <MSG extends ISimplePacket> void registerPacketNoLogger(
    Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
    BiConsumer<MSG, IPayloadContext> consumer, @Nullable PacketFlow direction) {
    Identifier packetId = channelName.withSuffix("/" + id++);
    CustomPacketPayload.Type<MSG> type = new CustomPacketPayload.Type<>(packetId);
    CustomPacketPayload.Type<?> previous = TYPES.putIfAbsent(clazz, type);
    if (previous != null && !previous.equals(type)) {
      throw new IllegalStateException("Packet class " + clazz.getName() + " registered with multiple IDs");
    }
    StreamCodec<RegistryFriendlyByteBuf, MSG> codec = StreamCodec.of(
      (buffer, message) -> encoder.accept(message, buffer),
      buffer -> decoder.apply(buffer));
    registrations.add(new Registration<>(type, codec, consumer, direction));
  }

  /** Wraps the given encoder function */
  private static <MSG> BiConsumer<MSG, FriendlyByteBuf> wrapLogger(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder) {
    return (message, buffer) -> {
      try {
        encoder.accept(message, buffer);
      } catch (RuntimeException exception) {
        Mantle.logger.error("Exception while encoding packet of class {}", clazz.getName(), exception);
        throw exception;
      }
    };
  }

  /** Wraps the given decoder function */
  private static <MSG> Function<FriendlyByteBuf, MSG> wrapLogger(Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder) {
    return buffer -> {
      try {
        return decoder.apply(buffer);
      } catch (RuntimeException exception) {
        Mantle.logger.error("Exception while decoding packet of class {}", clazz.getName(), exception);
        throw exception;
      }
    };
  }

  public static void registerPayloads(RegisterPayloadHandlersEvent event) {
    List<NetworkWrapper> snapshot;
    synchronized (WRAPPERS) {
      snapshot = List.copyOf(WRAPPERS);
    }
    for (NetworkWrapper wrapper : snapshot) {
      PayloadRegistrar registrar = event.registrar(wrapper.version);
      for (Registration<?> registration : wrapper.registrations) {
        register(registrar, registration);
      }
    }
  }

  private static <MSG extends ISimplePacket> void register(PayloadRegistrar registrar, Registration<MSG> registration) {
    if (registration.direction == PacketFlow.CLIENTBOUND) {
      registrar.playToClient(registration.type, registration.codec, registration.handler::accept);
    } else if (registration.direction == PacketFlow.SERVERBOUND) {
      registrar.playToServer(registration.type, registration.codec, registration.handler::accept);
    } else {
      registrar.playBidirectional(registration.type, registration.codec, registration.handler::accept, registration.handler::accept);
    }
  }

  @SuppressWarnings("unchecked")
  public static <MSG extends ISimplePacket> CustomPacketPayload.Type<MSG> typeFor(Class<?> packetClass) {
    CustomPacketPayload.Type<?> type = TYPES.get(packetClass);
    if (type == null) {
      throw new IllegalStateException("Unregistered packet class " + packetClass.getName());
    }
    return (CustomPacketPayload.Type<MSG>)type;
  }

  public void sendToServer(ISimplePacket message) {
    ClientOnly.sendToServer(message);
  }

  /**
   * Sends a vanilla packet to the given entity
   * @param player  Player receiving the packet
   * @param packet  Packet
   */
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(packet);
    }
  }

  /**
   * Sends a packet to a player
   * @param msg     Packet
   * @param player  Player to send
   */
  public void sendTo(ISimplePacket message, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(message, serverPlayer);
    }
  }

  /**
   * Sends a packet to a player
   * @param msg     Packet
   * @param player  Player to send
   */
  public void sendTo(ISimplePacket message, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      PacketDistributor.sendToPlayer(player, message);
    }
  }

  /**
   * Sends a packet to players near a location
   * @param msg          Packet to send
   * @param serverWorld  World instance
   * @param position     Position within range
   */
  public void sendToClientsAround(ISimplePacket message, ServerLevel level, BlockPos position) {
    PacketDistributor.sendToPlayersTrackingChunk(level, level.getChunkAt(position).getPos(), message);
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTrackingAndSelf(ISimplePacket message, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, message);
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTracking(ISimplePacket message, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntity(entity, message);
  }

  @Deprecated
  public final class Sender {
  /**
   * Sends a packet to the server
   * @param msg  Packet to send
   */
    public void sendToServer(Object message) {
      if (!(message instanceof ISimplePacket packet)) {
        throw new IllegalArgumentException("Message must implement ISimplePacket");
      }
      NetworkWrapper.this.sendToServer(packet);
    }
  }

  private static final class ClientOnly {
    private static void sendToServer(ISimplePacket message) {
      net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(message);
    }
  }

  private record Registration<MSG extends ISimplePacket>(
    CustomPacketPayload.Type<MSG> type,
    StreamCodec<RegistryFriendlyByteBuf, MSG> codec,
    BiConsumer<MSG, IPayloadContext> handler,
    @Nullable PacketFlow direction) {}
}
