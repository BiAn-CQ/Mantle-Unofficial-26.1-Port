package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;
import java.util.function.Function;

/**
 * Loadable for an item stack template.
 *
 * <p>Unlike {@link ItemStackLoadable}, this class never constructs an
 * {@code ItemStack} while decoding JSON.  This is required on 26.1, where
 * item default components are bound only after the datapack registries have
 * finished loading.</p>
 */
public final class ItemStackTemplateLoadable {
  private ItemStackTemplateLoadable() {}

  private static final Function<ItemStackTemplate,Item> ITEM_GETTER = template -> template.item().value();
  private static final LoadableField<Item,ItemStackTemplate> ITEM_FIELD = Loadables.ITEM.defaultField("item", Items.AIR, false, ITEM_GETTER);
  private static final LoadableField<Integer,ItemStackTemplate> COUNT = IntLoadable.FROM_ZERO.defaultField("count", 1, true, ItemStackTemplate::count);
  private static final LoadableField<CompoundTag,ItemStackTemplate> NBT = NBTLoadable.ALLOW_STRING.nullableField("nbt", ItemStackTemplateLoadable::getCustomData);

  /** Stack with a count of one and no custom data. */
  public static final Loadable<ItemStackTemplate> ITEM = Loadables.ITEM.flatXmap(
    item -> makeTemplate(item, 1, null), ITEM_GETTER);
  /** Stack with a variable count and no custom data. */
  public static final RecordLoadable<ItemStackTemplate> STACK = RecordLoadable.create(
    ITEM_FIELD, COUNT, (item, count) -> makeTemplate(item, count, null));
  /** Stack with custom data and a fixed count of one. */
  public static final RecordLoadable<ItemStackTemplate> ITEM_NBT = NBTStack.FIXED_COUNT;
  /** Stack with custom data and a variable count. */
  public static final RecordLoadable<ItemStackTemplate> STACK_NBT = NBTStack.READ_COUNT;

  private static ItemStackTemplate makeTemplate(Item item, int count, CompoundTag nbt) {
    if (item == Items.AIR || count == 0) {
      throw new IllegalArgumentException("ItemStackTemplate cannot be empty");
    }
    DataComponentPatch patch = DataComponentPatch.EMPTY;
    if (nbt != null) {
      patch = DataComponentPatch.builder()
        .set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()))
        .build();
    }
    return new ItemStackTemplate(item, count, patch);
  }

  private static CompoundTag getCustomData(ItemStackTemplate template) {
    Optional<CustomData> customData = template.components().getPatch(DataComponents.CUSTOM_DATA);
    return customData == null || customData.isEmpty() ? null : customData.orElseThrow().copyTag();
  }

  /** Loadable for the legacy NBT/count representation. */
  private enum NBTStack implements RecordLoadable<ItemStackTemplate> {
    /** Reads count from JSON. */
    READ_COUNT,
    /** Keeps the count at one. */
    FIXED_COUNT;

    @Override
    public ItemStackTemplate deserialize(JsonObject json, TypedMap context) {
      int count = this == READ_COUNT ? COUNT.get(json, context) : 1;
      return makeTemplate(ITEM_FIELD.get(json, context), count, NBT.get(json, context));
    }

    @Override
    public void serialize(ItemStackTemplate template, JsonObject json) {
      ITEM_FIELD.serialize(template, json);
      if (this == READ_COUNT) {
        COUNT.serialize(template, json);
      }
      NBT.serialize(template, json);
    }

    @Override
    public ItemStackTemplate convert(JsonElement element, String key, TypedMap context) {
      if (element.isJsonPrimitive()) {
        return ITEM.convert(element, key, context);
      }
      return RecordLoadable.super.convert(element, key, context);
    }

    @Override
    public JsonElement serialize(ItemStackTemplate template) {
      if ((this == FIXED_COUNT || template.count() == 1) && getCustomData(template) == null) {
        return ITEM.serialize(template);
      }
      return RecordLoadable.super.serialize(template);
    }

    @Override
    public ItemStackTemplate decode(net.minecraft.network.FriendlyByteBuf buffer, TypedMap context) {
      Item item = ITEM_FIELD.decode(buffer, context);
      int count = this == READ_COUNT ? COUNT.decode(buffer, context) : 1;
      return makeTemplate(item, count, buffer.readNbt());
    }

    @Override
    public void encode(net.minecraft.network.FriendlyByteBuf buffer, ItemStackTemplate template) {
      ITEM_FIELD.encode(buffer, template);
      if (this == READ_COUNT) {
        COUNT.encode(buffer, template);
      }
      buffer.writeNbt(getCustomData(template));
    }
  }
}
