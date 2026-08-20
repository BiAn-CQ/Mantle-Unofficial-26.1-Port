package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.MantleCommand;

/** A command for opening Mantle books. */
public class BookCommand {
  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(source -> MantleCommand.hasPermission(source, MantleCommand.PERMISSION_GAME_COMMANDS))
      .then(Commands.literal("open")
        .then(Commands.argument("id", IdentifierArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .executes(BookCommand::openBook)));
  }

  /**
   * Opens the specified book
   * @param context  Command context
   * @return  Integer return
   */
  private static int openBook(CommandContext<CommandSourceStack> context) {
    Identifier book = IdentifierArgument.getId(context, "id");
    BookData bookData = BookLoader.getBook(book);
    if (bookData != null) {
      Minecraft.getInstance().execute(() -> bookData.openGui(Component.literal("Book"), "", null, null));
    }
    return 0;
  }

  public static void bookNotFound(Identifier book) {
    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("command.mantle.book_test.not_found", String.valueOf(book)));
  }
}
