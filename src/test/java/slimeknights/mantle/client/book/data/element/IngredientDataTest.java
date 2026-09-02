package slimeknights.mantle.client.book.data.element;

import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientDataTest {
  @Test
  void readsNativeIngredientSyntax() {
    if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
      Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    var json = JsonParser.parseString("\"minecraft:stone\"");

    IngredientData data = new IngredientData.Deserializer().deserialize(json, IngredientData.class, null);

    assertThat(data.ingredients).hasSize(1);
    assertThat(data.ingredients[0].test(Items.STONE.getDefaultInstance())).isTrue();
  }

  @Test
  void rejectsLegacyIngredientObjectSyntax() {
    var json = JsonParser.parseString("{\"item\":\"minecraft:stone\"}");

    IngredientData data = new IngredientData.Deserializer().deserialize(json, IngredientData.class, null);

    assertThat(data.ingredients).isEmpty();
  }

  @Test
  void readsNativeItemStackComponents() {
    if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
      Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    var json = JsonParser.parseString("""
      {
        "id": "minecraft:stone",
        "components": {
          "minecraft:custom_data": {
            "mantle_test": true
          }
        }
      }
      """);

    IngredientData data = new IngredientData.Deserializer().deserialize(json, IngredientData.class, null);

    assertThat(data.getItems()).hasSize(1);
    CustomData customData = data.getItems().getFirst().get(DataComponents.CUSTOM_DATA);
    assertThat(customData).isNotNull();
    assertThat(customData.copyTag().getBooleanOr("mantle_test", false)).isTrue();
  }
}
