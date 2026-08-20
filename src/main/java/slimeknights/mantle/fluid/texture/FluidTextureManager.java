package slimeknights.mantle.fluid.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.Registry;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.mantle.fluid.InvertedFluidType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Manager for handling fluid textures */
public class FluidTextureManager implements IEarlySafeManagerReloadListener {
  /** Folder containing the logic */
  public static final String FOLDER = "mantle/fluid_texture";
  private static final FileToIdConverter LISTER = FileToIdConverter.json(FOLDER);

  /* Instance data */
  private static final FluidTextureManager INSTANCE = new FluidTextureManager();
  /** Stack-sensitive tint sources supplied by fluids whose color is not constant. */
  private static final Map<FluidType,FluidTintSource> TINT_SOURCES = new IdentityHashMap<>();
  /** Map of fluid type to texture */
  private Map<FluidType,FluidTexture> textures = Collections.emptyMap();
  /** Fallback texture instance */
  private static final FluidTexture FALLBACK = new FluidTexture(Identifier.parse("block/water_still"), Identifier.parse("block/water_flow"), null, null, 0, -1, -1, false, false, 0, 0);

  private FluidTextureManager() {}


  /**
   * Initializes this manager, registering it with the resource manager
   */
  public static void init(AddClientReloadListenersEvent event) {
    event.addListener(Mantle.getResource("fluid_textures"), INSTANCE);
  }

  /**
   * Registers a stack-sensitive tint source for a fluid type.
   * <p>
   * JSON fluid textures can only provide a constant fallback color. This hook
   * keeps that data-driven path while allowing component-bearing fluid stacks,
   * such as potions, to choose their final color from the stack.
   */
  public static void registerTintSource(FluidType type, FluidTintSource tintSource) {
    TINT_SOURCES.put(type, tintSource);
  }

  public static void registerModels(RegisterFluidModelsEvent event) {
    for (net.minecraft.world.level.material.Fluid fluid : BuiltInRegistries.FLUID) {
      FluidType type = fluid.getFluidType();
      FluidTexture data = INSTANCE.textures.get(type);
      if (data == null) {
        continue;
      }
      Identifier flowing = type instanceof InvertedFluidType ? data.flowing().withSuffix("_inverted") : data.flowing();
      Material overlay = data.overlay() == null ? null : new Material(data.overlay());
      FluidTintSource tint = TINT_SOURCES.get(type);
      if (tint == null && data.color() != -1) {
        tint = FluidTintSources.constant(data.color());
      }
      event.register(new FluidModel.Unbaked(new Material(data.still()), new Material(flowing), overlay, tint), fluid);
    }
  }

  @Override
  public void onReloadSafe(ResourceManager resourceManager) {
    long time = System.nanoTime();
    // fetch JSONs
    Map<Identifier,JsonElement> jsons = new HashMap<>();
    for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
      Identifier id = LISTER.fileToId(entry.getKey());
      try (Reader reader = entry.getValue().openAsReader()) {
        jsons.put(id, StrictJsonParser.parse(reader));
      } catch (IOException | JsonParseException exception) {
        Mantle.logger.error("Couldn't parse fluid texture {} from {}", id, entry.getKey(), exception);
      }
    }

    // start building fluid type map
    Map<FluidType, FluidTexture> map = new HashMap<>();
    Registry<FluidType> fluidTypeRegistry = NeoForgeRegistries.FLUID_TYPES;


    for (Map.Entry<Identifier,JsonElement> entry : jsons.entrySet()) {
      Identifier id = entry.getKey();
      // first step is to find the matching fluid type, if there is none ignore the file
      FluidType type = fluidTypeRegistry.getValue(id);
      if (type == null || !id.equals(fluidTypeRegistry.getKey(type))) {
        Mantle.logger.debug("Ignoring fluid texture {} as no fluid type exists with that name", id);
      } else {
        // parse it if valid
        map.put(type, FluidTexture.deserialize(GsonHelper.convertToJsonObject(entry.getValue(), "fluid_texture")));
      }
    }
    this.textures = map;
    Mantle.logger.info("Loaded {} fluid textures in {} ms", map.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Gets the texture for the given fluid */
  public static FluidTexture getData(FluidType fluid) {
    return INSTANCE.textures.getOrDefault(fluid, FALLBACK);
  }

  /** Gets the still texture for the given fluid */
  public static Identifier getStillTexture(FluidType fluid) {
    return getData(fluid).still();
  }

  /** Gets the flowing texture for the given fluid */
  public static Identifier getFlowingTexture(FluidType fluid) {
    return getData(fluid).flowing();
  }

  /** Gets the overlay texture for the given fluid */
  @Nullable
  public static Identifier getOverlayTexture(FluidType fluid) {
    return getData(fluid).overlay();
  }

  /** Gets the camera texture for the given fluid */
  @Nullable
  public static Identifier getCameraTexture(FluidType fluid) {
    return getData(fluid).camera();
  }

  /** Gets the still texture for the given fluid */
  public static int getColor(FluidType fluid) {
    return getData(fluid).color();
  }
}
