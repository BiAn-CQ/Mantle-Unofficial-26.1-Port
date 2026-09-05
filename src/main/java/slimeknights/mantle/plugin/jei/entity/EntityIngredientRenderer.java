package slimeknights.mantle.plugin.jei.entity;

import lombok.RequiredArgsConstructor;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renderer for entity type ingredients
 */
@RequiredArgsConstructor
public class EntityIngredientRenderer implements IIngredientRenderer<EntityIngredient.EntityInput> {
  private static final Identifier MISSING = Mantle.getResource("textures/item/missingno.png");
  /** Entity types that will not render, as they either errored or are the wrong type */
  private static final Set<EntityType<?>> IGNORED_ENTITIES = new HashSet<>();

  /** Square size of the renderer in pixels */
  private final int size;

  /** Cache of entities for each entity type */
  private final Map<EntityType<?>,Entity> ENTITY_MAP = new HashMap<>();

  @Override
  public int getWidth() {
    return size;
  }

  @Override
  public int getHeight() {
    return size;
  }

  @Override
  public void render(GuiGraphicsExtractor graphics, @Nullable EntityIngredient.EntityInput input) {
    renderAt(graphics, input, 0, 0);
  }

  /**
   * JEI positions ingredient renderers using the GUI pose stack. Picture-in-picture
   * render states, including entities, use absolute bounds instead, so convert the
   * ingredient rectangle through the current pose before submitting the entity.
   */
  @Override
  public void render(GuiGraphicsExtractor graphics, @Nullable EntityIngredient.EntityInput input, int x, int y) {
    renderAt(graphics, input, x, y);
  }

  private void renderAt(GuiGraphicsExtractor graphics, @Nullable EntityIngredient.EntityInput input, int x, int y) {
    if (input != null) {
      Level world = Minecraft.getInstance().level;
      EntityType<?> type = input.type();
      if (world != null && !IGNORED_ENTITIES.contains(type)) {
        Entity entity;
        // players cannot be created using the type, but we can use the client player
        // side effect is it renders armor/items
        if (type == EntityType.PLAYER) {
          entity = Minecraft.getInstance().player;
        } else {
          // entity is created with the client world, but the entity map is thrown away when JEI restarts so they should be okay I think
          entity = ENTITY_MAP.computeIfAbsent(type, t -> t.create(world, EntitySpawnReason.COMMAND));
        }
        // only can draw living entities, plus non-living ones don't get recipes anyways
        if (entity instanceof LivingEntity livingEntity) {
          // scale down large mobs, but don't scale up small ones
          int scale = size / 2;
          float height = entity.getBbHeight();
          float width = entity.getBbWidth();
          if (height > 2 || width > 2) {
            scale = Math.max(1, (int)(size / Math.max(height, width)));
          }
          // catch exceptions drawing the entity to be safe, any caught exceptions blacklist the entity
          try {
            Matrix3x2fc pose = graphics.pose();
            Vector2f corner0 = pose.transformPosition(x, y, new Vector2f());
            Vector2f corner1 = pose.transformPosition(x + size, y, new Vector2f());
            Vector2f corner2 = pose.transformPosition(x, y + size, new Vector2f());
            Vector2f corner3 = pose.transformPosition(x + size, y + size, new Vector2f());
            int x0 = Mth.floor(Math.min(Math.min(corner0.x, corner1.x), Math.min(corner2.x, corner3.x)));
            int y0 = Mth.floor(Math.min(Math.min(corner0.y, corner1.y), Math.min(corner2.y, corner3.y)));
            int x1 = Mth.ceil(Math.max(Math.max(corner0.x, corner1.x), Math.max(corner2.x, corner3.x)));
            int y1 = Mth.ceil(Math.max(Math.max(corner0.y, corner1.y), Math.max(corner2.y, corner3.y)));
            // 26.1 takes a clipping rectangle before the entity scale. The old
            // center-point call was accidentally interpreted as x0/y0/x1/y1,
            // producing negative PIP texture dimensions when JEI opened recipes.
            InventoryScreen.renderEntityInInventoryFollowsAngle(
              graphics, x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), scale, 0, 0, 0, livingEntity);
            return;
          } catch (Exception e) {
            Mantle.logger.error("Error drawing entity " + BuiltInRegistries.ENTITY_TYPE.getKey(type), e);
            IGNORED_ENTITIES.add(type);
            ENTITY_MAP.remove(type);
          }
        } else {
          // not living, so might as well skip next time
          IGNORED_ENTITIES.add(type);
          ENTITY_MAP.remove(type);
        }
      }

      // fallback, draw a pink and black "spawn egg"
      int offset = (size - 16) / 2;
      graphics.blit(MISSING, offset, offset, 16, 16, 0, 0, 16, 16);
    }
  }

  @Override
  public List<Component> getTooltip(EntityIngredient.EntityInput type, TooltipFlag flag) {
    List<Component> tooltip = new ArrayList<>();
    tooltip.add(type.type().getDescription());
    if (flag.isAdvanced()) {
      tooltip.add((Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(type.type()).toString())).withStyle(ChatFormatting.DARK_GRAY));
    }
    return tooltip;
  }

  @Override
  public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, EntityIngredient.EntityInput ingredient, TooltipFlag flag) {
    tooltip.add(ingredient.type().getDescription());
    if (flag.isAdvanced()) {
      tooltip.add(Component.literal(slimeknights.mantle.data.loadable.Loadables.ENTITY_TYPE.getString(ingredient.type()))
        .withStyle(ChatFormatting.DARK_GRAY));
    }
  }
}
