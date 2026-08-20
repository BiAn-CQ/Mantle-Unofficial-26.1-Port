package slimeknights.mantle.client.model.connected;

import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Registry for the small, data-driven predicate and suffix vocabulary used by
 * Mantle connected textures.
 *
 * <p>The 26.1 renderer consumes this registry from the native block-state
 * model implementation. Keeping the registry public also preserves the
 * extension point used by addons which register their own connection logic.</p>
 */
public final class ConnectedModelRegistry {
  private ConnectedModelRegistry() {}

  private static final BiPredicate<BlockState,BlockState> BLOCK_CONNECTION_PREDICATE =
      (state, neighbor) -> state.getBlock() == neighbor.getBlock();
  private static final Map<String,BiPredicate<BlockState,BlockState>> CONNECTION_PREDICATES = new HashMap<>();
  private static final Map<String,String[]> CONNECTION_TYPES = new HashMap<>();

  /** Registers a predicate unless a predicate with the same name already exists. */
  public static void registerPredicate(String name, BiPredicate<BlockState,BlockState> predicate) {
    CONNECTION_PREDICATES.putIfAbsent(name, predicate);
  }

  /** Gets a predicate, falling back to same-block connections for compatibility. */
  public static BiPredicate<BlockState,BlockState> getPredicate(String name) {
    return CONNECTION_PREDICATES.getOrDefault(name, BLOCK_CONNECTION_PREDICATE);
  }

  /** Registers one of the 16 horizontal texture suffix tables. */
  public static void registerType(String name, Function<Predicate<Direction>,String> mapper) {
    if (!CONNECTION_TYPES.containsKey(name)) {
      String[] suffixes = new String[16];
      for (int i = 0; i < suffixes.length; i++) {
        int index = i;
        suffixes[i] = mapper.apply(direction -> (index & (1 << direction.get2DDataValue())) != 0);
      }
      CONNECTION_TYPES.put(name, suffixes);
    }
  }

  /** Returns the suffix table or throws for an invalid data value. */
  public static String[] getTextureSuffixes(String name) {
    String[] suffixes = CONNECTION_TYPES.get(name);
    if (suffixes == null) {
      throw new IllegalArgumentException("Unknown connection type " + name);
    }
    return suffixes;
  }

  private static boolean safeGet(BlockState state, BooleanProperty property) {
    return state.hasProperty(property) && state.getValue(property);
  }

  static {
    registerPredicate("block", BLOCK_CONNECTION_PREDICATE);
    registerPredicate("pane", (state, neighbor) -> {
      boolean stateHasArms = safeGet(state, PipeBlock.NORTH) || safeGet(state, PipeBlock.EAST)
          || safeGet(state, PipeBlock.SOUTH) || safeGet(state, PipeBlock.WEST);
      boolean neighborHasArms = safeGet(neighbor, PipeBlock.NORTH) || safeGet(neighbor, PipeBlock.EAST)
          || safeGet(neighbor, PipeBlock.SOUTH) || safeGet(neighbor, PipeBlock.WEST);
      return state.getBlock() == neighbor.getBlock() && stateHasArms == neighborHasArms;
    });

    registerType("cornerless_full", predicate -> {
      StringBuilder name = new StringBuilder();
      if (predicate.test(Direction.NORTH)) name.append('u');
      if (predicate.test(Direction.SOUTH)) name.append('d');
      if (predicate.test(Direction.WEST)) name.append('l');
      if (predicate.test(Direction.EAST)) name.append('r');
      return name.toString();
    });
    registerType("horizontal", predicate -> {
      boolean right = predicate.test(Direction.EAST);
      if (predicate.test(Direction.WEST)) return right ? "middle" : "right";
      return right ? "left" : "";
    });
    registerType("vertical", predicate -> {
      boolean bottom = predicate.test(Direction.SOUTH);
      if (predicate.test(Direction.NORTH)) return bottom ? "middle" : "bottom";
      return bottom ? "top" : "";
    });
    registerType("top", predicate -> predicate.test(Direction.NORTH) ? "bottom" : "");
  }
}
