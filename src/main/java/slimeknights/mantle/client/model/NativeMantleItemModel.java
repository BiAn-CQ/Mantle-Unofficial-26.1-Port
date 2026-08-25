package slimeknights.mantle.client.model;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.model.util.MantleItemLayerGenerator;
import slimeknights.mantle.util.ItemLayerPixels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native 26.1 bridge for Mantle's data-driven item layer loaders.
 *
 * <p>The old loaders produced Forge {@code BakedModel} overrides.  26.1 uses
 * {@link ItemModel} render states instead, so the bridge bakes the same flat
 * layers with Minecraft's own {@link ItemModelGenerator}.  NBT variants are
 * selected from the modern {@link DataComponents#CUSTOM_DATA} component while
 * retaining the 1.20.1 key names used by Tinkers' Construct.</p>
 */
public final class NativeMantleItemModel implements ItemModel {
  /** Accepts both Mantle's historical eight-digit form and modern # colors. */
  private static final Codec<Integer> LAYER_COLOR_CODEC = Codec.either(
    ExtraCodecs.STRING_ARGB_COLOR,
    Codec.STRING.flatXmap(NativeMantleItemModel::parseLegacyColor,
      color -> DataResult.success(String.format(Locale.ROOT, "#%08X", color)))
  ).xmap(either -> either.map(value -> value, value -> value), Either::left);

  private final ItemModel delegate;

  private NativeMantleItemModel(ItemModel delegate) {
    this.delegate = delegate;
  }

  private static DataResult<Integer> parseLegacyColor(String value) {
    String hex = value.startsWith("#") ? value.substring(1) : value;
    if (hex.length() == 6) {
      hex = "FF" + hex;
    }
    if (hex.length() != 8) {
      return DataResult.error(() -> "Expected 6 or 8 hexadecimal color digits: " + value);
    }
    try {
      return DataResult.success((int)Long.parseLong(hex, 16));
    } catch (NumberFormatException exception) {
      return DataResult.error(() -> "Invalid hexadecimal color: " + value);
    }
  }

  @Override
  public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                     ItemDisplayContext displayContext, @Nullable ClientLevel level,
                     @Nullable ItemOwner owner, int seed) {
    delegate.update(output, item, resolver, displayContext, level, owner, seed);
  }

  private static ItemModel bakeLayers(ItemModel.BakingContext context, Matrix4fc transformation,
                                      Identifier parentId, Map<String, Material> textures,
                                      List<LayerData> layers) {
    ModelBaker baker = context.blockModelBaker();
    ResolvedModel parent = baker.getModel(parentId);
    ModelRenderProperties parentProperties = ModelRenderProperties.fromResolvedModel(
      baker, parent, parent.getTopTextureSlots());
    QuadCollection.Builder quads = new QuadCollection.Builder();
    Material.Baked particle = parentProperties.particleMaterial();
    List<BakedLayer> bakedLayers = new ArrayList<>();

    // Resolve layers in display order, then bake them from top to bottom so the
    // legacy pixel map can suppress only side faces hidden by a higher layer.
    for (int index = 0; index < ItemModelGenerator.LAYERS.size(); index++) {
      String layerName = ItemModelGenerator.LAYERS.get(index);
      Material base = textures.get(layerName);
      if (base == null) {
        break;
      }
      Material.Baked rendered = baker.materials().get(base, parent);
      if (index == 0) {
        particle = rendered;
      }
      LayerData data = index < layers.size() ? layers.get(index) : LayerData.DEFAULT;
      int color = data.color() == -1 ? 0xFFFFFFFF : data.color();
      int layerIndex = data.noTint() ? -1 : index;
      bakedLayers.add(new BakedLayer(rendered, layerIndex,
        new ExtraFaceData(color, data.luminosity(), true)));
    }

    ItemLayerPixels usedPixels = bakedLayers.size() > 1 ? new ItemLayerPixels() : null;
    List<QuadCollection> generatedLayers = new ArrayList<>(bakedLayers.size());
    for (int index = 0; index < bakedLayers.size(); index++) {
      generatedLayers.add(QuadCollection.EMPTY);
    }
    for (int index = bakedLayers.size() - 1; index >= 0; index--) {
      BakedLayer layer = bakedLayers.get(index);
      generatedLayers.set(index, MantleItemLayerGenerator.bake(
        baker, layer.material(), BlockModelRotation.IDENTITY, layer.tintIndex(), layer.faceData(), usedPixels));
    }
    generatedLayers.forEach(layer -> layer.getAll().forEach(quads::addUnculledFace));

    ModelRenderProperties properties = new ModelRenderProperties(
      parentProperties.usesBlockLight(), particle, parentProperties.transforms());
    return new CuboidItemModelWrapper(List.of(), quads.build(), properties, transformation);
  }

  private record BakedLayer(Material.Baked material, int tintIndex, ExtraFaceData faceData) {}

  private static final class DynamicNbtModel implements ItemModel {
    private final String nbtKey;
    private final ItemModel fallback;
    private final Map<String, ItemModel> variants;

    private DynamicNbtModel(String nbtKey, ItemModel fallback, Map<String, ItemModel> variants) {
      this.nbtKey = nbtKey;
      this.fallback = fallback;
      this.variants = variants;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      CustomData data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      String value = data.copyTag().getStringOr(nbtKey, "");
      variants.getOrDefault(value, fallback).update(output, item, resolver, displayContext, level, owner, seed);
    }
  }

  /** Per-layer static color and emission, matching Mantle's legacy JSON. */
  public record LayerData(int color, int luminosity, boolean noTint) {
    private static final Codec<LayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      LAYER_COLOR_CODEC.optionalFieldOf("color", -1).forGetter(LayerData::color),
      ExtraCodecs.intRange(0, 15).optionalFieldOf("luminosity", 0).forGetter(LayerData::luminosity),
      Codec.BOOL.optionalFieldOf("no_tint", false).forGetter(LayerData::noTint)
    ).apply(instance, LayerData::new));

    private static final LayerData DEFAULT = new LayerData(-1, 0, false);
  }

  /** Native replacement for {@code loader: mantle:item_layer}. */
  public record ItemLayerUnbaked(Identifier parent, Map<String, Material> textures,
                                 List<LayerData> layers) implements ItemModel.Unbaked {
    public static final MapCodec<ItemLayerUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.optionalFieldOf("parent", Identifier.withDefaultNamespace("item/generated"))
        .forGetter(ItemLayerUnbaked::parent),
      Codec.unboundedMap(Codec.STRING, Material.CODEC).optionalFieldOf("textures", Map.of())
        .forGetter(ItemLayerUnbaked::textures),
      LayerData.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(ItemLayerUnbaked::layers)
    ).apply(instance, ItemLayerUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return bakeLayers(context, transformation, parent, textures, layers);
    }

    @Override
    public MapCodec<ItemLayerUnbaked> type() {
      return MAP_CODEC;
    }
  }

  /** Native replacement for {@code loader: mantle:nbt_key}. */
  public record NbtKeyUnbaked(Identifier parent, Map<String, Material> textures,
                              String nbtKey) implements ItemModel.Unbaked {
    public static final MapCodec<NbtKeyUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Identifier.CODEC.optionalFieldOf("parent", Identifier.withDefaultNamespace("item/generated"))
        .forGetter(NbtKeyUnbaked::parent),
      Codec.unboundedMap(Codec.STRING, Material.CODEC).optionalFieldOf("textures", Map.of())
        .forGetter(NbtKeyUnbaked::textures),
      Codec.STRING.fieldOf("nbt_key").forGetter(NbtKeyUnbaked::nbtKey)
    ).apply(instance, NbtKeyUnbaked::new));

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      Material defaultTexture = textures.get("default");
      Map<String, Material> fallbackTextures = defaultTexture == null
        ? Map.of() : Map.of("layer0", defaultTexture);
      ItemModel fallback = bakeLayers(context, transformation, parent, fallbackTextures, List.of());
      Map<String, ItemModel> variants = new java.util.HashMap<>();
      for (Map.Entry<String, Material> entry : textures.entrySet()) {
        if (!entry.getKey().equals("default")) {
          variants.put(entry.getKey(), bakeLayers(context, transformation, parent,
            Map.of("layer0", entry.getValue()), List.of()));
        }
      }
      return new NativeMantleItemModel(new DynamicNbtModel(nbtKey, fallback, Map.copyOf(variants)));
    }

    @Override
    public MapCodec<NbtKeyUnbaked> type() {
      return MAP_CODEC;
    }
  }
}
