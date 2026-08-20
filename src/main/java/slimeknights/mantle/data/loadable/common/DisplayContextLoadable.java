package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.EnumMapLoadable;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map;

/** Special loadable for display contexts due to the Forge weirdness in {@link ItemDisplayContext} */
public enum DisplayContextLoadable implements IdentifierLoadable<ItemDisplayContext> {
  INSTANCE;

  @Override
  public ItemDisplayContext fromKey(Identifier name, String key, TypedMap context) {
    try {
      return ItemDisplayContext.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(name.toString())).getOrThrow();
    } catch (RuntimeException e) {
      throw new JsonSyntaxException("Unable to parse " + key + " as the ItemDisplayContext registry does not contain ID " + name, e);
    }
  }

  @Override
  public Identifier getKey(ItemDisplayContext object) {
    return Identifier.parse(object.getSerializedName());
  }

  @Override
  public ItemDisplayContext decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readEnum(ItemDisplayContext.class);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, ItemDisplayContext value) {
    buffer.writeEnum(value);
  }

  @Override
  public <V> Loadable<Map<ItemDisplayContext,V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(ItemDisplayContext.class, this, valueLoadable, minSize);
  }
}
