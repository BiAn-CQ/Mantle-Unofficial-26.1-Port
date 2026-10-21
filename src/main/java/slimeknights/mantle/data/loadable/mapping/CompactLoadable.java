package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Predicate;

/**
 * Loadable which uses a compact form if the condition is met
 * @param <T>  Object type
 */
public class CompactLoadable<T> implements Loadable<T> {
  private final Loadable<T> loadable;
  private final Loadable<T> compact;
  private final Predicate<T> compactCondition;

  protected CompactLoadable(Loadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
    this.loadable = loadable;
    this.compact = compact;
    this.compactCondition = compactCondition;
  }

  /**
   * Creates a new instance for a general loadable
   * @param loadable          Base loadable, used under most circumstances
   * @param compact           Compact form, will be used to deserialize primitives, and in serialization based on conditions
   * @param compactCondition  If true, uses the compact loadable
   * @param <T>  Object type
   */
  public static <T> Loadable<T> of(Loadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
    return new CompactLoadable<>(loadable, compact, compactCondition);
  }

  /**
   * Creates a new instance for a record loadable
   * @param loadable          Base loadable, used under most circumstances
   * @param compact           Compact form, will be used to deserialize primitives, and in serialization based on conditions
   * @param compactCondition  If true, uses the compact loadable
   * @param <T>  Object type
   */
  public static <T> RecordLoadable<T> of(RecordLoadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
    return new Record<>(loadable, compact, compactCondition);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (!element.isJsonObject()) {
      return compact.convert(element, key, context);
    }
    return loadable.convert(element, key, context);
  }

  @Override
  public JsonElement serialize(T object) {
    return serialize(object, TypedMap.EMPTY);
  }

  public JsonElement serialize(T object, TypedMap context) {
    if (compactCondition.test(object)) {
      return compact.serialize(object, context);
    }
    return loadable.serialize(object, context);
  }


  /* Networking */

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    encode(buffer, object, TypedMap.EMPTY);
  }

  public void encode(FriendlyByteBuf buffer, T object, TypedMap context) {
    loadable.encode(buffer, object, context);
  }

  /** Extension for records */
  private static class Record<T> extends CompactLoadable<T> implements RecordLoadable<T> {
    private final RecordLoadable<T> loadable;
    public Record(RecordLoadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
      super(loadable, compact, compactCondition);
      this.loadable = loadable;
    }

    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      return loadable.deserialize(json, context);
    }

    @Override
    public void serialize(T object, JsonObject json) {
      serialize(object, json, TypedMap.EMPTY);
    }

    public void serialize(T object, JsonObject json, TypedMap context) {
      loadable.serialize(object, json, context);
    }

    @Override
    public T decode(FriendlyByteBuf buffer, TypedMap context) {
      return loadable.decode(buffer, context);
    }
  }
}
