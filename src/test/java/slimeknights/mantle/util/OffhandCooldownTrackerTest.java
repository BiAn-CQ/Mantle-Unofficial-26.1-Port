package slimeknights.mantle.util;

import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OffhandCooldownTrackerTest {
  private Player first;
  private Player second;

  @AfterEach
  void tearDown() {
    if (first != null) {
      OffhandCooldownTracker.clear(first);
    }
    if (second != null) {
      OffhandCooldownTracker.clear(second);
    }
  }

  @Test
  void capabilityStorageKeepsLogicalPlayersSeparateByIdentity() {
    first = mock(Player.class);
    second = mock(Player.class);

    OffhandCooldownTracker firstTracker = OffhandCooldownTracker.getOrCreate(first);
    OffhandCooldownTracker secondTracker = OffhandCooldownTracker.getOrCreate(second);

    assertThat(firstTracker).isNotSameAs(secondTracker);
    assertThat(OffhandCooldownTracker.getOrCreate(first)).isSameAs(firstTracker);
    assertThat(OffhandCooldownTracker.getOrCreate(second)).isSameAs(secondTracker);
  }
}
