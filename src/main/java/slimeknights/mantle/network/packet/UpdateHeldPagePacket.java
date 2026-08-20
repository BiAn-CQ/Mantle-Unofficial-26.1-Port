package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.client.book.BookHelper;

/**
 * Packet to update the page in a book in the players hand
 */
public class UpdateHeldPagePacket implements IThreadsafePacket {
  private final InteractionHand hand;
  private final String page;

  public UpdateHeldPagePacket(InteractionHand hand, String page) {
    this.hand = hand;
    this.page = page;
  }

  public UpdateHeldPagePacket(FriendlyByteBuf buffer) {
    this.hand = buffer.readEnum(InteractionHand.class);
    this.page = buffer.readUtf(100);
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeEnum(hand);
    buf.writeUtf(this.page);
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    Player player = context.player();
    if (this.page != null) {
      ItemStack stack = player.getItemInHand(hand);
      if (!stack.isEmpty()) {
        BookHelper.writeSavedPageToBook(stack, this.page);
      }
    }
  }
}
