package slimeknights.mantle.data.loadable.common;

import net.minecraft.world.item.ItemStackTemplate;
import slimeknights.mantle.data.loadable.Loadable;

/** Native 26.1 item stack template loadable. */
public final class ItemStackTemplateLoadable {
  private ItemStackTemplateLoadable() {}

  public static final Loadable<ItemStackTemplate> STACK = new RegistryCodecLoadable<>(
    ItemStackTemplate.CODEC, ItemStackTemplate.STREAM_CODEC);
}
