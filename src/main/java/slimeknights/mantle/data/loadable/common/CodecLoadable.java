package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import javax.annotation.Nullable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;

/** Implementation of a loadable using a codec. Note this will be inefficient when reading from and writing to the network */
public record CodecLoadable<T>(DynamicOps<Tag> ops, Codec<T> codec) implements Loadable<T> {
  public CodecLoadable(Codec<T> codec) {
    this(NbtOps.INSTANCE, codec);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return codec.parse(ContextKey.optionalRegistryOps(context), element).getOrThrow();
  }

  @Override
  public JsonElement serialize(T object) {
    return serialize(object, ContextKey.currentContext());
  }

  @Override
  public JsonElement serialize(T object, TypedMap context) {
    return codec.encodeStart(ContextKey.optionalRegistryOps(context), object).getOrThrow();
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readWithCodecTrusted(ContextKey.withRegistryContext(ops, context), codec);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    encode(buffer, object, TypedMap.EMPTY);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object, TypedMap context) {
    buffer.writeWithCodec(ContextKey.withRegistryContext(ops, context), codec, object);
  }

  public interface Native<T> extends Loadable<T> {

  /** Codec used for JSON. */
  Codec<T> codec();

  /** Stream codec used for the buffer. If {@code null}, uses {@link #codec()} instead. */
  @Nullable
  StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();

  /** Gets the ops for the given context */
  DynamicOps<JsonElement> getOps(TypedMap context, ErrorFactory error);

  @Override
  default T convert(JsonElement element, String key, TypedMap context) {
    return JsonHelper.parse(getOps(context, ErrorFactory.JSON_SYNTAX_ERROR), codec(), element);
  }

  @Override
  default JsonElement serialize(T object, TypedMap context) {
    return JsonHelper.serialize(getOps(context, ErrorFactory.RUNTIME), codec(), object);
  }

  @Override
  default T decode(net.minecraft.network.FriendlyByteBuf buffer, TypedMap context) {
    StreamCodec<? super RegistryFriendlyByteBuf, T> codec = streamCodec();
    if (codec == null) {
      throw new IllegalStateException("The codec " + codec() + " does not support decoding from network.");
    }
    return codec.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
  }

  @Override
  default void encode(net.minecraft.network.FriendlyByteBuf buffer, T object, TypedMap context) {
    StreamCodec<? super RegistryFriendlyByteBuf, T> codec = streamCodec();
    if (codec == null) {
      throw new IllegalStateException("The codec " + codec() + " does not support encoding to network.");
    }
    codec.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, object);
  }


  }
  /** Parses the value directly without registry access */
  @SuppressWarnings("unused")  // API
  public record Direct<T>(Codec<T> codec, @Nullable StreamCodec<? super RegistryFriendlyByteBuf,T> streamCodec) implements Native<T> {
    /** Uses {@link ByteBufCodecs#fromCodec(Codec)} for network. */
    public Direct(Codec<T> codec) {
      this(codec, ByteBufCodecs.fromCodec(codec));
    }

    @Override
    public DynamicOps<JsonElement> getOps(TypedMap context, ErrorFactory error) {
      return ContextKey.optionalRegistryOps(context);
    }
  }

  /** Uses registry access to parse the object. */
  public record Registry<T>(Codec<T> codec, @Nullable StreamCodec<? super RegistryFriendlyByteBuf,T> streamCodec) implements Native<T> {
    /** Uses {@link ByteBufCodecs#fromCodec(Codec)} for network. */
    public Registry(Codec<T> codec) {
      this(codec, ByteBufCodecs.fromCodecWithRegistries(codec));
    }

    @Override
    public DynamicOps<JsonElement> getOps(TypedMap context, ErrorFactory error) {
      return ContextKey.createRegistryOps(context, error);
    }
  }

}
