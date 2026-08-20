// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/TemplateWorld.java
package slimeknights.mantle.client.book.structure.level;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * World implementation for the book structures
 */
public class TemplateLevel extends Level implements BlockAndTintGetter {

  private final Scoreboard scoreboard = new Scoreboard();
  private final TemplateChunkSource chunkSource;

  public TemplateLevel(List<StructureBlockInfo> blocks, Predicate<BlockPos> shouldShow) {
    super(
      new FakeLevelData(), Level.OVERWORLD, Objects.requireNonNull(Minecraft.getInstance().level).registryAccess(),
      Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
      true, false, 0, 0
    );

    this.chunkSource = new TemplateChunkSource(blocks, this, shouldShow);
  }

  @Override
  public void sendBlockUpdated(@Nonnull BlockPos pos, @Nonnull BlockState oldState, @Nonnull BlockState newState, int flags) {}

  @Override
  public void playSeededSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}

  @Override
  public void playSeededSound(@Nullable Entity except, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}

  @Override
  public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator,
                      double x, double y, double z, float radius, boolean fire, ExplosionInteraction interaction,
                      ParticleOptions smallParticles, ParticleOptions largeParticles,
                      WeightedList<ExplosionParticleInfo> blockParticles, Holder<SoundEvent> explosionSound) {}

  @Override
  public String gatherChunkSourceStats() {
    return chunkSource.gatherStats();
  }

  @Override
  public int getSeaLevel() {
    return 63;
  }

  @Override
  public WorldBorder getWorldBorder() {
    return Objects.requireNonNull(Minecraft.getInstance().level).getWorldBorder();
  }

  @Nullable
  @Override
  public Entity getEntity(int id) {
    return null;
  }

  @Override
  public Collection<? extends net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
    return List.of();
  }

  @Override
  public void setRespawnData(LevelData.RespawnData respawnData) {
    ((FakeLevelData)this.levelData).setSpawn(respawnData);
  }

  @Override
  public LevelData.RespawnData getRespawnData() {
    return ((FakeLevelData)this.levelData).getRespawnData();
  }

  @Nullable
  @Override
  public MapItemSavedData getMapData(@Nonnull MapId mapId) {
    return Objects.requireNonNull(Minecraft.getInstance().level).getMapData(mapId);
  }

  @Override
  public void destroyBlockProgress(int breakerId, @Nonnull BlockPos pos, int progress) {}

  @Nonnull
  @Override
  public Scoreboard getScoreboard() {
    return this.scoreboard;
  }

  @Nonnull
  @Override
  public RecipeAccess recipeAccess() {
    return Objects.requireNonNull(Minecraft.getInstance().level).recipeAccess();
  }

  @Override
  protected LevelEntityGetter<Entity> getEntities() {
    return FakeEntityGetter.INSTANCE;
  }

  @Nonnull
  @Override
  public LevelTickAccess<Block> getBlockTicks() {
    return BlackholeTickAccess.emptyLevelList();
  }

  @Nonnull
  @Override
  public LevelTickAccess<Fluid> getFluidTicks() {
    return BlackholeTickAccess.emptyLevelList();
  }

  @Nonnull
  @Override
  public ChunkSource getChunkSource() {
    return this.chunkSource;
  }

  @Override
  public void levelEvent(@Nullable Entity source, int type, @Nonnull BlockPos pos, int data) {}

  @Override
  public void gameEvent(Holder<GameEvent> event, Vec3 position, Context context) {}

  @Override
  public FeatureFlagSet enabledFeatures() {
    return Objects.requireNonNull(Minecraft.getInstance().level).enabledFeatures();
  }

  @Nonnull
  @Override
  public List<? extends Player> players() {
    return List.of();
  }

  @Nonnull
  @Override
  public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
    return registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
  }

  @Override
  public TickRateManager tickRateManager() {
    return Objects.requireNonNull(Minecraft.getInstance().level).tickRateManager();
  }

  @Override
  public ClockManager clockManager() {
    return Objects.requireNonNull(Minecraft.getInstance().level).clockManager();
  }

  @Override
  public EnvironmentAttributeSystem environmentAttributes() {
    return Objects.requireNonNull(Minecraft.getInstance().level).environmentAttributes();
  }

  @Override
  public PotionBrewing potionBrewing() {
    return Objects.requireNonNull(Minecraft.getInstance().level).potionBrewing();
  }

  @Override
  public FuelValues fuelValues() {
    return Objects.requireNonNull(Minecraft.getInstance().level).fuelValues();
  }

  @Override
  public CardinalLighting cardinalLighting() {
    return CardinalLighting.DEFAULT;
  }

  @Override
  public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
    return -1;
  }
}
