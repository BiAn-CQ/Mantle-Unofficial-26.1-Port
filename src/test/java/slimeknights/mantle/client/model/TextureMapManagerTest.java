package slimeknights.mantle.client.model;

import java.io.StringReader;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TextureMapManagerTest {
  private static final Identifier ID = Identifier.parse("test:slots");
  private static final Identifier FILE = Identifier.parse("test:mantle/textures/slots.json");

  @AfterEach void clearCache() {
    TextureMapManager.INSTANCE.onReloadSafe(mock(ResourceManager.class));
  }

  private static Resource resource(String json) throws Exception {
    var resource = mock(Resource.class);
    when(resource.openAsReader()).thenAnswer(ignored -> new java.io.BufferedReader(new StringReader(json)));
    when(resource.sourcePackId()).thenReturn("test_pack");
    return resource;
  }

  @Test void higherPriorityPacksReplaceEntriesAndCanAddVariants() throws Exception {
    var manager = mock(ResourceManager.class);
    var packs = List.of(
      resource("{\"textures\":{\"upgrade\":\"test:item/old\",\"defense\":\"test:item/defense\"}}"),
      resource("{\"textures\":{\"upgrade\":\"test:item/new\",\"custom\":\"test:item/custom\"}}"));
    when(manager.getResourceStack(FILE)).thenReturn(packs);
    var textures = TextureMapManager.INSTANCE.getTextureMap(manager, ID);
    assertThat(textures).hasSize(3)
      .containsEntry("upgrade", Identifier.parse("test:item/new"))
      .containsEntry("defense", Identifier.parse("test:item/defense"))
      .containsEntry("custom", Identifier.parse("test:item/custom"));
  }

  @Test void reloadDiscardsCachedTextures() throws Exception {
    var manager = mock(ResourceManager.class);
    var oldPack = resource("{\"textures\":{\"key\":\"test:item/old\"}}");
    var newPack = resource("{\"textures\":{\"key\":\"test:item/new\"}}");
    when(manager.getResourceStack(FILE)).thenReturn(List.of(oldPack));
    var cached = TextureMapManager.INSTANCE.getTextureMap(manager, ID);
    when(manager.getResourceStack(FILE)).thenReturn(List.of(newPack));
    assertThat(TextureMapManager.INSTANCE.getTextureMap(manager, ID)).isSameAs(cached);
    TextureMapManager.INSTANCE.onReloadSafe(manager);
    assertThat(TextureMapManager.INSTANCE.getTextureMap(manager, ID)).containsEntry("key", Identifier.parse("test:item/new"));
  }

  @Test void malformedPackDoesNotDiscardOtherPacks() throws Exception {
    var manager = mock(ResourceManager.class);
    var packs = List.of(resource("{\"textures\":{\"key\":\"test:item/valid\"}}"), resource("[]"));
    when(manager.getResourceStack(FILE)).thenReturn(packs);
    assertThat(TextureMapManager.INSTANCE.getTextureMap(manager, ID)).containsOnlyKeys("key");
  }
}
