package slimeknights.mantle.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Native 26.1 replacement for Mantle's colored block model loader. */
public final class NativeColoredBlockStateModel {
  private NativeColoredBlockStateModel() {}

  /** Codec payload used by blockstate variants with {@code type:mantle:colored_block}. */
  public record Unbaked(
      Identifier model,
      List<NativeBlockColorData> colors,
      Variant.SimpleModelState modelState
  ) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
        NativeBlockColorData.CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(Unbaked::colors),
        Variant.SimpleModelState.MAP_CODEC.forGetter(Unbaked::modelState)
    ).apply(instance, Unbaked::new));

    public Unbaked {
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
      ModelState state = modelState.asModelState();
      UnbakedGeometry geometry = resolved.getTopGeometry();
      if (!(geometry instanceof UnbakedCuboidGeometry cuboidGeometry)) {
        return new SingleVariant(SimpleModelWrapper.bake(modelBaker, resolved, state));
      }

      List<CuboidModelElement> colored = applyColors(cuboidGeometry.elements(), colors);
      TextureSlots textures = resolved.getTopTextureSlots();
      QuadCollection quads = new UnbakedCuboidGeometry(colored).bake(
          textures, modelBaker, state, resolved, resolved.getTopAdditionalProperties()
      );
      return new SingleVariant(new SimpleModelWrapper(
          quads,
          resolved.getTopAmbientOcclusion(),
          resolved.resolveParticleMaterial(textures, modelBaker)
      ));
    }
  }

  private static List<CuboidModelElement> applyColors(
      List<CuboidModelElement> elements, List<NativeBlockColorData> colors
  ) {
    List<CuboidModelElement> result = new ArrayList<>(elements.size());
    for (int index = 0; index < elements.size(); index++) {
      CuboidModelElement element = elements.get(index);
      NativeBlockColorData colorData = NativeBlockColorData.at(colors, index);
      Map<net.minecraft.core.Direction,CuboidFace> faces = new EnumMap<>(net.minecraft.core.Direction.class);
      for (Map.Entry<net.minecraft.core.Direction,CuboidFace> entry : element.faces().entrySet()) {
        CuboidFace face = entry.getValue();
        ExtraFaceData faceData = colorData.applyTo(face.faceData());
        faces.put(entry.getKey(), new CuboidFace(
            face.cullForDirection(), face.tintIndex(), face.texture(), face.uvs(), face.rotation(),
            faceData, new MutableObject<>()
        ));
      }
      ExtraFaceData elementData = colorData.applyTo(element.faceData());
      result.add(new CuboidModelElement(
          element.from(), element.to(), faces, element.rotation(), element.shade(), element.lightEmission(), elementData
      ));
    }
    return result;
  }
}
