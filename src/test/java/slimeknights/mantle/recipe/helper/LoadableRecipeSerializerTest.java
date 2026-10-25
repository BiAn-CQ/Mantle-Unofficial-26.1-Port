package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"rawtypes", "unchecked"})
class LoadableRecipeSerializerTest {
  private static final RecordLoadable<ContextRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPE.requiredField(),
    LoadableRecipeSerializer.SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(),
    StringLoadable.DEFAULT.requiredField("value", recipe -> recipe.value),
    ContextRecipe::new);

  @Test
  void sharedLoadableKeepsEachSerializersTypeAndIdentityInJson() {
    var json = JsonParser.parseString("{\"value\":\"payload\"}");
    for (int index = 0; index < 2; index++) {
      RecipeType<ContextRecipe> type = new RecipeType<>() {};
      var serializer = LoadableRecipeSerializer.of(LOADER, () -> type);
      var recipe = serializer.codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();

      assertThat(recipe.getType()).isSameAs(type);
      assertThat(recipe.getSerializer()).isSameAs(serializer);
      assertThat(recipe.value).isEqualTo("payload");
      assertThat(serializer.codec().codec().encodeStart(JsonOps.INSTANCE, recipe).getOrThrow()).isEqualTo(json);
    }
  }

  @Test
  void directJsonLoadingPreservesTheRecipeId() {
    RecipeType<ContextRecipe> type = new RecipeType<>() {};
    var serializer = LoadableRecipeSerializer.of(LOADER, () -> type);
    var id = Identifier.parse("test:named_recipe");
    var json = JsonParser.parseString("{\"value\":\"named\"}").getAsJsonObject();

    var recipe = LoadableRecipeSerializer.fromJson(serializer, id, json);

    assertThat(recipe.id).isEqualTo(id);
    assertThat(recipe.getType()).isSameAs(type);
    assertThat(recipe.getSerializer()).isSameAs(serializer);
  }

  @SuppressWarnings("deprecation")
  @Test
  void networkRoundTripInjectsTheTypeAndSerializerWithoutSerializingThem() {
    RecipeType<ContextRecipe> type = new RecipeType<>() {};
    var serializer = LoadableRecipeSerializer.of(LOADER, () -> type);
    var registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
    try {
      var original = new ContextRecipe(type, serializer, Identifier.parse("test:source"), "network");
      serializer.streamCodec().encode(buffer, original);
      var decoded = serializer.streamCodec().decode(buffer);

      assertThat(decoded.getType()).isSameAs(type);
      assertThat(decoded.getSerializer()).isSameAs(serializer);
      assertThat(decoded.value).isEqualTo("network");
      assertThat(buffer.readableBytes()).isZero();
    } finally {
      buffer.release();
    }
  }

  private static final class ContextRecipe extends CustomRecipe {
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    private final Identifier id;
    private final String value;

    private ContextRecipe(RecipeType<?> type, RecipeSerializer<?> serializer, Identifier id, String value) {
      this.type = type;
      this.serializer = serializer;
      this.id = id;
      this.value = value;
    }

    @Override public RecipeType getType() { return type; }
    @Override public RecipeSerializer getSerializer() { return serializer; }
    @Override public boolean matches(CraftingInput input, Level level) { return false; }
    @Override public ItemStack assemble(CraftingInput input) { return ItemStack.EMPTY; }
  }
}
