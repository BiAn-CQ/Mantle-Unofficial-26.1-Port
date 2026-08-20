package slimeknights.mantle.loot;

import com.google.gson.JsonDeserializer;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.loot.condition.BlockTagLootCondition;
import slimeknights.mantle.loot.condition.ContainsItemModifierLootCondition;
import slimeknights.mantle.loot.condition.EmptyModifierLootCondition;
import slimeknights.mantle.loot.condition.HasLootContextSetCondition;
import slimeknights.mantle.loot.condition.ILootModifierCondition;
import slimeknights.mantle.loot.condition.InvertedModifierLootCondition;
import slimeknights.mantle.loot.entry.TagPreferenceLootEntry;
import slimeknights.mantle.loot.function.RetexturedLootFunction;
import slimeknights.mantle.loot.function.SetFluidLootFunction;
import slimeknights.mantle.recipe.condition.TagEmptyCondition;
import slimeknights.mantle.recipe.condition.TagFilledCondition;

import static slimeknights.mantle.loot.condition.ILootModifierCondition.MODIFIER_CONDITIONS;

public final class MantleLoot {
  private MantleLoot() {}

  public static final MapCodec<TagEmptyCondition<?>> TAG_EMPTY = TagEmptyCondition.CODEC;
  public static final MapCodec<TagFilledCondition<?>> TAG_FILLED = TagFilledCondition.CODEC;
  public static final MapCodec<BlockTagLootCondition> BLOCK_TAG_CONDITION = BlockTagLootCondition.CODEC;
  public static final MapCodec<HasLootContextSetCondition> HAS_CONTEXT_SET = HasLootContextSetCondition.CODEC;
  public static final MapCodec<RetexturedLootFunction> RETEXTURED_FUNCTION = RetexturedLootFunction.CODEC;
  public static final MapCodec<SetFluidLootFunction> SET_FLUID_FUNCTION = SetFluidLootFunction.CODEC;
  public static final MapCodec<TagPreferenceLootEntry> TAG_PREFERENCE = TagPreferenceLootEntry.CODEC;

  /**
   * Called during serializer registration to register any relevant loot logic
   */
  public static void registerGlobalLootModifiers(final RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();

    if (key == NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS) {
      event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mantle.getResource("add_entry"), () -> AddEntryLootModifier.CODEC);
      event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mantle.getResource("replace_item"), () -> ReplaceItemLootModifier.CODEC);

      // loot modifier conditions
      MODIFIER_CONDITIONS.registerDeserializer(InvertedModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>)InvertedModifierLootCondition::deserialize);
      MODIFIER_CONDITIONS.registerDeserializer(EmptyModifierLootCondition.ID, EmptyModifierLootCondition.INSTANCE);
      MODIFIER_CONDITIONS.registerDeserializer(ContainsItemModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>)ContainsItemModifierLootCondition::deserialize);
    } else if (key == Registries.LOOT_FUNCTION_TYPE) {
      registerFunction(event, "fill_retextured_block", RETEXTURED_FUNCTION);
      registerFunction(event, "set_fluid", SET_FLUID_FUNCTION);
    } else if (key == Registries.LOOT_CONDITION_TYPE) {
      registerCondition(event, "block_tag", BLOCK_TAG_CONDITION);
      registerCondition(event, "has_context_set", HAS_CONTEXT_SET);
      event.register(Registries.LOOT_CONDITION_TYPE, TagEmptyCondition.ID, () -> TAG_EMPTY);
      event.register(Registries.LOOT_CONDITION_TYPE, TagFilledCondition.ID, () -> TAG_FILLED);
    } else if (key == Registries.LOOT_POOL_ENTRY_TYPE) {
      registerEntry(event, "tag_preference", TAG_PREFERENCE);
    }
  }

  private static void registerCondition(RegisterEvent event, String name, MapCodec<? extends LootItemCondition> codec) {
    event.register(Registries.LOOT_CONDITION_TYPE, Mantle.getResource(name), () -> codec);
  }

  /**
   * Registers a loot function
   * @param name        Loot function name
   * @param serializer  Loot function serializer
   * @return  Registered loot function
   */
  private static void registerFunction(RegisterEvent event, String name, MapCodec<? extends LootItemFunction> codec) {
    event.register(Registries.LOOT_FUNCTION_TYPE, Mantle.getResource(name), () -> codec);
  }

  private static void registerEntry(RegisterEvent event, String name, MapCodec<? extends LootPoolEntryContainer> codec) {
    event.register(Registries.LOOT_POOL_ENTRY_TYPE, Mantle.getResource(name), () -> codec);
  }
}
