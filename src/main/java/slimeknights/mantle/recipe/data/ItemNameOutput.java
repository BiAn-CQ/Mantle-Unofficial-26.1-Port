package slimeknights.mantle.recipe.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.recipe.helper.ItemOutput;

/**
 * Extension of {@link ItemOutput} for datagen of recipes using items from optional integrations.
 * Should never be used in an actual recipe.
 */
public class ItemNameOutput extends ItemOutput {
  private final Identifier name;
  private final int count;

  private ItemNameOutput(Identifier name, int count) {
    this.name = name;
    this.count = count;
  }

  /**
   * Creates an output for the given item with no NBT
   * @param name   Item name
   * @param count  Count
   * @return  Output
   */
  public static ItemNameOutput fromName(Identifier name, int count) {
    return new ItemNameOutput(name, count);
  }

  /**
   * Creates an output for the given item with a count of 1
   * @param name  Item name
   * @return  Output
   */
  public static ItemNameOutput fromName(Identifier name) {
    return fromName(name, 1);
  }

  @Override
  public ItemStack get() {
    throw new UnsupportedOperationException("Cannot get the item stack from a item name output");
  }

  @Override
  public int getCount() {
    return count;
  }

  @Override
  public JsonElement serialize(boolean writeCount) {
    JsonObject jsonResult = new JsonObject();
    jsonResult.addProperty("id", name.toString());
    if (writeCount && count != 1) {
      jsonResult.addProperty("count", count);
    }
    return jsonResult;
  }
}
