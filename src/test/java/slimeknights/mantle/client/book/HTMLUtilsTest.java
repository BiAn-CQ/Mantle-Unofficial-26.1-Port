package slimeknights.mantle.client.book;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.html.HtmlElement;
import slimeknights.mantle.util.html.HtmlSerializable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HTMLUtilsTest {
  @Test
  void translatedArgumentsPreserveTheirStyleWithoutStylingSurroundingText() {
    Component component = Component.translatableWithFallback("mantle.test.styled_argument", "Before %s after",
      Component.literal("argument").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
    String html = render(HTMLUtils.toHtml(component));
    assertThat(html).startsWith("Before ").contains("font-weight: bold", "argument").containsIgnoringCase("55ff55").endsWith("</span> after");
  }

  @Test
  void inheritedStylesAndNewlinesArePreservedInTooltipLines() {
    Component component = Component.literal("parent ").append(Component.literal("child\nnext")).withStyle(ChatFormatting.ITALIC);
    String html = render(HTMLUtils.toTooltip(List.of(component, Component.literal("last line"))));
    assertThat(html).contains("font-style: italic", "parent ", "child", "next", "last line");
    assertThat(html.indexOf("last line")).isGreaterThan(html.indexOf("next"));
  }

  @Test
  void tooltipApostrophesUseTheSingleQuoteEntity() {
    String html = HtmlElement.span().minetip(HTMLUtils.toTooltip(List.of(Component.literal("O'Connor\"s tooltip")))).toHtml();
    assertThat(html).contains("O&#39;Connor&quot;s tooltip").doesNotContain("O&quot;Connor");
  }

  private static String render(HtmlSerializable element) {
    StringBuilder builder = new StringBuilder();
    element.toHtml(builder, "");
    return builder.toString();
  }
}
