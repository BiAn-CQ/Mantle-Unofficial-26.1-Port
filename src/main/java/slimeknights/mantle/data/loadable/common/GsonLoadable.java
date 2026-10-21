package slimeknights.mantle.data.loadable.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Simple loadable mapping GSON to loadable. Uses NBT for networking */
public record GsonLoadable<T>(Gson gson, Class<T> classType) implements Loadable<T> {
  @Override
  public T convert(JsonElement json, String s, TypedMap context) {
    return gson.fromJson(json, classType);
  }

  @Override
  public JsonElement serialize(T object) {
    return gson.toJsonTree(object, classType);
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    Tag tag = ByteBufCodecs.TAG.decode(buffer);
    return gson.fromJson(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag), classType);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, gson.toJsonTree(object, classType));
    ByteBufCodecs.TAG.encode(buffer, tag);
  }
}
