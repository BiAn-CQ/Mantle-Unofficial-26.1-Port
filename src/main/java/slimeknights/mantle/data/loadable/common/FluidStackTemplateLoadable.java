package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;
import java.util.function.Function;

/**
 * Loadable for a fluid stack template.
 *
 * <p>NeoForge 26.1 binds the default fluid components after datapack loading.
 * Decoding directly to {@code FluidStack} during a reload therefore fails with
 * "Components not bound yet".  This loadable keeps the fluid and component
 * patch in a template until the output is actually used.</p>
 */
public final class FluidStackTemplateLoadable {
  private FluidStackTemplateLoadable() {}

  private static final Function<FluidStackTemplate,Fluid> FLUID_GETTER = template -> template.fluid().value();
  private static final LoadableField<Fluid,FluidStackTemplate> FLUID = Loadables.FLUID.defaultField(
    "fluid", Fluids.EMPTY, false, FLUID_GETTER);
  private static final LoadableField<Integer,FluidStackTemplate> AMOUNT = IntLoadable.FROM_ZERO.requiredField(
    "amount", FluidStackTemplate::amount);
  private static final LoadableField<CompoundTag,FluidStackTemplate> NBT = NBTLoadable.ALLOW_STRING.nullableField(
    "nbt", FluidStackTemplateLoadable::getCustomData);

  /** Loadable for a variable amount with no custom data. */
  public static final RecordLoadable<FluidStackTemplate> STACK = RecordLoadable.create(
    FLUID, AMOUNT, (fluid, amount) -> makeTemplate(fluid, amount, null));
  /** Loadable for a variable amount with legacy custom NBT. */
  public static final RecordLoadable<FluidStackTemplate> STACK_NBT = RecordLoadable.create(
    FLUID, AMOUNT, NBT, FluidStackTemplateLoadable::makeTemplate);

  private static FluidStackTemplate makeTemplate(Fluid fluid, int amount, CompoundTag nbt) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      throw new IllegalArgumentException("FluidStackTemplate cannot be empty");
    }
    DataComponentPatch patch = DataComponentPatch.EMPTY;
    if (nbt != null) {
      patch = DataComponentPatch.builder()
        .set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt.copy()))
        .build();
    }
    return new FluidStackTemplate(fluid, amount, patch);
  }

  private static CompoundTag getCustomData(FluidStackTemplate template) {
    Optional<net.minecraft.world.item.component.CustomData> customData =
      template.components().getPatch(DataComponents.CUSTOM_DATA);
    return customData == null || customData.isEmpty() ? null : customData.orElseThrow().copyTag();
  }

  /**
   * Keeps the compact fluid form useful for callers that use this loadable
   * directly, while retaining the legacy object form for amounts and NBT.
   */
  public static FluidStackTemplate convert(JsonElement element, String key, TypedMap context) {
    if (element.isJsonPrimitive()) {
      return makeTemplate(Loadables.FLUID.convert(element, key, context),
                          net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME, null);
    }
    return STACK_NBT.convert(element, key, context);
  }

  /** Serializes using the legacy Mantle fluid/amount/NBT representation. */
  public static JsonElement serialize(FluidStackTemplate template) {
    return STACK_NBT.serialize(template);
  }
}
