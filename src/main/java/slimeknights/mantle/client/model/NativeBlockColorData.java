package slimeknights.mantle.client.model;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Color and legacy luminosity data used by Mantle's block model loaders.
 *
 * <p>The old loaders attached this information to each cuboid element.  The
 * 26.1 cuboid baker accepts the same information through {@link
 * ExtraFaceData}, so keeping the conversion in one small value object avoids
 * subtly different color parsing between connected and colored blocks.</p>
 */
public record NativeBlockColorData(int color, int luminosity, @Nullable Boolean uvlock) {
  private static final Codec<Integer> COLOR_CODEC = Codec.either(
      ExtraCodecs.STRING_ARGB_COLOR,
      Codec.STRING.flatXmap(NativeBlockColorData::parseLegacyColor,
          color -> DataResult.success(String.format(Locale.ROOT, "%08X", color)))
  ).xmap(either -> either.map(value -> value, value -> value), Either::left);

  public static final NativeBlockColorData DEFAULT = new NativeBlockColorData(-1, -1, null);

  /** Codec for the legacy Mantle {@code colors} array entries. */
  public static final Codec<NativeBlockColorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      COLOR_CODEC.optionalFieldOf("color", -1).forGetter(NativeBlockColorData::color),
      ExtraCodecs.intRange(-1, 15).optionalFieldOf("luminosity", -1).forGetter(NativeBlockColorData::luminosity),
      Codec.BOOL.optionalFieldOf("uvlock").forGetter(data -> Optional.ofNullable(data.uvlock()))
  ).apply(instance, (color, luminosity, uvlock) -> new NativeBlockColorData(color, luminosity, uvlock.orElse(null))));

  /** Returns the entry for an element, or the legacy default when absent. */
  public static NativeBlockColorData at(List<NativeBlockColorData> colors, int elementIndex) {
    return elementIndex >= 0 && elementIndex < colors.size() ? colors.get(elementIndex) : DEFAULT;
  }

  /** Applies only the fields explicitly supplied by the legacy color entry. */
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

  private static DataResult<Integer> parseLegacyColor(String value) {
    String hex = value.startsWith("#") ? value.substring(1) : value;
    if (hex.length() == 6) {
      hex = "FF" + hex;
    }
    if (hex.length() != 8) {
      return DataResult.error(() -> "Expected 6 or 8 hexadecimal color digits: " + value);
    }
    try {
      return DataResult.success((int) Long.parseLong(hex, 16));
    } catch (NumberFormatException exception) {
      return DataResult.error(() -> "Invalid hexadecimal color: " + value);
    }
  }
}
