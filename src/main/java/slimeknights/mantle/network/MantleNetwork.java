package slimeknights.mantle.network;

import net.minecraft.network.protocol.PacketFlow;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferPacket;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.OpenLecternBookPacket;
import slimeknights.mantle.network.packet.OpenNamedBookPacket;
import slimeknights.mantle.network.packet.SwingArmPacket;
import slimeknights.mantle.network.packet.UpdateHeldPagePacket;
import slimeknights.mantle.network.packet.UpdateInventoryPagePacket;
import slimeknights.mantle.network.packet.UpdateLecternPagePacket;

public class MantleNetwork {
  /**
   * Network instance
   * 1: 1.11.101 and before
   * 2: 1.11.102 - New predicate types, enum loadable nullable field optimization
   * 3: 1.11.108 - New export book command and block entity packet helpers
   */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "3");

  /**
   * Registers packets into this network
   */
  public static void registerPackets() {
    INSTANCE.registerPacket(OpenLecternBookPacket.class, OpenLecternBookPacket::new, PacketFlow.CLIENTBOUND);
    INSTANCE.registerPacket(UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(DropLecternBookPacket.class, DropLecternBookPacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(SwingArmPacket.class, SwingArmPacket::new, PacketFlow.CLIENTBOUND);
    INSTANCE.registerPacket(OpenNamedBookPacket.class, OpenNamedBookPacket::new, PacketFlow.CLIENTBOUND);
    INSTANCE.registerPacket(FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, PacketFlow.CLIENTBOUND);
  }
}
