package slimeknights.mantle.registration.deferred;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred register that registers items with wrappers
 */
@SuppressWarnings("unused")
public class ItemDeferredRegister extends DeferredRegisterWrapper<Item> {

  /** Registry key of the item supplier currently being evaluated. */
  private static final ThreadLocal<ResourceKey<Item>> CURRENT_ITEM_ID = new ThreadLocal<>();

  public ItemDeferredRegister(String modID) {
    super(net.neoforged.neoforge.registries.DeferredRegister.createItems(modID), modID);
  }

  /** Gets the item key for the supplier currently being evaluated. */
  public static ResourceKey<Item> currentItemId() {
    return CURRENT_ITEM_ID.get();
  }

  /** Binds a property object to the item currently being constructed. */
  public static Item.Properties bindItemProperties(Item.Properties properties) {
    ResourceKey<Item> itemId = CURRENT_ITEM_ID.get();
    return itemId == null ? properties : properties.setId(itemId);
  }

  static ResourceKey<Item> itemKey(String modID, String name) {
    return ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(modID, name));
  }

  public static <I extends Item> Supplier<I> withCurrentItemId(String modID, String name, Supplier<? extends I> item) {
    return () -> {
      ResourceKey<Item> previous = CURRENT_ITEM_ID.get();
      CURRENT_ITEM_ID.set(itemKey(modID, name));
      try {
        return item.get();
      } finally {
        if (previous == null) {
          CURRENT_ITEM_ID.remove();
        } else {
          CURRENT_ITEM_ID.set(previous);
        }
      }
    };
  }

  /**
   * Adds a new item to the list to be registered, using the given supplier
   * @param name   Item name
   * @param sup    Supplier returning an item
   * @return  Item registry object
   */
  public <I extends Item> ItemObject<I> register(String name, Supplier<? extends I> sup) {
    return new ItemObject<>(register.register(name, withCurrentItemId(modID, name, sup)));
  }

  /**
   * Adds a new item to the list to be registered, based on the given item properties
   * @param name   Item name
   * @param props  Item properties
   * @return  Item registry object
   */
  public ItemObject<Item> register(String name, Item.Properties props) {
    return register(name, () -> new Item(props.setId(itemKey(modID, name))));
  }

  /**
   * Adds a new item to the list to be registered, with default item properties
   * @param name   Item name
   * @return  Item registry object
   */
  public ItemObject<Item> register(String name) {
    return register(name, new Item.Properties());
  }


  /* Specialty */

  /**
   * Registers an item with multiple variants, prefixing the name with the value name
   * @param values   Enum values to use for this item
   * @param name     Name of the block
   * @param mapper   Function to get a item for the given enum value
   * @return  EnumObject mapping between different item types
   */
  public <T extends Enum<T>, I extends Item> EnumObject<T,I> registerEnum(T[] values, String name, Function<T,? extends I> mapper) {
    return registerEnum(values, name, (fullName, type) -> register(fullName, () -> mapper.apply(type)));
  }

  /**
   * Registers an item with multiple variants, suffixing the name with the value name
   * @param values   Enum values to use for this item
   * @param name     Name of the block
   * @param mapper   Function to get a item for the given enum value
   * @return  EnumObject mapping between different item types
   */
  public <T extends Enum<T>, I extends Item> EnumObject<T,I> registerEnum(String name, T[] values, Function<T,? extends I> mapper) {
    return registerEnum(name, values, (fullName, type) -> register(fullName, () -> mapper.apply(type)));
  }
}
