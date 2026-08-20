package slimeknights.mantle.util.typed;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedMapTest {
  private static final TypedMap.Key<String> TEXT = new TypedMap.Key<>() {};
  private static final TypedMap.Key<Integer> NUMBER = new TypedMap.Key<>() {};

  @Test
  void emptyMapHonorsDefaultValue() {
    assertEquals("fallback", TypedMap.EMPTY.getOrDefault(TEXT, "fallback"));
    assertNull(TypedMap.EMPTY.get(TEXT));
  }

  @Test
  void builderPreservesTypedValues() {
    TypedMap map = TypedMapBuilder.builder().put(TEXT, "value").put(NUMBER, 7).build();

    assertEquals(2, map.size());
    assertFalse(map.isEmpty());
    assertTrue(map.containsKey(TEXT));
    assertEquals("value", map.get(TEXT));
    assertEquals(7, map.get(NUMBER));
  }

  @Test
  void emptyBuilderUsesCanonicalEmptyMap() {
    assertSame(TypedMap.EMPTY, TypedMapBuilder.builder().build());
  }

  @Test
  void mutableMapComputesMissingValueOnlyOnce() {
    AtomicInteger calls = new AtomicInteger();
    MutableTypedMap.ComputingKey<String> key = () -> {
      calls.incrementAndGet();
      return "computed";
    };
    BackedTypedMap map = new BackedTypedMap();

    assertEquals("computed", map.computeIfAbsent(key));
    assertEquals("computed", map.computeIfAbsent(key));
    assertEquals(1, calls.get());
  }
}
