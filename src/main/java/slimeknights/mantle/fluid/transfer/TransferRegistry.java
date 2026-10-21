package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.util.typed.TypedMap;

/** Registry-aware transfer loading with a bridge for existing Gson registrations. */
public final class TransferRegistry extends GenericLoaderRegistry<IFluidContainerTransfer> {
  static final RecordLoadable<IFluidContainerTransfer> LEGACY = new RecordLoadable<>() {
    @Override public IFluidContainerTransfer deserialize(JsonObject json, TypedMap context) {
      try (var ignored = IngredientLoadable.pushOps(ContextKey.optionalRegistryOps(context))) {
        return FluidContainerTransferManager.GSON.fromJson(json, IFluidContainerTransfer.class);
      }
    }
    @Override public void serialize(IFluidContainerTransfer value, JsonObject json, TypedMap context) {
      try (var ignored = IngredientLoadable.pushOps(ContextKey.optionalRegistryOps(context))) {
        FluidContainerTransferManager.GSON.toJsonTree(value, IFluidContainerTransfer.class).getAsJsonObject().entrySet()
          .forEach(entry -> json.add(entry.getKey(), entry.getValue()));
      }
    }
    @Override public IFluidContainerTransfer decode(FriendlyByteBuf buffer, TypedMap context) {
      return deserialize(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), context);
    }
    @Override public void encode(FriendlyByteBuf buffer, IFluidContainerTransfer value, TypedMap context) {
      var json = new JsonObject(); serialize(value,json,context); buffer.writeUtf(json.toString());
    }
  };
  private boolean builtInsRegistered;
  TransferRegistry() {
    super("Fluid Container Transfer", false);
    register(Mantle.getResource("legacy_transfer"), LEGACY);
  }
  private synchronized void builtIns() {
    if (!builtInsRegistered) {
      register(EmptyFluidContainerTransfer.ID, EmptyFluidContainerTransfer.LOADER);
      register(FillFluidContainerTransfer.ID, FillFluidContainerTransfer.LOADER);
      register(EmptyFluidCopyDataTransfer.ID, EmptyFluidCopyDataTransfer.LOADER);
      register(FillFluidCopyDataTransfer.ID, FillFluidCopyDataTransfer.LOADER);
      register(EmptyPotionTransfer.ID, EmptyPotionTransfer.DESERIALIZER);
      builtInsRegistered = true;
    }
  }
  @Override public IFluidContainerTransfer deserialize(JsonObject json, TypedMap context) {
    builtIns();
    if (json.has("type") && loaders.getValue(Identifier.parse(json.get("type").getAsString())) != null) return super.deserialize(json,context);
    return LEGACY.deserialize(json,context);
  }
  @Override public IFluidContainerTransfer convert(JsonElement element, String key, TypedMap context) {
    if (!element.isJsonObject()) throw new com.google.gson.JsonSyntaxException("Expected fluid transfer object at " + key);
    return deserialize(element.getAsJsonObject(),context);
  }
  @Override public void serialize(IFluidContainerTransfer value, JsonObject json, TypedMap context) {
    builtIns();
    if (value.getLoader() == LEGACY) LEGACY.serialize(value,json,context);
    else super.serialize(value,json,context);
  }
  @Override public JsonElement serialize(IFluidContainerTransfer value, TypedMap context) {
    var json = new JsonObject(); serialize(value,json,context); return json;
  }
  @Override public void encode(FriendlyByteBuf buffer, IFluidContainerTransfer value, TypedMap context) {
    builtIns(); super.encode(buffer,value,context);
  }
  @Override public IFluidContainerTransfer decode(FriendlyByteBuf buffer, TypedMap context) {
    builtIns(); return super.decode(buffer,context);
  }
}
