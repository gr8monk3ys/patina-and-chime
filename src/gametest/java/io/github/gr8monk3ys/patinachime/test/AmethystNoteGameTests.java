package io.github.gr8monk3ys.patinachime.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.gr8monk3ys.patinachime.music.AmethystNoteFeature;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

public class AmethystNoteGameTests {
	private static final BlockPos NOTE = new BlockPos(3, 2, 3);
	private static final BlockPos BELOW = NOTE.below();
	private static final BlockPos ABOVE = NOTE.above();

	// The note block goes down first, so that the block placed under or over it updates its instrument the way play does.
	private static void placeNoteBlock(GameTestHelper helper, Block below) {
		helper.setBlock(NOTE, Blocks.NOTE_BLOCK);
		helper.setBlock(BELOW, below);
	}

	private static Holder<SoundEvent> chosenSound(GameTestHelper helper) {
		NoteBlockInstrument instrument = helper.getBlockState(NOTE).getValue(NoteBlock.INSTRUMENT);
		return AmethystNoteFeature.noteSound(helper.getLevel(), helper.absolutePos(NOTE), instrument.getSoundEvent());
	}

	private static void assertSound(GameTestHelper helper, Holder<SoundEvent> actual, Holder<SoundEvent> expected, String what) {
		helper.assertValueEqual(actual.value().location(), expected.value().location(), what);
	}

	@GameTest
	public void onAmethystUsesTheAmethystEvent(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.AMETHYST_BLOCK);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.HARP);
		assertSound(helper, chosenSound(helper), AmethystNoteFeature.NOTE_SOUND, "sound on a Block of Amethyst");
		helper.assertValueEqual(chosenSound(helper).value().location().toString(), "patinachime:note_block.amethyst", "event id");
		helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(AmethystNoteFeature.NOTE_SOUND_ID), "the event should be registered");
		helper.succeed();
	}

	@GameTest
	public void onAnotherBlockKeepsVanilla(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.DIRT);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.HARP);
		assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_HARP, "sound on dirt");

		for (Block harpBlock : List.of(Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER)) {
			placeNoteBlock(helper, harpBlock);
			helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.HARP);
			assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_HARP, "only a Block of Amethyst counts, not " + harpBlock);
		}

		placeNoteBlock(helper, Blocks.STONE);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.BASEDRUM);
		assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_BASEDRUM, "sound on stone");

		placeNoteBlock(helper, Blocks.GOLD_BLOCK);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL);
		assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_BELL, "sound on gold");
		helper.succeed();
	}

	@GameTest
	public void amethystMustBeDirectlyBelow(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.AIR);
		helper.setBlock(BELOW.below(), Blocks.AMETHYST_BLOCK);
		assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_HARP, "sound one block above the amethyst");
		helper.succeed();
	}

	@GameTest
	public void headOnTopWins(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.AMETHYST_BLOCK);
		helper.setBlock(ABOVE, Blocks.ZOMBIE_HEAD);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.ZOMBIE);
		assertSound(helper, chosenSound(helper), SoundEvents.NOTE_BLOCK_IMITATE_ZOMBIE, "sound under a zombie head");

		helper.setBlock(ABOVE, Blocks.PLAYER_HEAD);
		helper.assertBlockProperty(NOTE, NoteBlock.INSTRUMENT, NoteBlockInstrument.CUSTOM_HEAD);
		Holder<SoundEvent> vanilla = NoteBlockInstrument.CUSTOM_HEAD.getSoundEvent();
		assertSound(helper, chosenSound(helper), vanilla, "sound under a player head");

		helper.setBlock(ABOVE, Blocks.AIR);
		assertSound(helper, chosenSound(helper), AmethystNoteFeature.NOTE_SOUND, "taking the head off restores the amethyst sound");
		helper.succeed();
	}

	@GameTest
	public void notABlockAtAllKeepsVanilla(GameTestHelper helper) {
		helper.setBlock(NOTE, Blocks.STONE);
		helper.setBlock(BELOW, Blocks.AMETHYST_BLOCK);
		Holder<SoundEvent> vanilla = SoundEvents.NOTE_BLOCK_HARP;
		assertSound(helper, AmethystNoteFeature.noteSound(helper.getLevel(), helper.absolutePos(NOTE), vanilla), vanilla, "sound of a position that isn't a note block");
		helper.succeed();
	}

	// The real triggerEvent path

	// Sound packets read positions back through a float, and test areas can be 15 million blocks out, so this can't compare exactly.
	private static ClientboundSoundPacket playAndCapture(GameTestHelper helper, MusicGrowthGameTests.PacketSpy spy) {
		BlockPos pos = helper.absolutePos(NOTE);
		spy.clear();
		BlockState state = helper.getLevel().getBlockState(pos);
		helper.assertTrue(state.triggerEvent(helper.getLevel(), pos, 0, 0), "triggerEvent should report a played note");
		List<ClientboundSoundPacket> packets = spy.sent(ClientboundSoundPacket.class).stream()
			.filter(packet -> pos.closerToCenterThan(new Vec3(packet.getX(), packet.getY(), packet.getZ()), 2.0))
			.toList();
		helper.assertValueEqual(packets.size(), 1, "sound packets from one note");
		return packets.get(0);
	}

	@GameTest
	public void triggerEventPlaysAmethystAtTheNormalPitch(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.AMETHYST_BLOCK);
		MusicGrowthGameTests.PacketSpy spy = MusicGrowthGameTests.PacketSpy.at(helper, helper.absolutePos(NOTE).north(4));
		try {
			for (int note : List.of(0, 1, 12, 24)) {
				helper.setBlock(NOTE, helper.getBlockState(NOTE).setValue(NoteBlock.NOTE, note));
				ClientboundSoundPacket packet = playAndCapture(helper, spy);
				assertSound(helper, packet.getSound(), AmethystNoteFeature.NOTE_SOUND, "sound sent for note " + note);
				helper.assertValueEqual(packet.getPitch(), NoteBlock.getPitchFromNote(note), "pitch of note " + note);
				helper.assertValueEqual(packet.getVolume(), 3.0F, "volume of note " + note);
				helper.assertValueEqual(packet.getSource(), SoundSource.RECORDS, "category of note " + note);
			}
		} finally {
			spy.close();
		}

		helper.succeed();
	}

	@GameTest
	public void triggerEventLeavesOtherInstrumentsAlone(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.DIRT);
		MusicGrowthGameTests.PacketSpy spy = MusicGrowthGameTests.PacketSpy.at(helper, helper.absolutePos(NOTE).north(4));
		try {
			assertSound(helper, playAndCapture(helper, spy).getSound(), SoundEvents.NOTE_BLOCK_HARP, "sound sent on dirt");

			placeNoteBlock(helper, Blocks.GOLD_BLOCK);
			assertSound(helper, playAndCapture(helper, spy).getSound(), SoundEvents.NOTE_BLOCK_BELL, "sound sent on gold");

			placeNoteBlock(helper, Blocks.AMETHYST_BLOCK);
			helper.setBlock(ABOVE, Blocks.ZOMBIE_HEAD);
			ClientboundSoundPacket head = playAndCapture(helper, spy);
			assertSound(helper, head.getSound(), SoundEvents.NOTE_BLOCK_IMITATE_ZOMBIE, "sound sent under a zombie head");
			helper.assertValueEqual(head.getPitch(), 1.0F, "a mob head is not tunable");
		} finally {
			spy.close();
		}

		helper.succeed();
	}

	@GameTest
	public void triggerEventUnderASoundlessHeadPlaysNothing(GameTestHelper helper) {
		placeNoteBlock(helper, Blocks.AMETHYST_BLOCK);
		helper.setBlock(ABOVE, Blocks.PLAYER_HEAD);
		BlockPos pos = helper.absolutePos(NOTE);
		helper.assertFalse(helper.getLevel().getBlockState(pos).triggerEvent(helper.getLevel(), pos, 0, 0), "a player head with no sound plays nothing");
		helper.assertFalse(MusicGrowthFeature.tracker(helper.getLevel()).isActiveNear(pos, helper.getLevel().getGameTime()), "and so isn't music");
		helper.succeed();
	}

	// Resources

	private static JsonObject readModJson(String path) throws IOException {
		var file = FabricLoader.getInstance().getModContainer("patinachime").orElseThrow().findPath(path).orElseThrow();
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	@GameTest
	public void subtitleKeyExistsInTheLangFile(GameTestHelper helper) throws IOException {
		JsonObject event = readModJson("assets/patinachime/sounds.json").getAsJsonObject("note_block.amethyst");
		helper.assertTrue(event != null, "sounds.json should define note_block.amethyst");

		String subtitle = event.get("subtitle").getAsString();
		helper.assertValueEqual(subtitle, "subtitles.patinachime.note_block.amethyst", "subtitle key");
		helper.assertTrue(readModJson("assets/patinachime/lang/en_us.json").has(subtitle), "en_us.json should have " + subtitle);
		helper.succeed();
	}

	@GameTest
	public void soundEventIsOneVanillaFileAtItsOwnPitch(GameTestHelper helper) throws IOException {
		JsonArray sounds = readModJson("assets/patinachime/sounds.json").getAsJsonObject("note_block.amethyst").getAsJsonArray("sounds");
		helper.assertValueEqual(sounds.size(), 1, "files in the event, so each note has one base pitch");
		helper.assertTrue(sounds.get(0).isJsonPrimitive(), "a plain file name, with no pitch field that would cut into the note range");
		helper.assertValueEqual(sounds.get(0).getAsString(), "minecraft:block/amethyst/step4", "file");
		helper.succeed();
	}
}
