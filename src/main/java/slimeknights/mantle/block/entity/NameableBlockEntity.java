package slimeknights.mantle.block.entity;

import lombok.Getter;
import lombok.Setter;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.mantle.Mantle;

/**
 * Extension of tile entity to make it namable
 */
public abstract class NameableBlockEntity extends MantleBlockEntity implements INameableMenuProvider {
	private static final String TAG_CUSTOM_NAME = "CustomName";

	/** Default title for this tile entity */
	@Getter
	private final Component defaultName;
	/** Title set to this tile entity */
	@Getter @Setter
	private Component customName;

	public NameableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component defaultTitle) {
		super(type, pos, state);
		this.defaultName = defaultTitle;
	}

	@Override
	public void load(CompoundTag tags) {
		super.load(tags);
		this.customName = null;
		Tag name = tags.get(TAG_CUSTOM_NAME);
		if (name instanceof StringTag string) {
			String text = string.value();
			// Older Mantle versions stored a JSON component inside an NBT string.
			if (text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")) {
				try {
					this.customName = ComponentSerialization.CODEC.parse(registries().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(text)).result().orElse(null);
				} catch (JsonParseException ignored) {
					// A native literal name may itself start with JSON punctuation.
				}
			}
		}
		if (name != null && this.customName == null) {
			this.customName = ComponentSerialization.CODEC.parse(registries().createSerializationContext(NbtOps.INSTANCE), name).resultOrPartial(Mantle.logger::error).orElse(null);
		}
	}

	@Override
	public void saveSynced(CompoundTag tags) {
		super.saveSynced(tags);
		if (this.hasCustomName()) {
			Tag name = ComponentSerialization.CODEC.encodeStart(registries().createSerializationContext(NbtOps.INSTANCE), this.customName).getOrThrow();
			// Structured native components distinguish new literal names from legacy JSON strings.
			if (name instanceof StringTag literal) {
				CompoundTag component = new CompoundTag();
				component.putString("text", literal.value());
				name = component;
			}
			tags.put(TAG_CUSTOM_NAME, name);
		} else {
			tags.remove(TAG_CUSTOM_NAME);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		this.customName = components.get(DataComponents.CUSTOM_NAME);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.CUSTOM_NAME, this.customName);
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard(TAG_CUSTOM_NAME);
	}
}
