package slimeknights.mantle.client.model.connected;

import com.mojang.datafixers.util.Either;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.apache.commons.lang3.mutable.MutableObject;
import slimeknights.mantle.client.model.NativeBlockColorData;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/** Native 26.1 connected-texture block-state model. */
public final class NativeConnectedBlockStateModel {
  private NativeConnectedBlockStateModel() {}

  /** Codec payload used by blockstate variants with {@code type:mantle:connected}. */
  public record Unbaked(
      Identifier model,
      Map<String,String> textures,
      String predicate,
      List<String> sides,
      List<NativeBlockColorData> colors,
      Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("textures").forGetter(Unbaked::textures),
        Codec.STRING.optionalFieldOf("predicate", "block").forGetter(Unbaked::predicate),
        Codec.STRING.listOf().optionalFieldOf("sides", List.of()).forGetter(Unbaked::sides),
        NativeBlockColorData.CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(Unbaked::colors),
        Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::modelState)
    ).apply(instance, Unbaked::new));

    public Unbaked {
      textures = Map.copyOf(textures);
      sides = List.copyOf(sides);
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
      ResolvedModel resolved = modelBaker.getModel(model);
      ModelState state = this.modelState.asModelState();
      UnbakedGeometry geometry = resolved.getTopGeometry();
      if (!(geometry instanceof UnbakedCuboidGeometry cuboidGeometry)) {
        // Connected textures are element-face based. A non-cuboid third-party
        // model remains renderable through the normal 26.1 model path.
        return new SingleVariant(SimpleModelWrapper.bake(modelBaker, resolved, state));
      }

      Set<Direction> connectionSides = parseSides(sides);
      BiPredicate<BlockState,BlockState> connectionPredicate = ConnectedModelRegistry.getPredicate(predicate);
      return bakeConnected(
          modelBaker, resolved, cuboidGeometry, state, textures, colors, connectionSides, connectionPredicate
      );
    }
  }

  private static Set<Direction> parseSides(List<String> sideNames) {
    if (sideNames.isEmpty()) {
      return EnumSet.allOf(Direction.class);
    }
    EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
    for (String sideName : sideNames) {
      Direction side = Direction.byName(sideName);
      if (side == null) {
        throw new IllegalArgumentException("Invalid connected model side " + sideName);
      }
      result.add(side);
    }
    return result;
  }

  private static BlockStateModel bakeConnected(
      ModelBaker modelBaker,
      ResolvedModel resolved,
      UnbakedCuboidGeometry geometry,
      ModelState modelState,
      Map<String,String> connectedTextures,
      List<NativeBlockColorData> colors,
      Set<Direction> sides,
      BiPredicate<BlockState,BlockState> connectionPredicate
  ) {
    TextureSlots baseSlots = resolved.getTopTextureSlots();
    Map<String,Material> materials = collectMaterials(geometry, baseSlots);
    Map<String,String[]> suffixes = new HashMap<>(connectedTextures.size());
    for (Map.Entry<String,String> entry : connectedTextures.entrySet()) {
      suffixes.put(entry.getKey(), ConnectedModelRegistry.getTextureSuffixes(entry.getValue()));
    }

    BlockStateModelPart[] parts = new BlockStateModelPart[64];
    int materialFlags = 0;
    boolean ambientOcclusion = resolved.getTopAmbientOcclusion();
    Material.Baked particle = resolved.resolveParticleMaterial(baseSlots, modelBaker);
    for (int connection = 0; connection < parts.length; connection++) {
      List<CuboidModelElement> elements = remapElements(geometry.elements(), connectedTextures, suffixes, colors, (byte)connection);
      TextureSlots slots = createTextureSlots(materials, suffixes, connection, resolved);
      // Bake the remapped element list. Calling the original geometry here
      // would silently discard the per-face suffix substitutions.
      QuadCollection quads = new UnbakedCuboidGeometry(elements).bake(
          slots, modelBaker, modelState, resolved, resolved.getTopAdditionalProperties()
      );
      parts[connection] = new SimpleModelWrapper(quads, ambientOcclusion, particle);
      materialFlags |= quads.materialFlags();
    }
    return new Baked(parts, particle, materialFlags, sides, connectionPredicate, modelState.transformation());
  }

  private static Map<String,Material> collectMaterials(UnbakedCuboidGeometry geometry, TextureSlots baseSlots) {
    Map<String,Material> materials = new HashMap<>();
    for (CuboidModelElement element : geometry.elements()) {
      for (CuboidFace face : element.faces().values()) {
        String slot = stripReference(face.texture());
        Material material = baseSlots.getMaterial(slot);
        if (material != null) {
          materials.putIfAbsent(slot, material);
        }
      }
    }
    Material particle = baseSlots.getMaterial("particle");
    if (particle != null) {
      materials.putIfAbsent("particle", particle);
    }
    return materials;
  }

  private static TextureSlots createTextureSlots(
      Map<String,Material> materials,
      Map<String,String[]> suffixes,
      int connection,
      ResolvedModel debugName
  ) {
    TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
    materials.forEach(builder::addTexture);
    for (Map.Entry<String,String[]> entry : suffixes.entrySet()) {
      Material base = materials.get(entry.getKey());
      if (base == null) {
        continue;
      }
      for (String suffix : entry.getValue()) {
        if (!suffix.isEmpty()) {
          builder.addTexture(entry.getKey() + "_" + suffix, withSuffix(base, suffix));
        }
      }
    }
    return new TextureSlots.Resolver().addLast(builder.build()).resolve(debugName);
  }

  private static Material withSuffix(Material base, String suffix) {
    Identifier texture = base.sprite();
    Identifier suffixed = Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "/" + suffix);
    return new Material(suffixed, base.forceTranslucent());
  }

  private static List<CuboidModelElement> remapElements(
      List<CuboidModelElement> elements,
      Map<String,String> connectedTextures,
      Map<String,String[]> suffixes,
      List<NativeBlockColorData> colors,
      byte connections
  ) {
    return java.util.stream.IntStream.range(0, elements.size()).mapToObj(index -> {
      CuboidModelElement element = elements.get(index);
      NativeBlockColorData colorData = NativeBlockColorData.at(colors, index);
      Map<Direction,CuboidFace> faces = new EnumMap<>(Direction.class);
      for (Map.Entry<Direction,CuboidFace> entry : element.faces().entrySet()) {
        Direction faceDirection = entry.getKey();
        CuboidFace original = entry.getValue();
        ExtraFaceData faceData = colorData.applyTo(original.faceData());
        String remappedTexture = original.texture();
        String slot = stripReference(original.texture());
        String connectedType = connectedTextures.get(slot);
        if (connectedType != null) {
          String[] table = suffixes.get(slot);
          String suffix = getTextureSuffix(table, connections, getTransform(faceDirection, original));
          if (!suffix.isEmpty()) {
            remappedTexture = "#" + slot + "_" + suffix;
          }
        }
        CuboidFace remapped = new CuboidFace(
            original.cullForDirection(), original.tintIndex(), remappedTexture,
            original.uvs(), original.rotation(), faceData, new MutableObject<>()
        );
        faces.put(faceDirection, remapped);
      }
      ExtraFaceData elementFaceData = colorData.applyTo(element.faceData());
      return new CuboidModelElement(
          element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), elementFaceData
      );
    }).toList();
  }

  private static String stripReference(String texture) {
    return texture.startsWith("#") ? texture.substring(1) : texture;
  }

  private static Function<Direction,Direction> getTransform(Direction face, CuboidFace cuboidFace) {
    Function<Direction,Direction> transform = direction -> rotateDirection(direction, face);
    CuboidFace.UVs uv = cuboidFace.uvs();
    if (uv != null) {
      boolean flipV = uv.minV() > uv.maxV();
      if (uv.minU() > uv.maxU()) {
        if (flipV) {
          transform = transform.compose(Direction::getOpposite);
        } else {
          transform = transform.compose(direction -> direction.getAxis() == Direction.Axis.X ? direction.getOpposite() : direction);
        }
      } else if (flipV) {
        transform = transform.compose(direction -> direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction);
      }
    }
    return switch (cuboidFace.rotation()) {
      case R90 -> transform.compose(Direction::getClockWise);
      case R180 -> transform.compose(Direction::getOpposite);
      case R270 -> transform.compose(Direction::getCounterClockWise);
      default -> transform;
    };
  }

  private static Direction rotateDirection(Direction direction, Direction rotation) {
    if (rotation == Direction.UP) {
      return direction;
    }
    if (rotation == Direction.DOWN) {
      return direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction;
    }
    return switch (direction) {
      case NORTH -> Direction.UP;
      case SOUTH -> Direction.DOWN;
      case EAST -> rotation.getCounterClockWise();
      case WEST -> rotation.getClockWise();
      default -> throw new IllegalArgumentException("Direction must be horizontal axis");
    };
  }

  private static String getTextureSuffix(String[] suffixes, byte connections, Function<Direction,Direction> transform) {
    int key = 0;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      int flag = 1 << transform.apply(direction).get3DDataValue();
      if ((connections & flag) == flag) {
        key |= 1 << direction.get2DDataValue();
      }
    }
    return suffixes[key];
  }

  private static final class Baked implements BlockStateModel {
    private final BlockStateModelPart[] parts;
    private final Material.Baked particle;
    private final int materialFlags;
    private final Set<Direction> sides;
    private final BiPredicate<BlockState,BlockState> connectionPredicate;
    private final Transformation transformation;

    private Baked(
        BlockStateModelPart[] parts,
        Material.Baked particle,
        int materialFlags,
        Set<Direction> sides,
        BiPredicate<BlockState,BlockState> connectionPredicate,
        Transformation transformation
    ) {
      this.parts = parts;
      this.particle = particle;
      this.materialFlags = materialFlags;
      this.sides = Set.copyOf(sides);
      this.connectionPredicate = connectionPredicate;
      this.transformation = transformation;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
      output.add(parts[0]);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      output.add(parts[getConnections(level, pos, state)]);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return getConnections(level, pos, state);
    }

    @Override
    public Material.Baked particleMaterial() {
      return particle;
    }

    @Override
    public int materialFlags() {
      return materialFlags;
    }

    private int getConnections(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      int connections = 0;
      for (Direction direction : Direction.values()) {
        if (sides.contains(direction)
            && connectionPredicate.test(state, level.getBlockState(pos.relative(transformation.rotateTransform(direction))))) {
          connections |= 1 << direction.get3DDataValue();
        }
      }
      return connections;
    }
  }
}
