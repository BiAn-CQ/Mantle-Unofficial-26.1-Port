package slimeknights.mantle.compat;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.item.EdibleItem;
import slimeknights.mantle.recipe.data.SimpleRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.FluidStackSet;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.registration.deferred.RecipeSerializerDeferredRegister;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpstreamRecipeHelpersTest {
  private static final RegistryAccess REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

  @BeforeAll static void bindComponents() {
    for (var fluid : List.of(Fluids.WATER, Fluids.LAVA)) {
      if (!fluid.builtInRegistryHolder().areComponentsBound()) fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
  }

  @Test void fluidSetRoundTripsThroughCodecsAndLoadables() {
    var value = FluidStackSet.of(List.of(Fluids.WATER, Fluids.LAVA), 180);
    var ops = REGISTRIES.createSerializationContext(JsonOps.INSTANCE);
    var codecCopy = FluidStackSet.CODEC.parse(ops, FluidStackSet.CODEC.encodeStart(ops, value).getOrThrow()).getOrThrow();
    var context = ContextKey.lookupContext(REGISTRIES).build();
    var loadableCopy = FluidStackSet.LOADABLE.convert(FluidStackSet.LOADABLE.serialize(value, context), "fluid set", context);
    for (var copy : List.of(codecCopy, loadableCopy)) {
      assertThat(copy.amount()).isEqualTo(180);
      assertThat(copy.fluids().stream().map(holder -> holder.value())).containsExactly(Fluids.WATER, Fluids.LAVA);
      assertThat(copy.contains(Fluids.WATER)).isTrue();
      assertThat(copy.contains(Fluids.EMPTY)).isFalse();
      assertThat(copy.display()).allSatisfy(stack -> assertThat(stack.getAmount()).isEqualTo(180));
    }
  }

  @Test void fluidSetNetworkRoundTripKeepsHoldersAndAmount() {
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    try {
      FluidStackSet.STREAM_CODEC.encode(buffer, FluidStackSet.of(Fluids.WATER, 250));
      var result = FluidStackSet.STREAM_CODEC.decode(buffer);
      assertThat(result.contains(Fluids.WATER.builtInRegistryHolder())).isTrue();
      assertThat(result.amount()).isEqualTo(250);
      assertThat(buffer.readableBytes()).isZero();
    } finally { buffer.release(); }
  }

  @Test void fluidSetJsonRejectsNonPositiveAmountsAndEmptyDisplaysRemainEmpty() {
    var json = new JsonObject(); json.addProperty("fluid", "minecraft:water"); json.addProperty("amount", 0);
    assertThat(FluidStackSet.CODEC.parse(REGISTRIES.createSerializationContext(JsonOps.INSTANCE), json).error()).isPresent();
    assertThat(FluidStackSet.EMPTY.display()).isEmpty();
  }

  @Test void copyingFluidOutputWithAmountKeepsComponentsAndDoesNotMutateSource() {
    var source = new FluidStack(Fluids.WATER, 250);
    source.set(DataComponents.CUSTOM_NAME, Component.literal("original"));
    var output = FluidOutput.fromStack(source);
    var copy = output.copyWithAmount(900);
    assertThat(copy.getAmount()).isEqualTo(900);
    assertThat(copy.get(DataComponents.CUSTOM_NAME)).isEqualTo(Component.literal("original"));
    copy.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
    assertThat(output.getAmount()).isEqualTo(250);
    assertThat(source.get(DataComponents.CUSTOM_NAME)).isEqualTo(Component.literal("original"));
  }

  @Test void singletonSerializerReturnsTheSameInstanceForJsonAndNetwork() {
    Recipe<?> recipe = mock(Recipe.class);
    var serializer = SimpleRecipeSerializer.<Recipe<?>>singleton(recipe);
    assertThat(serializer.codec().codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow()).isSameAs(recipe);
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    try {
      serializer.streamCodec().encode(buffer, recipe);
      assertThat(buffer.readableBytes()).isZero();
      assertThat(serializer.streamCodec().decode(buffer)).isSameAs(recipe);
    } finally { buffer.release(); }
  }

  @Test void noArgumentsSerializerRemainsLazyAndCreatesEachDecodedRecipe() {
    var count = new AtomicInteger();
    var serializer = SimpleRecipeSerializer.noArguments(() -> { count.incrementAndGet(); return mock(Recipe.class); });
    assertThat(count).hasValue(0);
    var first = serializer.codec().codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    try {
      var second = serializer.streamCodec().decode(buffer);
      assertThat(second).isNotSameAs(first);
      assertThat(count).hasValue(2);
    } finally { buffer.release(); }
  }

  @Test void simpleBuilderUsesExplicitNativeRecipeKeyWithoutAnAdvancement() {
    Recipe<?> recipe = mock(Recipe.class);
    var output = mock(RecipeOutput.class);
    var id = Identifier.parse("test:fieldless");
    var builder = SimpleRecipeBuilder.recipe(recipe);
    builder.save(output, id);
    verify(output).accept(ResourceKey.create(Registries.RECIPE, id), recipe, null);
    assertThatThrownBy(() -> builder.save(output)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test void registrationHelpersDoNotConstructFieldlessRecipesDuringDeclaration() {
    var count = new AtomicInteger();
    var register = new RecipeSerializerDeferredRegister("upstream_helpers_test");
    var singleton = register.registerSingleton("singleton", mock(Recipe.class));
    var constructor = register.registerNoArguments("constructor", () -> { count.incrementAndGet(); return mock(Recipe.class); });
    assertThat(singleton.getId()).isEqualTo(Identifier.parse("upstream_helpers_test:singleton"));
    assertThat(constructor.getId()).isEqualTo(Identifier.parse("upstream_helpers_test:constructor"));
    assertThat(count).hasValue(0);
    assertThat(constructor.isBound()).isFalse();
  }

  @Test void consumableTooltipUsesTheContextsTickRateForItsDurationThreshold() {
    var consumable = Consumable.builder().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 30))).build();
    var context = mock(Item.TooltipContext.class);
    when(context.tickRate()).thenReturn(40F);
    var lines = new ArrayList<Component>();
    EdibleItem.addEffectTooltip(consumable, context, lines);
    assertThat(((TranslatableContents)lines.getFirst().getContents()).getKey()).isEqualTo("effect.minecraft.speed");
    when(context.tickRate()).thenReturn(20F);
    lines.clear(); EdibleItem.addEffectTooltip(consumable, context, lines);
    assertThat(((TranslatableContents)lines.getFirst().getContents()).getKey()).isEqualTo("potion.withDuration");
  }

  @Test void renamedEntityEggDisplayKeepsTheCompatibilityEntryPointForEmptyIngredients() {
    var ingredient = EntityIngredient.EMPTY;
    var eggs = ingredient.getDisplayEggs();
    assertThat(eggs).isEmpty();
    assertThat(ingredient.getEggs()).isSameAs(eggs);
  }
}
