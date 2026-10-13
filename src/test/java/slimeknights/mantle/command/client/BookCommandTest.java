package slimeknights.mantle.command.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookCommandTest {
  private static final Identifier BOOK = Identifier.fromNamespaceAndPath("mantle", "test_book");

  @Test
  void singleBookExportUsesTheSuppliedKeyVerbatim() {
    assertThat(BookCommand.bookKey(BOOK, "my_book_key", false)).isEqualTo("my_book_key");
  }

  @Test
  void domainExportAddsTheVersionSuffixToEachBook() {
    assertThat(BookCommand.bookKey(BOOK, "custom_version", true)).isEqualTo("test_book_custom_version");
    assertThat(BookCommand.bookKey(BOOK, "", false)).isEqualTo(BookCommand.bookKey(BOOK, "", true));
    assertThat(BookCommand.bookKey(BOOK, "", false)).startsWith("test_book_");
  }

  @Test
  void namespacedBookIdsAndDomainExportsParseIntoTheirOwnBranches() {
    var book = Commands.literal("book");
    BookCommand.register(book);
    book.requires(source -> true);
    CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    dispatcher.register(Commands.literal("mantle").then(book));
    CommandSourceStack source = mock(CommandSourceStack.class);
    String html = "mantle book export_html mantle:test custom_key";
    var single = dispatcher.parse(html, source);
    assertThat(single.getReader().canRead()).isFalse();
    assertThat(single.getExceptions()).isEmpty();
    assertThat(single.getContext().build(html).getArgument("id", Identifier.class)).isEqualTo(Identifier.parse("mantle:test"));
    String domain = "mantle book export_html mantle custom_version";
    var all = dispatcher.parse(domain, source);
    assertThat(all.getReader().canRead()).isFalse();
    assertThat(all.getContext().build(domain).getArgument("domain", String.class)).isEqualTo("mantle");
    String images = "mantle book export_images mantle:test 3";
    var scaled = dispatcher.parse(images, source);
    assertThat(scaled.getReader().canRead()).isFalse();
    assertThat(scaled.getContext().build(images).getArgument("scale", Integer.class)).isEqualTo(3);
  }
}
