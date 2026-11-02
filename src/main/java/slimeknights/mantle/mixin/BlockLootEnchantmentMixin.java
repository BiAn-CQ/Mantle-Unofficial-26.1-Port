package slimeknights.mantle.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.registration.MantleAttributes;

/** Keeps the breaking entity available for attribute-based fortune on 26.1. */
@Mixin(EventHooks.class)
public abstract class BlockLootEnchantmentMixin {
  @Inject(method = "getBlockLootEnchantmentLevel", at = @At("RETURN"), cancellable = true, remap = false)
  private static void mantle$fortune(ItemInstance tool, Holder<Enchantment> enchantment, int level,
                                      LootContext context, CallbackInfoReturnable<Integer> callback) {
    if (enchantment.is(Enchantments.FORTUNE)
        && context.getOptionalParameter(LootContextParams.BLOCK_STATE) != null
        && context.getOptionalParameter(LootContextParams.ORIGIN) != null
        && context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
      callback.setReturnValue(MantleAttributes.getEnchantmentLevel(entity, MantleAttributes.FORTUNE_BOOST, callback.getReturnValueI()));
    }
  }
}
