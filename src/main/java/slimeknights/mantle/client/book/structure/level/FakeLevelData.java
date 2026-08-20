// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/FakeSpawnInfo.java
package slimeknights.mantle.client.book.structure.level;

import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;

public class FakeLevelData implements WritableLevelData {

  private LevelData.RespawnData respawnData = LevelData.RespawnData.DEFAULT;

  @Override
  public void setSpawn(LevelData.RespawnData respawnData) {
    this.respawnData = respawnData;
  }

  @Override
  public LevelData.RespawnData getRespawnData() {
    return respawnData;
  }

  @Override
  public long getGameTime() {
    return 0;
  }

  @Override
  public boolean isHardcore() {
    return false;
  }

  @Override
  public Difficulty getDifficulty() {
    return Difficulty.PEACEFUL;
  }

  @Override
  public boolean isDifficultyLocked() {
    return false;
  }
}
