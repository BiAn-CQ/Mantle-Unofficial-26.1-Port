package slimeknights.mantle.fluid.transfer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.network.codec.StreamableCodec;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.recipe.NamedRecipe;
import java.util.List;

/** Complete transfer data shared with client-side container interaction and recipe viewers. */
@Internal
public record FluidContainerTransferPacket(List<NamedRecipe<IFluidContainerTransfer>> transfers) implements IThreadsafePacket {
  public static final StreamCodec<RegistryFriendlyByteBuf,FluidContainerTransferPacket> CODEC = NamedRecipe.streamCodec(new StreamableCodec<>(IFluidContainerTransfer.LOADER))
    .apply(ByteBufCodecs.list()).map(FluidContainerTransferPacket::new, FluidContainerTransferPacket::transfers);
  public FluidContainerTransferPacket { transfers = List.copyOf(transfers); }
  public FluidContainerTransferPacket(FriendlyByteBuf buffer) { this(CODEC.decode((RegistryFriendlyByteBuf)buffer).transfers()); }
  public void encode(FriendlyByteBuf buffer) { CODEC.encode((RegistryFriendlyByteBuf)buffer, this); }
  public void handleThreadsafe(IPayloadContext context) { FluidContainerTransferManager.INSTANCE.setTransfers(transfers); }
}
