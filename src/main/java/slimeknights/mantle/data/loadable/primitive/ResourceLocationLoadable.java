package slimeknights.mantle.data.loadable.primitive;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Helper for the common case of making a string loadable that uses resource locations.
 * @param <T>
 * @see Loadables#RESOURCE_LOCATION
 */
@SuppressWarnings("unused")  // API
public interface ResourceLocationLoadable<T> extends StringLoadable<T> {
  @Override
  default com.google.gson.JsonElement serialize(T object, TypedMap context) {
    return new com.google.gson.JsonPrimitive(getKey(object, context).toString());
  }
  /** Standard implementation of a resource location loadable for raw resource locations. */
  StringLoadable<Identifier> DEFAULT = new ResourceLocationLoadable<>() {
    @Override
    public Identifier fromKey(Identifier name, String key, TypedMap context) {
      return name;
    }

    @Override
    public Identifier getKey(Identifier object, TypedMap context) {
      return object;
    }

    @Override
    public Identifier decode(net.minecraft.network.FriendlyByteBuf buffer, TypedMap context) {
      return buffer.readIdentifier();
    }

    @Override
    public void encode(net.minecraft.network.FriendlyByteBuf buffer, Identifier value, TypedMap context) {
      buffer.writeIdentifier(value);
    }
  };

  /**
   * Converts this value from a resource location.
   * @param name   Location to parse
   * @param key    Json key containing the value used for exceptions only.
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   * @return  Converted value.'
   * @throws com.google.gson.JsonSyntaxException  If no value exists for that key
   */
  T fromKey(Identifier name, String key, TypedMap context);

  /** Same as {@link #fromKey(Identifier, String, TypedMap)} but passes {@link TypedMap#EMPTY} for context. */
  @NonExtendable
  default T fromKey(Identifier name, String key) {
    return fromKey(name, key, TypedMap.EMPTY);
  }

  @Override
  default T parseString(String value, String key, TypedMap context) {
    return fromKey(JsonHelper.parseIdentifier(value, key), key, context);
  }

  /**
   * Converts this object to its serialized representation.
   * @param object  Object to serialize
   * @return  String representation of the object.
   * @throws RuntimeException  if unable to serialize this to a string
   */
  Identifier getKey(T object, TypedMap context);

  /** same as {@link #getKey(Object, TypedMap)} but passes {@link TypedMap#EMPTY} as context. */
  @NonExtendable
  default Identifier getKey(T object) {
    return getKey(object, TypedMap.EMPTY);
  }

  @Override
  default String getString(T object, TypedMap context) {
    return getKey(object, context).toString();
  }
}
