package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.data.loadable.common.RegistryCodecLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class representing an item stack output. Supports both direct stacks and tag output, behaving like an ingredient used for output
 */
public abstract class ItemOutput extends StackOutput<Item,ItemStack> {
  /* Codecs - just adding these as needed */
  /** Codec for an output that may not be empty with any size */
  public static Codec<ItemOutput> REQUIRED_STACK_CODEC = new LoadableCodec<>(Loadable.REQUIRED_STACK);
  private static final slimeknights.mantle.data.loadable.Loadable<DataComponentPatch> COMPONENTS =
    new RegistryCodecLoadable<>(DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
  public static final StreamCodec<RegistryFriendlyByteBuf, ItemOutput> STREAM_CODEC =
    ItemStack.OPTIONAL_STREAM_CODEC.map(ItemOutput::fromStack, ItemOutput::get);

  public static final Codec<ItemOutput> REQUIRED_ITEM_CODEC = new LoadableCodec<>(Loadable.REQUIRED_ITEM);
  public static final Codec<ItemOutput> OPTIONAL_ITEM_CODEC = new LoadableCodec<>(Loadable.OPTIONAL_ITEM);
  public static final Codec<ItemOutput> OPTIONAL_STACK_CODEC = new LoadableCodec<>(Loadable.OPTIONAL_STACK);

  @Override public DataComponentPatch getComponents() {
    if (this instanceof OfTemplate value) { return value.template.components(); }
    if (this instanceof OfTagPreference value) { return value.components; }
    if (this instanceof OfStack value) { return value.stack.getComponentsPatch(); }
    return DataComponentPatch.EMPTY;
  }
  @Override public com.mojang.datafixers.util.Either<Item,TagKey<Item>> getValueOrTag() {
    TagKey<Item> tag = getTag();
    return tag == null ? com.mojang.datafixers.util.Either.left(getItem()) : com.mojang.datafixers.util.Either.right(tag);
  }

  public static final slimeknights.mantle.data.loadable.Loadable<ItemOutput> OPTIONAL_ITEM = Loadable.OPTIONAL_ITEM;
  public static final slimeknights.mantle.data.loadable.Loadable<ItemOutput> REQUIRED_ITEM = Loadable.REQUIRED_ITEM;
  public static final slimeknights.mantle.data.loadable.Loadable<ItemOutput> OPTIONAL_STACK = Loadable.OPTIONAL_STACK;
  public static final slimeknights.mantle.data.loadable.Loadable<ItemOutput> REQUIRED_STACK = Loadable.REQUIRED_STACK;

  public static final RecordLoadable<ItemOutput> REQUIRED_ITEM_LOADABLE = Loadable.REQUIRED_ITEM;
  public static final RecordLoadable<ItemOutput> OPTIONAL_ITEM_LOADABLE = Loadable.OPTIONAL_ITEM;
  public static final RecordLoadable<ItemOutput> REQUIRED_STACK_LOADABLE = Loadable.REQUIRED_STACK;
  public static final RecordLoadable<ItemOutput> OPTIONAL_STACK_LOADABLE = Loadable.OPTIONAL_STACK;
  public static final StreamCodec<RegistryFriendlyByteBuf,ItemOutput> REQUIRED_STREAM_CODEC = ItemStack.STREAM_CODEC.map(ItemOutput::fromStack,ItemOutput::get);
  public static final StreamCodec<RegistryFriendlyByteBuf,ItemOutput> OPTIONAL_STREAM_CODEC = STREAM_CODEC;

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

  public JsonElement serialize(boolean writeCount, TypedMap context) {
    return serialize(writeCount);
  }

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
   * @param components Stack components
   * @return Output
   */
  public static ItemOutput fromTag(TagKey<Item> tag, int count, DataComponentPatch components) {
    return new OfTagPreference(tag, count, components);
  }

  /**
   * Creates a new output for the given tag
   * @param tag   Tag
   * @param count Stack count
   * @return Output
   */
  public static ItemOutput fromTag(TagKey<Item> tag, int count) {
    return fromTag(tag, count, DataComponentPatch.EMPTY);
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
      return serialize(writeCount, ContextKey.currentContext());
    }

    public JsonElement serialize(boolean writeCount, TypedMap context) {
      return ItemStackTemplateLoadable.STACK.serialize(
        new ItemStackTemplate(item, writeCount ? count : 1, DataComponentPatch.EMPTY), context);
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
      return serialize(writeCount, ContextKey.currentContext());
    }

    public JsonElement serialize(boolean writeCount, TypedMap context) {
      if (stack.isEmpty()) {
        JsonObject empty = new JsonObject();
        if (writeCount) empty.addProperty("count",0);
        return empty;
      }
      return ItemStackTemplateLoadable.STACK.serialize(
        new ItemStackTemplate(stack.getItem(), writeCount ? stack.getCount() : 1, stack.getComponentsPatch()), context);
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
      return serialize(writeCount, ContextKey.currentContext());
    }

    public JsonElement serialize(boolean writeCount, TypedMap context) {
      return ItemStackTemplateLoadable.STACK.serialize(writeCount ? template : template.withCount(1), context);
    }
  }

  /** Class for an output from a tag preference */
  private static class OfTagPreference extends ItemOutput {
    private final TagKey<Item> tag;
    private final int count;
    private final DataComponentPatch components;
    private ItemStack cachedResult = null;

    private OfTagPreference(TagKey<Item> tag, int count, DataComponentPatch components) {
      this.tag = tag;
      this.count = count;
      this.components = components;
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
        cachedResult = new ItemStackTemplate(preference.orElseThrow(), count, components).create();
      }
      return cachedResult;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      return serialize(writeCount, ContextKey.currentContext());
    }

    public JsonElement serialize(boolean writeCount, TypedMap context) {
      JsonObject json = new JsonObject();
      if (!writeCount || count > 0) {
        json.addProperty("tag", tag.location().toString());
      }
      if (writeCount) {
        json.addProperty("count", count);
      }
      if (count > 0 && !components.isEmpty()) {
        json.add("components", COMPONENTS.serialize(components, context));
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
    private final slimeknights.mantle.data.loadable.Loadable<ItemStackTemplate> stack;
    Loadable(boolean nonEmpty, boolean readCount) {
      this.nonEmpty = nonEmpty;
      this.readCount = readCount;
      this.stack = ItemStackTemplateLoadable.STACK;
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
        return fromTag(tag, count, COMPONENTS.getOrDefault(json, "components", DataComponentPatch.EMPTY, context));
      }
      if (!nonEmpty && ((!json.has("id") && !json.has("item"))
          || json.has("count") && json.get("count").getAsInt() == 0
          || json.has("id") && json.get("id").getAsString().equals("minecraft:air")
          || json.has("item") && json.get("item").getAsString().equals("minecraft:air"))) {
        return EMPTY;
      }
      if (json.has("item") && !json.has("id")) {
        json = json.deepCopy();
        json.add("id", json.remove("item"));
      }
      ItemStackTemplate template = stack.convert(json, "item output", context);
      if (!readCount && template.count() != 1) {
        throw new IllegalArgumentException("Item output for this recipe must have a count of 1");
      }
      return fromTemplate(template);
    }

    @Override
    public void serialize(ItemOutput object, JsonObject json) {
      serialize(object, json, TypedMap.EMPTY);
    }

    public void serialize(ItemOutput object, JsonObject json, TypedMap context) {
      serialize(object, context).getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
    }

    @Override
    public JsonElement serialize(ItemOutput output) {
      return serialize(output, TypedMap.EMPTY);
    }

    public JsonElement serialize(ItemOutput output, TypedMap context) {
      if (nonEmpty && output.isEmpty()) {
        throw new IllegalArgumentException("ItemOutput cannot be empty for this recipe");
      }
      return output.serialize(readCount, context);
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
      encode(buffer, object, TypedMap.EMPTY);
    }

    public void encode(FriendlyByteBuf buffer, ItemOutput object, TypedMap context) {
      if (!nonEmpty) {
        boolean present = !object.isEmpty();
        buffer.writeBoolean(present);
        if (!present) {
          return;
        }
      }
      if (object instanceof OfTemplate template) {
        stack.encode(buffer, template.template, context);
      } else {
        stack.encode(buffer, ItemStackTemplate.fromNonEmptyStack(object.get()), context);
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
