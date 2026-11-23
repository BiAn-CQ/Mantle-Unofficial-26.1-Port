package slimeknights.mantle.client.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.fluid.texture.FluidTextureManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Native component-aware fluid-container item model. */
public final class FluidContainerModel {
  public static final Identifier ID = slimeknights.mantle.Mantle.getResource("fluid_container");
  private static final Identifier DEFAULT_PARENT = Identifier.withDefaultNamespace("item/generated");
  private static final Transformation FLUID_TRANSFORM = new Transformation(
    new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

  private FluidContainerModel() {}

  /** Optional explicit fluid, supporting a compact ID and native component data. */
  public record FluidDefinition(Identifier name, net.minecraft.core.component.DataComponentPatch components, boolean optional) {
    public FluidDefinition(Identifier name, net.minecraft.core.component.DataComponentPatch components) { this(name, components, false); }
    private static final com.mojang.serialization.Codec<FluidDefinition> FULL_CODEC = new MapCodec<FluidDefinition>() {
      @Override public <T> com.mojang.serialization.DataResult<FluidDefinition> decode(com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
        return Identifier.CODEC.parse(ops, input.get("name")).flatMap(name -> {
          T flag = input.get("optional");
          var optional = flag == null ? com.mojang.serialization.DataResult.success(false) : com.mojang.serialization.Codec.BOOL.parse(ops, flag);
          return optional.flatMap(isOptional -> {
            // An absent optional fluid must not require its component types either.
            if (isOptional && !BuiltInRegistries.FLUID.containsKey(name)) {
              return com.mojang.serialization.DataResult.success(new FluidDefinition(name, net.minecraft.core.component.DataComponentPatch.EMPTY, true));
            }
            T components = input.get("components");
            var patch = components == null ? com.mojang.serialization.DataResult.success(net.minecraft.core.component.DataComponentPatch.EMPTY)
              : net.minecraft.core.component.DataComponentPatch.CODEC.parse(ops, components);
            return patch.map(value -> new FluidDefinition(name, value, isOptional));
          });
        });
      }
      @Override public <T> com.mojang.serialization.RecordBuilder<T> encode(FluidDefinition value, com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.RecordBuilder<T> prefix) {
        prefix.add("name", Identifier.CODEC.encodeStart(ops, value.name()));
        if (!value.components().isEmpty()) prefix.add("components", net.minecraft.core.component.DataComponentPatch.CODEC.encodeStart(ops, value.components()));
        if (value.optional()) prefix.add("optional", ops.createBoolean(true));
        return prefix;
      }
      @Override public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
        return java.util.stream.Stream.of("name", "components", "optional").map(ops::createString);
      }
    }.codec();
    public static final com.mojang.serialization.Codec<FluidDefinition> CODEC = com.mojang.serialization.Codec.either(Identifier.CODEC, FULL_CODEC)
      .xmap(e -> e.map(id -> new FluidDefinition(id, net.minecraft.core.component.DataComponentPatch.EMPTY), v -> v),
        v -> v.components().isEmpty() && !v.optional() ? com.mojang.datafixers.util.Either.left(v.name()) : com.mojang.datafixers.util.Either.right(v))
      .validate(value -> value.optional() || BuiltInRegistries.FLUID.containsKey(value.name())
        ? com.mojang.serialization.DataResult.success(value)
        : com.mojang.serialization.DataResult.error(() -> "Unknown fluid " + value.name()));
  }

  /** Model definition corresponding to {@code type: mantle:fluid_container}. */
  public record Unbaked(
      Identifier parent,
      Map<String,Material> textures,
      Optional<FluidDefinition> fluid,
      boolean flipGas,
      Optional<Identifier> textureOverrides
  ) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("parent", DEFAULT_PARENT).forGetter(Unbaked::parent),
        com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, Material.CODEC)
            .optionalFieldOf("textures", Map.of()).forGetter(Unbaked::textures),
        FluidDefinition.CODEC.optionalFieldOf("fluid").forGetter(Unbaked::fluid),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("flip_gas", true).forGetter(Unbaked::flipGas),
        Identifier.CODEC.optionalFieldOf("texture_overrides").forGetter(Unbaked::textureOverrides)
    ).apply(instance, Unbaked::new));

    public Unbaked(Identifier parent, Map<String,Material> textures, Optional<FluidDefinition> fluid, boolean flipGas) {
      this(parent, textures, fluid, flipGas, Optional.empty());
    }

    public Unbaked {
      textures = Map.copyOf(textures);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(parent);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      // Registry components and the fluid model set are ready when an item is
      // rendered, after the initial resource-bake phase has completed.
      return new Dynamic(this, context, transformation);
    }

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }
  }

  private static ItemModel bakeContainer(ItemModel.BakingContext context, Matrix4fc transformation,
                                          Unbaked definition, FluidStack fluid) {
    ModelBaker baker = context.blockModelBaker();
    ResolvedModel parent = baker.getModel(definition.parent());
    Material base = firstMaterial(definition.textures(), parent, "base", "layer0");
    Material fluidMask = firstMaterial(definition.textures(), parent, "fluid", "layer1");
    ModelState state = BlockModelRotation.IDENTITY;
    if (definition.flipGas() && !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir()) {
      state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
    }

    QuadCollection.Builder quads = new QuadCollection.Builder();
    Material.Baked particle = null;
    List<net.minecraft.client.color.item.ItemTintSource> tints = List.of();
    if (base != null) {
      Material.Baked baked = baker.materials().get(base, parent);
      particle = baked;
      QuadCollection generated = baker.compute(new ItemModelGenerator.ItemLayerKey(
          baked, state, -1,
          new ExtraFaceData(0xFFFFFFFF, 0, true)
      ));
      generated.getAll().forEach(quads::addUnculledFace);
    }

    Material.Baked override = null;
    if (!fluid.isEmpty() && definition.fluid().isEmpty() && definition.textureOverrides().isPresent()) {
      Map<String,Identifier> overrides = TextureMapManager.INSTANCE.getTextureMap(Minecraft.getInstance().getResourceManager(), definition.textureOverrides().get());
      Identifier sprite = overrides.get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
      if (sprite != null) override = baker.materials().get(new Material(sprite), parent);
    }
    if (!fluid.isEmpty() && (fluidMask != null || override != null)) {
      Material.Baked mask = fluidMask == null ? null : baker.materials().get(fluidMask, parent);
      FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
      Material.Baked baked = override == null ? fluidModel.stillMaterial() : override;
      if (particle == null) {
        particle = baked;
      }
      int color = fluidColor(fluid);
      tints = override == null ? List.of(new Constant(color)) : List.of();
      int light = fluid.getFluid().getFluidType().getLightLevel(fluid);
      ModelState fluidState = new ComposedModelState(state, FLUID_TRANSFORM);
      QuadCollection generated = override == null
        ? UnbakedElementsHelper.bakeItemMaskQuads(baker, 0, mask, baked, fluidState, new ExtraFaceData(0xFFFFFFFF, light, true))
        : baker.compute(new ItemModelGenerator.ItemLayerKey(baked, fluidState, -1, new ExtraFaceData(0xFFFFFFFF, light, true)));
      generated.getAll().forEach(quads::addUnculledFace);
    }

    if (particle == null) {
      particle = parent.resolveParticleMaterial(parent.getTopTextureSlots(), baker);
    }
    ModelRenderProperties parentProperties = ModelRenderProperties.fromResolvedModel(
        baker, parent, parent.getTopTextureSlots()
    );
    ModelRenderProperties properties = new ModelRenderProperties(
        false, particle, parentProperties.transforms()
    );
    return new CuboidItemModelWrapper(tints, quads.build(), properties, transformation);
  }

  private static Material firstMaterial(Map<String,Material> explicit, ResolvedModel parent, String... names) {
    for (String name : names) {
      Material material = explicit.get(name);
      if (material != null) {
        return material;
      }
      material = parent.getTopTextureSlots().getMaterial(name);
      if (material != null) {
        return material;
      }
    }
    return null;
  }

  private static int fluidColor(FluidStack fluid) {
    FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
        .get(fluid.getFluid().defaultFluidState());
    if (model.fluidTintSource() != null) {
      return model.fluidTintSource().colorAsStack(fluid);
    }
    return FluidTextureManager.getColor(fluid.getFluid().getFluidType());
  }

  private static final class Dynamic implements ItemModel {
    private final FluidContainerModel.Unbaked definition;
    private final ItemModel.BakingContext context;
    private final Matrix4fc transformation;
    private ItemModel emptyModel;
    private final Map<FluidKey,ItemModel> cache = new ConcurrentHashMap<>();

    private Dynamic(FluidContainerModel.Unbaked definition, ItemModel.BakingContext context, Matrix4fc transformation) {
      this.definition = definition;
      this.context = context;
      this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
      FluidStack contained;
      if (definition.fluid().isPresent()) {
        FluidDefinition explicit = definition.fluid().get();
        var value = explicit.optional() && !BuiltInRegistries.FLUID.containsKey(explicit.name())
          ? net.minecraft.world.level.material.Fluids.LAVA : BuiltInRegistries.FLUID.getValue(explicit.name());
        contained = value == null || value == net.minecraft.world.level.material.Fluids.EMPTY ? FluidStack.EMPTY
          : slimeknights.mantle.util.DataComponentHelper.makeStack(value, FluidType.BUCKET_VOLUME, explicit.components());
      } else {
        contained = net.neoforged.neoforge.transfer.fluid.FluidUtil.getFirstStackContained(stack);
      }
      if (contained.isEmpty()) {
        if (emptyModel == null) emptyModel = bakeContainer(context, transformation, definition, FluidStack.EMPTY);
        emptyModel.update(output, stack, resolver, displayContext, level, owner, seed);
        return;
      }
      FluidStack fluid = contained.copy();
      fluid.setAmount(FluidType.BUCKET_VOLUME);
      FluidKey key = new FluidKey(fluid);
      cache.computeIfAbsent(key, value -> bakeContainer(context, transformation, definition, value.fluid()))
          .update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }

  /** Stable key for a contained fluid; FluidStack itself is mutable and has identity equality. */
  private static final class FluidKey {
    private final FluidStack fluid;

    private FluidKey(FluidStack fluid) {
      this.fluid = fluid;
    }

    private FluidStack fluid() {
      return fluid;
    }

    @Override
    public boolean equals(Object object) {
      return this == object || object instanceof FluidKey other
          && FluidStack.isSameFluidSameComponents(fluid, other.fluid);
    }

    @Override
    public int hashCode() {
      return FluidStack.hashFluidAndComponents(fluid);
    }
  }
}
