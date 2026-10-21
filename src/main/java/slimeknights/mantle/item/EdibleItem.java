package slimeknights.mantle.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;
import java.util.function.Consumer;

/** Food item showing effects from the native consumable component. */
public class EdibleItem extends Item {
  private final ItemUseAnimation animation;
  public EdibleItem(ItemUseAnimation animation, Properties properties) { super(properties); this.animation=animation; }
  public EdibleItem(Properties properties) { this(ItemUseAnimation.EAT,properties); }
  @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return animation; }

  public static void addEffectTooltip(ItemStack stack, TooltipContext context, List<Component> tooltip) { addEffectTooltip(stack,context,tooltip::add); }
  public static void addEffectTooltip(ItemStack stack, TooltipContext context, Consumer<Component> tooltip) {
    var consumable=stack.get(DataComponents.CONSUMABLE);
    if (consumable==null) { return; }
    float ticksPerSecond=context.level()==null ? 20 : context.level().tickRateManager().tickrate();
    for (var consumeEffect:consumable.onConsumeEffects()) {
      if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect effect) {
        for (MobEffectInstance instance:effect.effects()) {
          MutableComponent text=Component.translatable(instance.getDescriptionId());
          if (instance.getAmplifier()>0) { text=Component.translatable("potion.withAmplifier",text,Component.translatable("potion.potency."+instance.getAmplifier())); }
          if (instance.getDuration()>20) { text=Component.translatable("potion.withDuration",text,MobEffectUtil.formatDuration(instance,1,ticksPerSecond)); }
          tooltip.accept(text.withStyle(instance.getEffect().value().getCategory().getTooltipFormatting()));
        }
      }
    }
  }
  @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack,context,display,tooltip,flag);
    TranslationHelper.addOptionalTooltip(stack,tooltip);
    addEffectTooltip(stack,context,tooltip);
  }
}
