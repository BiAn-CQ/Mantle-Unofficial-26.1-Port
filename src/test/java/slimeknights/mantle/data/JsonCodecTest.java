package slimeknights.mantle.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonCodecTest {
  private final JsonCodec<Ingredient> ingredientCodec = new JsonCodec<>() {
    @Override
    public Ingredient deserialize(JsonElement element, DynamicOps<?> ops) {
      return IngredientLoadable.DISALLOW_EMPTY.convert(element.getAsJsonObject().get("cast"), "cast");
    }

    @Override
    public JsonElement serialize(Ingredient ingredient, DynamicOps<?> ops) {
      return IngredientLoadable.DISALLOW_EMPTY.serialize(ingredient);
    }
  };

  @Test
  void invalidCastReturnsAnErrorAndTheNextRecipeStillDecodes() {
    var invalid = ingredientCodec.parse(JsonOps.INSTANCE,
      JsonParser.parseString("{\"cast\":{\"tag\":\"tconstruct:casts/single_use/ingot\"}}"));
    assertThat(invalid.error()).isPresent();
    assertThat(invalid.error().orElseThrow().message()).contains("Invalid cast");
    assertThat(ingredientCodec.parse(JsonOps.INSTANCE,
      JsonParser.parseString("{\"cast\":\"minecraft:stone\"}")).result()).isPresent();
  }

  @Test
  void validationErrorsBecomeDataErrorsWithoutHidingUnexpectedFailures() {
    JsonCodec<String> codec = new JsonCodec<>() {
      @Override
      public String deserialize(JsonElement element, DynamicOps<?> ops) {
        if (element.getAsString().equals("invalid")) {
          throw new IllegalArgumentException("Invalid recipe value");
        }
        throw new IllegalStateException("Unexpected failure");
      }

      @Override
      public JsonElement serialize(String value, DynamicOps<?> ops) {
        return new com.google.gson.JsonPrimitive(value);
      }
    };
    assertThat(codec.parse(JsonOps.INSTANCE, JsonParser.parseString("\"invalid\"")).error()
      .orElseThrow().message()).isEqualTo("Invalid recipe value");
    assertThatThrownBy(() -> codec.parse(JsonOps.INSTANCE, JsonParser.parseString("\"unexpected\"")))
      .isInstanceOf(IllegalStateException.class).hasMessage("Unexpected failure");
  }
}
