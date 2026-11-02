package io.github.gr8monk3ys.patinachime.music;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public final class AmethystNoteFeature {
	public static final Identifier NOTE_SOUND_ID = PatinaChime.id("note_block.amethyst");
	public static final Holder<SoundEvent> NOTE_SOUND = Registry.registerForHolder(
		BuiltInRegistries.SOUND_EVENT, NOTE_SOUND_ID, SoundEvent.createVariableRangeEvent(NOTE_SOUND_ID)
	);

	private AmethystNoteFeature() {
	}

	public static void init() {
	}

	/**
	 * A Block of Amethyst has no instrument of its own, so a note block on one reads as harp. Any head on top or real
	 * instrument below has already changed the block state's instrument away from harp, and keeps its vanilla sound.
	 */
	public static Holder<SoundEvent> noteSound(BlockGetter level, BlockPos pos, Holder<SoundEvent> vanillaSound) {
		BlockState state = level.getBlockState(pos);
		boolean onAmethyst = state.is(Blocks.NOTE_BLOCK)
			&& state.getValue(NoteBlock.INSTRUMENT) == NoteBlockInstrument.HARP
			&& level.getBlockState(pos.below()).is(Blocks.AMETHYST_BLOCK);
		return onAmethyst ? NOTE_SOUND : vanillaSound;
	}
}
