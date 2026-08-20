package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.NetworkWrapper;

/**
 * Packet interface to add common methods for registration
 */
public interface ISimplePacket extends CustomPacketPayload {
  /**
   * Encodes a packet for the buffer
   * @param buf  Buffer instance
   */
  void encode(FriendlyByteBuf buf);

  /**
   * Handles receiving the packet
   * @param context  Packet context
   */
  void handle(IPayloadContext context);

  @Override
  default Type<? extends CustomPacketPayload> type() {
    return NetworkWrapper.typeFor(getClass());
  }
}
