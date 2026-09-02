package slimeknights.mantle.client.book.data.element;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringUtil;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class IngredientData implements IDataElement {
  private static final DynamicOps<JsonElement> JSON_OPS = RegistryOps.create(
    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

  public SizedIngredient[] ingredients = new SizedIngredient[0];
  public String action;

  private transient String error;
  private transient NonNullList<ItemStack> items;
  private transient boolean customData;

  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  public static IngredientData getItemStackData(ItemStack stack) {
    IngredientData data = new IngredientData();
    data.items = NonNullList.withSize(1, stack);
    data.customData = true;

    return data;
  }

  public static IngredientData getItemStackData(NonNullList<ItemStack> items) {
    IngredientData data = new IngredientData();
    data.items = items;
    data.customData = true;

    return data;
  }

  @Override
  public void load(BookRepository source) {
    if (this.customData) {
      return;
    }

    ArrayList<ItemStack> stacks = new ArrayList<>();
    for(SizedIngredient ingredient : ingredients) {
      if(ingredient == null) {
        continue;
      }

      stacks.addAll(ingredient.getMatchingStacks());
    }

    if(ingredients == null || stacks.isEmpty() || !StringUtil.isNullOrEmpty(error)) {
      items = NonNullList.withSize(1, getMissingItem());
      return;
    }

    items = NonNullList.of(getMissingItem(), stacks.toArray(new ItemStack[0]));
  }

  private ItemStack getMissingItem() {
    return getMissingItem(this.error);
  }

  private ItemStack getMissingItem(String error) {
    ItemStack missingItem = new ItemStack(Items.BARRIER);

    List<Component> lore = new ArrayList<>();
    if(!StringUtil.isNullOrEmpty(error)) {
      lore.add(Component.literal("\u00A7r\u00A7eError:"));
      lore.add(Component.literal("\u00A7r\u00A7e" + error));
    }
    missingItem.set(DataComponents.CUSTOM_NAME, Component.literal("\u00A7rError Loading Item"));
    missingItem.set(DataComponents.LORE, new ItemLore(lore));

    return missingItem;
  }

  public static class Deserializer implements JsonDeserializer<IngredientData> {
    @Override
    public IngredientData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      IngredientData data = new IngredientData();

      if(json.isJsonArray()) {
        JsonArray array = json.getAsJsonArray();
        data.ingredients = new SizedIngredient[array.size()];
        ArrayList<ItemStack> expandedItems = new ArrayList<>();
        boolean hasStackData = false;

        for(int i = 0; i < array.size(); i++) {
          try {
            JsonElement element = array.get(i);
            if (isStackData(element)) {
              hasStackData = true;
              expandedItems.add(readStack(element));
            } else {
              data.ingredients[i] = readIngredient(element);
              expandedItems.addAll(data.ingredients[i].getMatchingStacks());
            }
          } catch (Exception e) {
            data.ingredients[i] = SizedIngredient.of(Ingredient.of(data.getMissingItem(e.getMessage()).getItem()));
            expandedItems.add(data.getMissingItem(e.getMessage()));
          }
        }

        // Ingredient cannot retain item components. Preserve explicit native
        // stack entries directly while expanding ordinary ingredients beside them.
        if (hasStackData) {
          data.customData = true;
          data.items = expandedItems.isEmpty()
                       ? NonNullList.withSize(1, data.getMissingItem())
                       : NonNullList.of(data.getMissingItem(), expandedItems.toArray(new ItemStack[0]));
          }

        return data;
      }

      if (isStackData(json)) {
        try {
          data.items = NonNullList.withSize(1, readStack(json));
          data.customData = true;
        } catch (Exception e) {
          data.error = e.getMessage();
        }
        return data;
      }

      try {
        data.ingredients = new SizedIngredient[]{ readIngredient(json) };
      } catch (Exception e) {
        data.error = e.getMessage();
        return data;
      }

      if(json.isJsonObject()) {
        JsonObject object = json.getAsJsonObject();
        if (object.has("action")) {
          JsonElement action = object.get("action");
          if (action.isJsonPrimitive()) {
            JsonPrimitive primitive = action.getAsJsonPrimitive();
            if (primitive.isString()) {
              data.action = primitive.getAsString();
            }
          }
        }
      }

      return data;
    }

    private static boolean isStackData(JsonElement json) {
      return json.isJsonObject() && json.getAsJsonObject().has("id");
    }

    private static ItemStack readStack(JsonElement json) {
      ItemStack stack = ItemStackTemplate.CODEC.parse(JSON_OPS, json).getOrThrow(JsonParseException::new).create();
      if (stack.isEmpty()) {
        throw new JsonParseException("Item stack resolved to an empty item");
      }
      return stack;
    }

    private SizedIngredient readIngredient(JsonElement json) {
      if (json.isJsonObject() && json.getAsJsonObject().has("ingredient")) {
        return SizedIngredient.LOADABLE.deserialize(json.getAsJsonObject());
      }
      return SizedIngredient.of(IngredientLoadable.DISALLOW_EMPTY.convert(json, "ingredient"));
    }
  }
}
