package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class representing an item stack output. Supports both direct stacks and tag output, behaving like an ingredient used for output
 */
public abstract class ItemOutput implements Supplier<ItemStack> {
  /* Codecs - just adding these as needed */
  /** Codec for an output that may not be empty with any size */
  public static Codec<ItemOutput> REQUIRED_STACK_CODEC = new LoadableCodec<>(Loadable.REQUIRED_STACK);
  public static final StreamCodec<RegistryFriendlyByteBuf, ItemOutput> STREAM_CODEC =
    ItemStack.OPTIONAL_STREAM_CODEC.map(ItemOutput::fromStack, ItemOutput::get);

  /** Empty instance */
  public static final ItemOutput EMPTY = new OfStack(ItemStack.EMPTY);


  /**
   * Gets the item output of this recipe
   * @return  Item output
   */
  @Override
  public abstract ItemStack get();

  /**
   * Gets a copy of the result stack
   * @return  Item output
   */
  public final ItemStack copy() {
    return get().copy();
  }

  /** Gets the size of the output without resolving the stack */
  public abstract int getCount();

  /** Checks if the contents are empty without resolving the stack */
  public boolean isEmpty() {
    return getCount() <= 0;
  }

  /** Gets the tag for this output. Will be {@code null} if this is not a tag output. */
  @Nullable
  public TagKey<Item> getTag() {
    return null;
  }

  /**
   * Gets the concrete item without resolving an item stack, when this output has one.
   * This is useful during data reload, before item component holders are bound.
   */
  @Nullable
  public Item getItem() {
    return null;
  }

  /**
   * Writes this output to JSON
   * @param  writeCount  If true, serializes the count
   * @return  Json element
   */
  public abstract JsonElement serialize(boolean writeCount);

  /**
   * Creates a new output for the given stack
   * @param stack  Stack
   * @return  Output
   */
  public static ItemOutput fromStack(ItemStack stack) {
    if (stack.isEmpty()) {
      return EMPTY;
    }
    return new OfStack(stack);
  }

  /** Creates an output backed by a template, resolving the stack only when it is used. */
  public static ItemOutput fromTemplate(ItemStackTemplate template) {
    return new OfTemplate(template);
  }

  /**
   * Creates a new output for the given item
   * @param item  Item
   * @param count Stack count
   * @return  Output
   */
  public static ItemOutput fromItem(ItemLike item, int count) {
    return new OfItem(item.asItem(), count);
  }

  /**
   * Creates a new output for the given item
   * @param item  Item
   * @return  Output
   */
  public static ItemOutput fromItem(ItemLike item) {
    return fromItem(item, 1);
  }

  /**
   * Creates a new output for the given tag
   * @param tag   Tag
   * @param count Stack count
   * @param nbt   Stack NBT
   * @return Output
   */
  public static ItemOutput fromTag(TagKey<Item> tag, int count, @Nullable CompoundTag nbt) {
    return new OfTagPreference(tag, count, nbt);
  }

  /**
   * Creates a new output for the given tag
   * @param tag   Tag
   * @param count Stack count
   * @return Output
   */
  public static ItemOutput fromTag(TagKey<Item> tag, int count) {
    return fromTag(tag, count, null);
  }

  /**
   * Creates a new output for the given tag
   * @param tag  Tag
   * @return Output
   */
  public static ItemOutput fromTag(TagKey<Item> tag) {
    return fromTag(tag, 1);
  }

  /**
   * Writes this output to the packet buffer
   * @param buffer  Packet buffer instance
   */
  public void write(RegistryFriendlyByteBuf buffer) {
    STREAM_CODEC.encode(buffer, this);
  }

  /**
   * Reads an item output from the packet buffer
   * @param buffer  Buffer instance
   * @return  Item output
   */
  public static ItemOutput read(RegistryFriendlyByteBuf buffer) {
    return STREAM_CODEC.decode(buffer);
  }

  /** Class for an output that is just an item, simplifies NBT for serializing as vanilla forces NBT to be set for tools and forge goes through extra steps when NBT is set */
  private static class OfItem extends ItemOutput {
    private final Item item;
    private final int count;
    private ItemStack cachedStack;

    private OfItem(Item item, int count) {
      this.item = item;
      this.count = count;
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public Item getItem() {
      return item;
    }

    @Override
    public ItemStack get() {
      if (cachedStack == null) {
        cachedStack = new ItemStack(item, count);
      }
      return cachedStack;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      JsonElement item = Loadables.ITEM.serialize(this.item);
      if (writeCount && count > 1) {
        JsonObject json = new JsonObject();
        json.add("item", item);
        json.addProperty("count", count);
        return json;
      } else {
        return item;
      }
    }
  }

  /** Class for an output that is just a stack */
  private static class OfStack extends ItemOutput {
    private final ItemStack stack;

    private OfStack(ItemStack stack) {
      this.stack = stack;
    }

    @Override
    public ItemStack get() {
      return stack;
    }

    @Override
    public int getCount() {
      return stack.getCount();
    }

    @Override
    public Item getItem() {
      return stack.getItem();
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      if (writeCount) {
        return ItemStackLoadable.OPTIONAL_STACK_NBT.serialize(stack);
      }
      return ItemStackLoadable.OPTIONAL_ITEM_NBT.serialize(stack);
    }
  }

  /** Output backed by a 26.1 item stack template, avoiding early component access during reload. */
  private static class OfTemplate extends ItemOutput {
    private final ItemStackTemplate template;
    private ItemStack cachedStack;

    private OfTemplate(ItemStackTemplate template) {
      this.template = template;
    }

    @Override
    public ItemStack get() {
      if (cachedStack == null) {
        cachedStack = template.create();
      }
      return cachedStack;
    }

    @Override
    public int getCount() {
      return template.count();
    }

    @Override
    public Item getItem() {
      return template.item().value();
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      JsonElement item = Loadables.ITEM.serialize(template.item().value());
      CompoundTag nbt = template.components().getPatch(DataComponents.CUSTOM_DATA) == null
        ? null
        : template.components().getPatch(DataComponents.CUSTOM_DATA).flatMap(data -> Optional.of(data.copyTag())).orElse(null);
      if (!writeCount && nbt == null) {
        return item;
      }
      JsonObject json = new JsonObject();
      json.add("item", item);
      if (writeCount && template.count() > 1) {
        json.addProperty("count", template.count());
      }
      if (nbt != null) {
        json.add("nbt", NBTLoadable.ALLOW_STRING.serialize(nbt));
      }
      return json;
    }
  }

  /** Class for an output from a tag preference */
  private static class OfTagPreference extends ItemOutput {
    private final TagKey<Item> tag;
    private final int count;
    @Nullable
    private final CompoundTag nbt;
    private ItemStack cachedResult = null;

    private OfTagPreference(TagKey<Item> tag, int count, @Nullable CompoundTag nbt) {
      this.tag = tag;
      this.count = count;
      this.nbt = nbt;
    }

    @Override
    public TagKey<Item> getTag() {
      return tag;
    }

    @Override
    public int getCount() {
      return count;
    }

    @Override
    public ItemStack get() {
      // cache the result from the tag preference to save effort, especially helpful if the tag becomes invalid
      // this object should only exist in recipes so no need to invalidate the cache
      if (cachedResult == null) {
        // if the preference is empty, do not cache it.
        // This should only happen if someone scans recipes before tag are computed in which case we cache the wrong result.
        // We protect against empty tags in our recipes via conditions.
        Optional<Item> preference = TagPreference.getPreference(tag);
        if (preference.isEmpty()) {
          return ItemStack.EMPTY;
        }
        cachedResult = new ItemStack(preference.orElseThrow(), count);
        if (nbt != null) {
          cachedResult.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
        }
      }
      return cachedResult;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      JsonObject json = new JsonObject();
      if (!writeCount || count > 0) {
        json.addProperty("tag", tag.location().toString());
      }
      if (writeCount) {
        json.addProperty("count", count);
      }
      if (count > 0 && nbt != null) {
        json.add("nbt", NBTLoadable.ALLOW_STRING.serialize(nbt));
      }
      return json;
    }
  }

  /** Loadable logic for an ItemOutput */
  public enum Loadable implements RecordLoadable<ItemOutput> {
    /** Loadable for an output that may be empty with a fixed size of 1 */
    OPTIONAL_ITEM(false, false),
    /** Loadable for an output that may be empty with any size */
    OPTIONAL_STACK(false, true),
    /** Loadable for an output that may not empty with a fixed size of 1 */
    REQUIRED_ITEM(true, false),
    /** Loadable for an output that may not be empty with any size */
    REQUIRED_STACK(true, true);

    private final boolean nonEmpty;
    private final boolean readCount;
    private final RecordLoadable<ItemStackTemplate> stack;
    Loadable(boolean nonEmpty, boolean readCount) {
      this.nonEmpty = nonEmpty;
      this.readCount = readCount;
      // figure out the stack serializer to use based on the two parameters
      // we always do NBT, just those that vary
      if (nonEmpty) {
        this.stack = readCount ? ItemStackTemplateLoadable.STACK_NBT : ItemStackTemplateLoadable.ITEM_NBT;
      } else {
        this.stack = readCount ? ItemStackTemplateLoadable.STACK_NBT : ItemStackTemplateLoadable.ITEM_NBT;
      }
    }

    @Override
    public ItemOutput deserialize(JsonObject json, TypedMap context) {
      if (json.has("tag")) {
        TagKey<Item> tag = Loadables.ITEM_TAG.getIfPresent(json, "tag", context);
        int count = 1;
        // 0 count field means we load count from JSON
        if (readCount) {
          count = IntLoadable.FROM_ONE.getOrDefault(json, "count", 1, context);
        }
        return fromTag(tag, count, NBTLoadable.ALLOW_STRING.getOrDefault(json, "nbt", null));
      }
      return fromTemplate(stack.deserialize(json, context));
    }

    @Override
    public ItemOutput convert(JsonElement element, String key, TypedMap context) {
      // if it's a primitive, parse it directly with the stack logic
      // that handles single items and ensures both count and non-empty
      if (element.isJsonPrimitive()) {
        return fromTemplate(stack.convert(element, key, context));
      }
      return deserialize(GsonHelper.convertToJsonObject(element, key), context);
    }

    @Override
    public void serialize(ItemOutput object, JsonObject json) {
      JsonElement element = serialize(object);
      if (element.isJsonObject()) {
        for (Entry<String,JsonElement> entry : element.getAsJsonObject().entrySet()) {
          json.add(entry.getKey(), entry.getValue());
        }
      } else {
        // if its a primitive, it must be the item field, so add that directly
        json.add("item", element);
      }
    }

    @Override
    public JsonElement serialize(ItemOutput output) {
      if (nonEmpty && output.isEmpty()) {
        throw new IllegalArgumentException("ItemOutput cannot be empty for this recipe");
      }
      return output.serialize(readCount);
    }

    @Override
    public ItemOutput decode(FriendlyByteBuf buffer, TypedMap context) {
      // Optional recipe outputs are encoded with an explicit presence bit.
      // ItemStackTemplate intentionally cannot represent an empty stack, but
      // optional leftovers and child outputs are allowed to be empty.
      if (!nonEmpty && !buffer.readBoolean()) {
        return EMPTY;
      }
      return fromTemplate(stack.decode(buffer, context));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, ItemOutput object) {
      if (!nonEmpty) {
        boolean present = !object.isEmpty();
        buffer.writeBoolean(present);
        if (!present) {
          return;
        }
      }
      if (object instanceof OfTemplate template) {
        stack.encode(buffer, template.template);
      } else {
        stack.encode(buffer, ItemStackTemplate.fromNonEmptyStack(object.get()));
      }
    }


    /* Defaulting behavior */

    /** Gets the output, defaulting to empty. Note this will not stop you from getting empty with a non-empty loadable, thats on you for weirdly calling. */
    public ItemOutput getOrEmpty(JsonObject parent, String key) {
      return getOrDefault(parent, key, ItemOutput.EMPTY);
    }

    /** Creates a field defaulting to empty */
    public <P> LoadableField<ItemOutput,P> emptyField(String key, boolean serializeDefault, Function<P,ItemOutput> getter) {
      return defaultField(key, ItemOutput.EMPTY, serializeDefault, getter);
    }

    /** Creates a field defaulting to empty that does not serialize if empty */
    public <P> LoadableField<ItemOutput,P> emptyField(String key, Function<P,ItemOutput> getter) {
      return emptyField(key, false, getter);
    }
  }
}
