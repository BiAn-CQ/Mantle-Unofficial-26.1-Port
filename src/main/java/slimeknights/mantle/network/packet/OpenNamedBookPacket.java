package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.client.BookCommand;

public class OpenNamedBookPacket implements IThreadsafePacket {
  private final Identifier book;

  public OpenNamedBookPacket(Identifier book) {
    this.book = book;
  }

  public OpenNamedBookPacket(FriendlyByteBuf buffer) {
    this.book = buffer.readIdentifier();
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeIdentifier(book);
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      bookData.openGui(Component.literal("Book"), "", null, null);
    } else {
      ClientOnly.errorStatus(book);
    }
  }

  static class ClientOnly {
    static void errorStatus(Identifier book) {
      BookCommand.bookNotFound(book);
    }
  }
}
