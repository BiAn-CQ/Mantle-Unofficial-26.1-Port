package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.MantleCommand;

import java.util.List;

/** Commands for opening books and exporting their images and HTML. */
public class BookCommand {
  static final String VERSION_FULL = SharedConstants.getCurrentVersion().name();
  static final String DEFAULT_BOOK_VERSION = VERSION_FULL.replace('.', '_');
  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(source -> MantleCommand.hasPermission(source, MantleCommand.PERMISSION_GAME_COMMANDS))
      .then(Commands.literal("open")
        .then(Commands.argument("id", IdentifierArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .executes(BookCommand::openBook)))
      .then(Commands.literal("export_images")
        .then(Commands.argument("domain", StringArgumentType.word()).suggests(MantleClientCommand.REGISTERED_BOOK_DOMAINS)
          .then(Commands.argument("scale", IntegerArgumentType.integer(1, 16))
            .executes(context -> exportDomain(context, IntegerArgumentType.getInteger(context, "scale"), false, "")))
          .executes(context -> exportDomain(context, 2, false, "")))
        .then(Commands.argument("id", IdentifierArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .then(Commands.argument("scale", IntegerArgumentType.integer(1, 16))
            .executes(context -> exportBook(context, IntegerArgumentType.getInteger(context, "scale"), false, "")))
          .executes(context -> exportBook(context, 2, false, ""))))
      .then(Commands.literal("export_html")
        .then(Commands.argument("domain", StringArgumentType.word()).suggests(MantleClientCommand.REGISTERED_BOOK_DOMAINS)
          .then(Commands.argument("version", StringArgumentType.word())
            .executes(context -> exportDomain(context, 2, true, StringArgumentType.getString(context, "version"))))
          .executes(context -> exportDomain(context, 2, true, "")))
        .then(Commands.argument("id", IdentifierArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .then(Commands.argument("book_key", StringArgumentType.word())
            .executes(context -> exportBook(context, 2, true, StringArgumentType.getString(context, "book_key"))))
          .executes(context -> exportBook(context, 2, true, ""))));
  }

  private static int exportBook(CommandContext<CommandSourceStack> context, int scale, boolean html, String key) {
    Identifier book = IdentifierArgument.getId(context, "id");
    return export(context, book.toString(), List.of(book), scale, html, key, false);
  }

  private static int exportDomain(CommandContext<CommandSourceStack> context, int scale, boolean html, String key) {
    String domain = StringArgumentType.getString(context, "domain");
    List<Identifier> books = BookLoader.getAllBooks().stream().filter(book -> domain.equals(book.getNamespace())).toList();
    return export(context, domain, books, scale, html, key, true);
  }

  private static int export(CommandContext<CommandSourceStack> context, String target, List<Identifier> books, int scale, boolean html, String key, boolean domain) {
    if (books.isEmpty() || books.stream().anyMatch(book -> BookLoader.getBook(book) == null)) {
      context.getSource().sendFailure(Component.translatable("command.mantle.book_test.not_found", target));
      return 0;
    }
    BookExporter.export(books, scale, html, key, domain);
    return books.size();
  }

  /** A domain export accepts a version suffix; a single book accepts its complete key. */
  static String bookKey(Identifier book, String key, boolean domain) {
    return !domain && !key.isEmpty() ? key : book.getPath() + '_' + (key.isEmpty() ? DEFAULT_BOOK_VERSION : key);
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
