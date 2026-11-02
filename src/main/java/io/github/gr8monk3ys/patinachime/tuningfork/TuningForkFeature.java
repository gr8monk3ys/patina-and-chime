package io.github.gr8monk3ys.patinachime.tuningfork;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class TuningForkFeature {
	static final SoundEvent SOUND_NONE = registerSound("tuning_fork.none");
	static final SoundEvent SOUND_FAINT = registerSound("tuning_fork.faint");
	static final SoundEvent SOUND_STEADY = registerSound("tuning_fork.steady");
	static final SoundEvent SOUND_STRONG = registerSound("tuning_fork.strong");

	private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, PatinaChime.id("tuning_fork"));
	public static final Item TUNING_FORK = Registry.register(
		BuiltInRegistries.ITEM, KEY, new TuningForkItem(new Item.Properties().setId(KEY).stacksTo(1).useCooldown(1.0F))
	);

	private TuningForkFeature() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.insertAfter(Items.SPYGLASS, TUNING_FORK));
	}

	private static SoundEvent registerSound(String path) {
		Identifier id = PatinaChime.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
