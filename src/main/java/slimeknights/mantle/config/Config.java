package slimeknights.mantle.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for all Mantle specific config options
 */
@Internal
public class Config {
  public enum HeartRenderer {
    DISABLE, NO_MAX, WITH_MAX
  }

  /** Heart renderer mode */
  public static final EnumValue<HeartRenderer> HEART_RENDERER;

  /** Whether JEI displays registered potion fluid variants. */
  public static final BooleanValue SHOW_POTION_FLUID_IN_JEI;
  public static final BooleanValue POWDER_SNOW_CAULDRON_FLUID;
  public static final BooleanValue POWDER_SNOW_BUCKET;

  /** List of preferences for tag outputs */
  private static final List<String> DEFAULT_TAG_PREFERENCES = Arrays.asList("minecraft", "tconstruct", "tmechworks", "metalborn", "embers", "create", "immersiveengineering", "mekanism", "thermal");
  public static final ConfigValue<List<? extends String>> TAG_PREFERENCES;

  public static final ModConfigSpec CLIENT_SPEC, COMMON_SPEC, SERVER_SPEC;

  static {
    ModConfigSpec.Builder client = new ModConfigSpec.Builder();
    ModConfigSpec.Builder common = new ModConfigSpec.Builder();
    ModConfigSpec.Builder server = new ModConfigSpec.Builder();

    // client options
    HEART_RENDERER = client
      .comment(
        "If not DISABLE, enables the Mantle heart renderer, which stacks hearts by changing the color instead of vertically stacking them.",
        "WITH_MAX will show the max health in colored containers behind the health bar. NO_MAX will show just the health bar",
        "If DISABLE, uses the Forge heart renderer.",
        "Mod authors: this config is not meant for compatibility with your heart renderer, cancel the RenderGameOverlayEvent.Pre event and our logic won't run")
      .translation("config.mantle.extraHeartRenderer")
      .defineEnum("heartRenderer", HeartRenderer.WITH_MAX);

    SHOW_POTION_FLUID_IN_JEI = client.comment("Show all registered potion variants in JEI when the shared potion fluid is enabled.")
      .define("showPotionFluid", true);
    POWDER_SNOW_CAULDRON_FLUID = common.comment("Register shared powder snow for the vanilla cauldron when available; skip if another fluid already owns it.")
      .define("powder_snow_cauldron_fluid", true);
    POWDER_SNOW_BUCKET = common.comment("Register shared powder snow for the vanilla bucket when available.")
      .define("powder_snow.bucket", true);
    COMMON_SPEC = common.build();

    // server options
    TAG_PREFERENCES = server.comment("Preferences for outputs from tags used in automatic compat in recipes")
                            .translation("config.mantle.tagPreferences")
                            .defineList("tagPreferences", DEFAULT_TAG_PREFERENCES, str -> true);

    CLIENT_SPEC = client.build();
    SERVER_SPEC = server.build();
  }
}
