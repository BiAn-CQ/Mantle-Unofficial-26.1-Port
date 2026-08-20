package slimeknights.mantle.data.loadable;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.CharacterLoadable;
import slimeknights.mantle.data.loadable.primitive.DoubleLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.LongLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimitiveLoadableTest {
  private enum Mode { FIRST, SECOND }

  @Test
  void booleanLoadableAcceptsOnlyBooleanNames() {
    assertTrue(BooleanLoadable.DEFAULT.convert(new JsonPrimitive(true), "flag"));
    assertEquals(false, BooleanLoadable.DEFAULT.parseString("FALSE", "flag"));
    assertThrows(JsonSyntaxException.class, () -> BooleanLoadable.DEFAULT.parseString("yes", "flag"));
  }

  @Test
  void integerRangeIncludesBothBounds() {
    IntLoadable loadable = IntLoadable.range(-2, 2);
    assertEquals(-2, loadable.convert(new JsonPrimitive(-2), "value"));
    assertEquals(2, loadable.convert(new JsonPrimitive(2), "value"));
    assertThrows(JsonSyntaxException.class, () -> loadable.convert(new JsonPrimitive(3), "value"));
    assertThrows(JsonSyntaxException.class, () -> loadable.serialize(-3));
  }

  @Test
  void floatingPointRangesRejectOutOfRangeAndNan() {
    assertEquals(0.5f, FloatLoadable.PERCENT.convert(new JsonPrimitive(0.5f), "value"));
    assertEquals(1.0d, DoubleLoadable.PERCENT.convert(new JsonPrimitive(1.0d), "value"));
    assertThrows(JsonSyntaxException.class, () -> FloatLoadable.PERCENT.serialize(Float.NaN));
    assertThrows(JsonSyntaxException.class, () -> DoubleLoadable.PERCENT.convert(new JsonPrimitive(-0.01d), "value"));
  }

  @Test
  void longStringFormRoundTripsItsRadix() {
    var hexadecimal = LongLoadable.ANY.asString(16);
    assertEquals(255L, hexadecimal.parseString("ff", "value"));
    assertEquals("ff", hexadecimal.getString(255L));
    assertThrows(JsonSyntaxException.class, () -> hexadecimal.parseString("not-hex", "value"));
  }

  @Test
  void stringsAndCharactersEnforceLength() {
    StringLoadable<String> shortString = StringLoadable.maxLength(3);
    assertEquals("abc", shortString.parseString("abc", "name"));
    assertThrows(JsonSyntaxException.class, () -> shortString.parseString("abcd", "name"));
    assertEquals('x', CharacterLoadable.DEFAULT.parseString("x", "character"));
    assertThrows(JsonSyntaxException.class, () -> CharacterLoadable.DEFAULT.parseString("xy", "character"));
  }

  @Test
  void enumLoadableUsesStableLowercaseNames() {
    EnumLoadable<Mode> loadable = new EnumLoadable<>(Mode.class);
    assertEquals(Mode.SECOND, loadable.parseString("second", "mode"));
    assertEquals("first", loadable.getString(Mode.FIRST));
    assertThrows(JsonSyntaxException.class, () -> loadable.parseString("missing", "mode"));
  }

  @Test
  void alphaColorSupportsRgbAndArgb() {
    assertEquals(0xFF112233, ColorLoadable.ALPHA.parseString("112233", "color"));
    assertEquals(0x80112233, ColorLoadable.ALPHA.parseString("80112233", "color"));
    assertEquals("80112233", ColorLoadable.ALPHA.getString(0x80112233));
  }

  @Test
  void noAlphaColorForcesOpaqueAndRejectsInvalidInput() {
    assertEquals(0xFFABCDEF, ColorLoadable.NO_ALPHA.parseString("ABCDEF", "color"));
    assertEquals("ABCDEF", ColorLoadable.NO_ALPHA.getString(0x80ABCDEF));
    assertThrows(JsonSyntaxException.class, () -> ColorLoadable.NO_ALPHA.parseString("", "color"));
    assertThrows(JsonSyntaxException.class, () -> ColorLoadable.ALPHA.parseString("", "color"));
    assertThrows(JsonSyntaxException.class, () -> ColorLoadable.NO_ALPHA.parseString("12345678", "color"));
  }
}
