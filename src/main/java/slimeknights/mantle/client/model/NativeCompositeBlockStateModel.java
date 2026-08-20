package slimeknights.mantle.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Native 26.1 replacement for Forge's inline composite block model loader. */
public final class NativeCompositeBlockStateModel {
  private NativeCompositeBlockStateModel() {}

  /** Codec payload used by blockstate variants with {@code type:mantle:composite}. */
  public record Unbaked(
      Map<String,Identifier> children,
      Optional<Identifier> particle,
      Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.unboundedMap(Codec.STRING, Identifier.CODEC).fieldOf("children").forGetter(Unbaked::children),
        Identifier.CODEC.optionalFieldOf("particle").forGetter(Unbaked::particle),
        Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::modelState)
    ).apply(instance, Unbaked::new));

    public Unbaked {
      children = Map.copyOf(children);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      children.values().forEach(resolver::markDependency);
    }

    @Override
    public BlockStateModel bake(ModelBaker modelBaker) {
      ModelState state = modelState.asModelState();
      List<BlockStateModelPart> parts = new ArrayList<>(children.size());
      for (Identifier child : children.values()) {
        ResolvedModel resolved = modelBaker.getModel(child);
        parts.add(SimpleModelWrapper.bake(modelBaker, resolved, state));
      }
      Optional<Material.Baked> bakedParticle = particle.flatMap(texture -> children.values().stream().findFirst()
        .map(child -> modelBaker.materials().get(new Material(texture), modelBaker.getModel(child))));
      return new Baked(parts, bakedParticle);
    }
  }

  private static final class Baked implements BlockStateModel {
    private final List<BlockStateModelPart> parts;
    private final Material.Baked particle;
    private final int materialFlags;

    private Baked(List<BlockStateModelPart> parts, Optional<Material.Baked> explicitParticle) {
      if (parts.isEmpty()) {
        throw new IllegalArgumentException("A Mantle composite block model must contain at least one child");
      }
      this.parts = List.copyOf(parts);
      this.particle = explicitParticle.orElseGet(() -> parts.getFirst().particleMaterial());
      this.materialFlags = parts.stream().mapToInt(BlockStateModelPart::materialFlags).reduce(0, (left, right) -> left | right);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
      output.addAll(parts);
    }

    @Override
    public Material.Baked particleMaterial() {
      return particle;
    }

    @Override
    public int materialFlags() {
      return materialFlags;
    }
  }
}
