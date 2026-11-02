package slimeknights.mantle.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.Mantle;

/** List of all tags used directly by mantle */
public class MantleTags {
  public static void init() {
    Blocks.init();
    Items.init();
    Fluids.init();
    BlockEntities.init();
  }

  public static class Blocks {
    private static void init() {}
    /** Blocks in this tag will show fluid tooltips when targeted */
    public static final TagKey<Block> GAUGES = tag("gauges");
    /**
     * Blocks in this tag will show the fluid of attached block.
     * Must have {@link net.minecraft.world.level.block.state.properties.BlockStateProperties#FACING}.
     * @see slimeknights.mantle.block.GaugeBlock
     */
    public static final TagKey<Block> ATTACHED_GAUGES = tag("gauges/attached");
    /** Blocks in this tag will show the fluid contained. Must have a block entity with a fluid handler capability. */
    public static final TagKey<Block> GAUGE_TANKS = tag("gauges/tank");
    /** Blocks that must not expose tank information through a gauge. */
    public static final TagKey<Block> GAUGE_BLACKLIST = tag("gauge_blacklist");
    /** Blocks whose gauges only display their fluid name. */
    public static final TagKey<Block> HIDES_GAUGE_AMOUNT = tag("hides_gauge_amount");

    /** Adds a mantle domain tag */
    private static TagKey<Block> tag(String name) {
      return TagKey.create(Registries.BLOCK, Mantle.getResource(name));
    }
  }

  public static class Items {
    private static void init() {}
    /** Tag of empty glass bottles that would contain a splash potion */
    public static final TagKey<Item> SPLASH_BOTTLE = common("bottles/splash");
    /** Tag of empty glass bottles that would contain a lingering potion */
    public static final TagKey<Item> LINGERING_BOTTLE = common("bottles/lingering");
    /** Items in this tag remain on the player after death */
    public static final TagKey<Item> POTION_BUCKETS = common("potions/bucket");
    public static final TagKey<Item> EMPTY_FLUID_CONTAINERS = TagKey.create(Registries.ITEM, Mantle.getResource("fluid_containers/empty"));
    public static final TagKey<Item> FILLED_FLUID_CONTAINERS = TagKey.create(Registries.ITEM, Mantle.getResource("fluid_containers/filled"));
    public static final TagKey<Item> SOULBOUND = ItemTags.create(Mantle.getResource("soulbound"));

    /** Adds a common domain tag */
    private static TagKey<Item> common(String name) {
      return TagKey.create(Registries.ITEM, Mantle.commonResource(name));
    }
  }

  public static class Fluids {
    private static void init() {}
    public static final TagKey<Fluid> SOUP = tag("soup");
    public static final TagKey<Fluid> HIDE_IN_DISPLAY = tag("hide_in_display");
    private static TagKey<Fluid> tag(String name) { return TagKey.create(Registries.FLUID, Mantle.getResource(name)); }
  }

  public static class BlockEntities {
    private static void init() {}

    /**
     * Any block entities in this tag will show just the fluid name, no capacity when viewed in a gauge.
     * Useful for blocks that don't fully sync the fluid to client, such as channels.
     */
    public static final TagKey<BlockEntityType<?>> HIDES_GAUGE_AMOUNT = tag("hides_gauge_amount");

    /** Any block entities in this tag will not show any gauge information. */
    public static final TagKey<BlockEntityType<?>> GAUGE_BLACKLIST = tag("gauge_blacklist");


    /** Adds a mantle domain tag */
    private static TagKey<BlockEntityType<?>> tag(String name) {
      return TagKey.create(Registries.BLOCK_ENTITY_TYPE, Mantle.getResource(name));
    }
  }

  public static class Potions {
    public static final TagKey<net.minecraft.world.item.alchemy.Potion> HIDDEN_FLUID = TagKey.create(Registries.POTION, Mantle.getResource("hide_in_display"));
  }

  public static class MenuTypes {
    private static void init() {}

    /** Any menus in this tag allow closing during {@link Item#overrideOtherStackedOnMe(ItemStack, ItemStack, Slot, ClickAction, Player, SlotAccess)} to open another menu or screen. */
    public static final TagKey<MenuType<?>> REPLACEABLE = TagKey.create(Registries.MENU, Mantle.getResource("replaceable"));
  }
}
