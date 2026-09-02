package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Loadable for reading a compound tag from a JSON object. */
public enum CompoundTagLoadable implements RecordLoadable<CompoundTag> {
  INSTANCE;

  @Override
  public CompoundTag deserialize(JsonObject json, TypedMap context) {
    return (CompoundTag)JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
  }

  @Override
  public JsonObject serialize(CompoundTag object) {
    return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, object).getAsJsonObject();
  }

  @Override
  public void serialize(CompoundTag object, JsonObject json) {
    json.entrySet().addAll(serialize(object).entrySet());
  }

  @Override
  public CompoundTag decode(FriendlyByteBuf buffer, TypedMap context) {
    CompoundTag tag = buffer.readNbt();
    if (tag == null) {
      return new CompoundTag();
    }
    return tag;
  }

  @Override
  public void encode(FriendlyByteBuf buffer, CompoundTag object) {
    buffer.writeNbt(object);
  }

  @Override
  public <P> LoadableField<CompoundTag,P> nullableField(String key, Function<P,CompoundTag> getter) {
    return new NullableCompoundTagField<>(this, key, getter);
  }


  /** Compact nullable field implementation using the buffer's nullable compound-tag encoding. */
  private record NullableCompoundTagField<P>(Loadable<CompoundTag> loadable, String key, Function<P,CompoundTag> getter) implements LoadableField<CompoundTag,P> {
    @Nullable
    @Override
    public CompoundTag get(JsonObject json, String key, TypedMap context) {
      return loadable.getOrDefault(json, key, null, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      CompoundTag tag = getter.apply(parent);
      if (tag != null) {
        json.add(key, loadable.serialize(tag));
      }
    }

    @Nullable
    @Override
    public CompoundTag decode(FriendlyByteBuf buffer, TypedMap context) {
      return buffer.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer, P parent) {
      buffer.writeNbt(getter.apply(parent));
    }
  }
}
