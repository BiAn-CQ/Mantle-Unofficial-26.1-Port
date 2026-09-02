package slimeknights.mantle.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Color and luminosity data used by Mantle's block model loaders.
 *
 * <p>The old loaders attached this information to each cuboid element.  The
 * 26.1 cuboid baker accepts the same information through {@link
 * ExtraFaceData}, so keeping the conversion in one small value object avoids
 * subtly different color parsing between connected and colored blocks.</p>
 */
public record NativeBlockColorData(int color, int luminosity, @Nullable Boolean uvlock) {
  private static final Codec<Integer> COLOR_CODEC = ExtraCodecs.STRING_ARGB_COLOR;

  public static final NativeBlockColorData DEFAULT = new NativeBlockColorData(-1, -1, null);

  /** Codec for Mantle {@code colors} array entries. */
  public static final Codec<NativeBlockColorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      COLOR_CODEC.optionalFieldOf("color", -1).forGetter(NativeBlockColorData::color),
      ExtraCodecs.intRange(-1, 15).optionalFieldOf("luminosity", -1).forGetter(NativeBlockColorData::luminosity),
      Codec.BOOL.optionalFieldOf("uvlock").forGetter(data -> Optional.ofNullable(data.uvlock()))
  ).apply(instance, (color, luminosity, uvlock) -> new NativeBlockColorData(color, luminosity, uvlock.orElse(null))));

  /** Returns the entry for an element, or the default when absent. */
  public static NativeBlockColorData at(List<NativeBlockColorData> colors, int elementIndex) {
    return elementIndex >= 0 && elementIndex < colors.size() ? colors.get(elementIndex) : DEFAULT;
  }

  /** Applies only the fields explicitly supplied by the color entry. */
  public ExtraFaceData applyTo(@Nullable ExtraFaceData original) {
    ExtraFaceData base = original == null ? ExtraFaceData.DEFAULT : original;
    return new ExtraFaceData(
        color == -1 ? base.color() : color,
        luminosity == -1 ? base.lightEmission() : luminosity,
        base.ambientOcclusion()
    );
  }

  /** Gets the UV-lock override, falling back to the model-state value. */
  public boolean isUvLock(boolean defaultValue) {
    return uvlock == null ? defaultValue : uvlock;
  }
}
