package slimeknights.mantle.client.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.util.RetexturedHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native 26.1 replacement for Mantle's block {@code retextured} model loader.
 *
 * <p>The old loader changed the material for selected cuboid texture slots at
 * render time using {@link RetexturedHelper#BLOCK_PROPERTY}.  The modern
 * block-state model API has no legacy baked-model wrapper, so this adapter
 * keeps the same data contract while caching one native part per texture
 * block.</p>
 */
public final class NativeRetexturedBlockStateModel {
  private NativeRetexturedBlockStateModel() {}

  /** Codec payload used by blockstate variants with {@code type:mantle:retextured}. */
  public record Unbaked(
      Identifier model,
      List<String> retexturedSlots,
      List<NativeBlockColorData> colors,
      Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
        com.mojang.serialization.Codec.STRING.listOf().optionalFieldOf("retextured_slots", List.of()).forGetter(Unbaked::retexturedSlots),
        NativeBlockColorData.CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(Unbaked::colors),
        Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::modelState)
    ).apply(instance, Unbaked::new));

    public Unbaked {
      retexturedSlots = List.copyOf(retexturedSlots);
      colors = List.copyOf(colors);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public BlockStateModel bake(ModelBaker modelBaker) {
      return new Baked(this, modelBaker);
    }
  }

  private static final class Baked implements BlockStateModel {
    private final ModelBaker baker;
    private final ResolvedModel resolved;
    private final ModelState modelState;
    private final BlockStateModelPart fallback;
    @Nullable
    private final UnbakedCuboidGeometry geometry;
    private final TextureSlots baseTextures;
    private final TextureSlots.Data baseTextureData;
    private final Set<String> slots;
    private final List<NativeBlockColorData> colors;
    private final Map<Block,BlockStateModelPart> cache = new ConcurrentHashMap<>();

    private Baked(NativeRetexturedBlockStateModel.Unbaked definition, ModelBaker baker) {
      this.baker = baker;
      this.resolved = baker.getModel(definition.model);
      this.modelState = definition.modelState.asModelState();
      this.fallback = SimpleModelWrapper.bake(baker, resolved, modelState);
      this.geometry = resolved.getTopGeometry() instanceof UnbakedCuboidGeometry cuboids ? cuboids : null;
      this.baseTextures = resolved.getTopTextureSlots();
      this.baseTextureData = copyBaseTextureData();
      this.slots = definition.retexturedSlots.stream().map(Baked::stripReference).collect(java.util.stream.Collectors.toUnmodifiableSet());
      this.colors = List.copyOf(definition.colors);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
      output.add(fallback);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
      output.add(texture == null || texture == net.minecraft.world.level.block.Blocks.AIR || slots.isEmpty()
                 ? fallback : cache.computeIfAbsent(texture, this::bakeTexture));
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
      return texture == null || texture == net.minecraft.world.level.block.Blocks.AIR || slots.isEmpty()
             ? fallback : texture;
    }

    @Override
    public Material.Baked particleMaterial() {
      return fallback.particleMaterial();
    }

    @Override
    public int materialFlags() {
      return fallback.materialFlags();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
      return texture == null || texture == net.minecraft.world.level.block.Blocks.AIR || slots.isEmpty()
             ? fallback.particleMaterial() : cache.computeIfAbsent(texture, this::bakeTexture).particleMaterial();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      Block texture = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
      return texture == null || texture == net.minecraft.world.level.block.Blocks.AIR || slots.isEmpty()
             ? fallback.materialFlags() : cache.computeIfAbsent(texture, this::bakeTexture).materialFlags();
    }

    private BlockStateModelPart bakeTexture(Block texture) {
      if (geometry == null) {
        return fallback;
      }
      Identifier particle = ModelHelper.getParticleTexture(texture);
      Map<String,Material> replacements = new java.util.HashMap<>();
      for (String slot : slots) {
        if (baseTextures.getMaterial(slot) != null) {
          replacements.put(slot, new Material(particle));
        }
      }
      if (replacements.isEmpty()) {
        return fallback;
      }
      List<CuboidModelElement> elements = copyElements(geometry.elements());
      TextureSlots.Data.Builder overrideData = new TextureSlots.Data.Builder();
      replacements.forEach(overrideData::addTexture);
      TextureSlots textures = new TextureSlots.Resolver()
          // TextureSlots resolves from the end of the list.  Keep the base
          // first so the block-specific replacement wins over its original
          // material rather than being overwritten by it.
          .addLast(overrideData.build())
          .addLast(baseTextureData)
          .resolve(resolved);
      var quads = new UnbakedCuboidGeometry(elements).bake(
          textures, baker, modelState, resolved, resolved.getTopAdditionalProperties()
      );
      return new SimpleModelWrapper(quads, resolved.getTopAmbientOcclusion(), resolved.resolveParticleMaterial(textures, baker));
    }

    private List<CuboidModelElement> copyElements(List<CuboidModelElement> source) {
      List<CuboidModelElement> result = new ArrayList<>(source.size());
      for (int index = 0; index < source.size(); index++) {
        CuboidModelElement element = source.get(index);
        NativeBlockColorData colorData = NativeBlockColorData.at(colors, index);
        Map<Direction,CuboidFace> faces = new EnumMap<>(Direction.class);
        element.faces().forEach((direction, face) -> faces.put(direction, copyFace(face, colorData.applyTo(face.faceData()))));
        result.add(new CuboidModelElement(
            element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), colorData.applyTo(element.faceData())
        ));
      }
      return result;
    }

    private static CuboidFace copyFace(CuboidFace face, @Nullable ExtraFaceData data) {
      return new CuboidFace(
          face.cullForDirection(), face.tintIndex(), face.texture(), face.uvs(), face.rotation(), data, new MutableObject<>()
      );
    }

    private TextureSlots.Data copyBaseTextureData() {
      TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
      if (geometry != null) {
        for (CuboidModelElement element : geometry.elements()) {
          for (CuboidFace face : element.faces().values()) {
            String slot = stripReference(face.texture());
            Material material = baseTextures.getMaterial(slot);
            if (material != null) {
              builder.addTexture(slot, material);
            }
          }
        }
      }
      Material particle = baseTextures.getMaterial("particle");
      if (particle != null) {
        builder.addTexture("particle", particle);
      }
      return builder.build();
    }

    private static String stripReference(String texture) {
      return texture.startsWith("#") ? texture.substring(1) : texture;
    }
  }
}
