package slimeknights.mantle.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4fc;
import slimeknights.mantle.Mantle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Native item model selecting a texture with any registered ItemKey. */
public final class ItemKeyModel implements ItemModel {
  public static final Identifier ID = Mantle.getResource("item_key");
  private static final Map<Identifier,Map<String,Material>> EXTRA_TEXTURES = new ConcurrentHashMap<>();
  private final ItemKey key;
  private final ItemModel fallback;
  private final Map<String,ItemModel> variants;

  public ItemKeyModel(ItemKey key, ItemModel fallback, Map<String,ItemModel> variants) {
    this.key = key;
    this.fallback = fallback;
    this.variants = Map.copyOf(variants);
  }

  public static void registerExtraTexture(Identifier key, String name, Identifier texture) {
    EXTRA_TEXTURES.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).put(name, new Material(texture));
  }

  @Override
  public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext display,
                     @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    variants.getOrDefault(key.getKey(stack), fallback).update(state, stack, resolver, display, level, owner, seed);
  }

  public record Unbaked(ItemKey key, Identifier parent, Map<String,Material> textures, Optional<Identifier> extraTexturesKey) implements ItemModel.Unbaked {
    private static final Codec<ItemKey> KEY_CODEC = new slimeknights.mantle.data.codec.LoadableCodec<>(ItemKey.LOADER);
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      KEY_CODEC.fieldOf("key").forGetter(Unbaked::key),
      Identifier.CODEC.optionalFieldOf("parent", Identifier.withDefaultNamespace("item/generated")).forGetter(Unbaked::parent),
      Codec.unboundedMap(Codec.STRING, Material.CODEC).fieldOf("textures").forGetter(Unbaked::textures),
      Identifier.CODEC.optionalFieldOf("extra_textures_key").forGetter(Unbaked::extraTexturesKey)
    ).apply(i, Unbaked::new));

    public Unbaked { textures = Map.copyOf(textures); }
    @Override public void resolveDependencies(Resolver resolver) { resolver.markDependency(parent); }
    @Override public MapCodec<Unbaked> type() { return MAP_CODEC; }
    @Override public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      Map<String,Material> allTextures = new java.util.HashMap<>();
      extraTexturesKey.ifPresent(id -> allTextures.putAll(EXTRA_TEXTURES.getOrDefault(id, Map.of())));
      allTextures.putAll(textures);
      Material defaultTexture = allTextures.remove("default");
      ItemModel fallback = NativeMantleItemModel.bakeLayers(context, transformation, parent,
        defaultTexture == null ? Map.of() : Map.of("layer0", defaultTexture), List.of());
      Map<String,ItemModel> variants = new java.util.HashMap<>();
      allTextures.forEach((name, material) -> variants.put(name,
        NativeMantleItemModel.bakeLayers(context, transformation, parent, Map.of("layer0", material), List.of())));
      return new ItemKeyModel(key, fallback, variants);
    }
  }
}
