package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable backed by native registry-aware JSON and network codecs. */
public record RegistryCodecLoadable<T>(Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf,T> streamCodec) implements Loadable<T> {
  private static final DynamicOps<JsonElement> JSON_OPS = RegistryOps.create(
    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return codec.parse(JSON_OPS, element).getOrThrow(message -> new JsonParseException("Invalid " + key + ": " + message));
  }

  @Override
  public JsonElement serialize(T object) {
    return codec.encodeStart(JSON_OPS, object).getOrThrow(JsonParseException::new);
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      return streamCodec.decode(registryBuffer);
    }
    throw new IllegalArgumentException("Registry-aware value requires RegistryFriendlyByteBuf");
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      streamCodec.encode(registryBuffer, object);
      return;
    }
    throw new IllegalArgumentException("Registry-aware value requires RegistryFriendlyByteBuf");
  }
}
