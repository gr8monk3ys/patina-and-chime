package io.github.gr8monk3ys.patinachime.test;

import com.mojang.authlib.GameProfile;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import io.github.gr8monk3ys.patinachime.music.MusicTracker;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MusicGrowthGameTests {
	// Every test keeps its blocks on this column. Test areas are at least 13 blocks apart, so no test's music reaches another's budding amethyst.
	private static final BlockPos BUDDING = new BlockPos(3, 1, 3);
	private static final BlockPos ABOVE_BUDDING = BUDDING.above();
	private static final BlockPos SOURCE = new BlockPos(3, 4, 3);
	private static final int VANILLA_CHANCE = 5;
	private static final int NO_GROWTH_ROLL = 1;

	private static MusicTracker tracker(GameTestHelper helper) {
		return MusicGrowthFeature.tracker(helper.getLevel());
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	private static boolean musicAt(GameTestHelper helper, BlockPos relative) {
		return tracker(helper).isActiveNear(helper.absolutePos(relative), now(helper));
	}

	private static void playNote(GameTestHelper helper, BlockPos relative) {
		helper.setBlock(relative, Blocks.NOTE_BLOCK);
		BlockPos pos = helper.absolutePos(relative);
		helper.assertTrue(helper.getLevel().getBlockState(pos).triggerEvent(helper.getLevel(), pos, 0, 0), "a note block on open air should play");
	}

	private static ScriptedRandom randomTick(GameTestHelper helper, BlockPos budding, int roll, Direction side) {
		ScriptedRandom random = new ScriptedRandom(roll, side.ordinal());
		helper.getBlockState(budding).randomTick(helper.getLevel(), helper.absolutePos(budding), random);
		return random;
	}

	private static void assertChanceIsExactlyOneIn(GameTestHelper helper, int chance) {
		for (int roll = 0; roll < chance; roll++) {
			helper.setBlock(ABOVE_BUDDING, Blocks.AIR);
			ScriptedRandom random = randomTick(helper, BUDDING, roll, Direction.UP);
			helper.assertValueEqual(random.bounds.get(0), chance, "growth chance");
			if (roll == 0) {
				helper.assertBlockPresent(Blocks.SMALL_AMETHYST_BUD, ABOVE_BUDDING);
				helper.assertValueEqual(random.bounds, List.of(chance, Direction.values().length), "bounds drawn on a growing tick");
			} else {
				helper.assertBlockPresent(Blocks.AIR, ABOVE_BUDDING);
				helper.assertValueEqual(random.bounds, List.of(chance), "bounds drawn on a tick that fails its roll");
			}
		}
	}

	// Plain tracker

	@GameTest
	public void radiusIsASphereOfEightBlocks(GameTestHelper helper) {
		BlockPos origin = new BlockPos(100, 64, -100);
		MusicTracker tracker = new MusicTracker();
		tracker.mark(origin, 1000L, 0L);

		for (Direction direction : Direction.values()) {
			helper.assertTrue(tracker.isActiveNear(origin.relative(direction, 8), 0L), "8 blocks straight along " + direction + " counts");
			helper.assertFalse(tracker.isActiveNear(origin.relative(direction, 9), 0L), "9 blocks straight along " + direction + " is out");
		}

		helper.assertFalse(tracker.isActiveNear(origin.offset(8, 1, 0), 0L), "just over 8 at distance squared 65 is out");
		helper.assertFalse(tracker.isActiveNear(origin.offset(-7, 4, 0), 0L), "distance squared 65 is out in any direction");
		helper.assertTrue(tracker.isActiveNear(origin.offset(4, 4, 4), 0L), "distance squared 48 is in");
		helper.assertTrue(tracker.isActiveNear(origin.offset(4, -4, 5), 0L), "distance squared 57 is in");
		helper.assertTrue(tracker.isActiveNear(origin.offset(-6, 5, -1), 0L), "distance squared 62 is in");
		helper.assertFalse(tracker.isActiveNear(origin.offset(8, 8, 8), 0L), "the corner of the 8 block cube is outside the sphere");
		helper.assertFalse(tracker.isActiveNear(origin.offset(-5, 5, 5), 0L), "distance squared 75 is inside the cube, outside the sphere");
		helper.succeed();
	}

	@GameTest
	public void radiusSurvivesFarApartPositions(GameTestHelper helper) {
		MusicTracker tracker = new MusicTracker();
		BlockPos source = new BlockPos(-30000000, 64, 0);
		tracker.mark(source, 1000L, 0L);

		helper.assertFalse(tracker.isActiveNear(source.offset(65536, 0, 0), 0L), "65536 squared overflows an int to zero");
		helper.assertFalse(tracker.isActiveNear(new BlockPos(30000000, 64, 0), 0L), "opposite edges of the world are not near");
		helper.succeed();
	}

	@GameTest
	public void trackerStopsCountingAtTheExpiryTick(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 64, 0);
		MusicTracker tracker = new MusicTracker();
		tracker.mark(pos, 100L, 0L);

		helper.assertTrue(tracker.isActiveNear(pos, 99L), "the last tick before expiry still counts");
		helper.assertFalse(tracker.isActiveNear(pos, 100L), "the expiry tick no longer counts");
		helper.assertValueEqual(tracker.size(), 0, "entries after a query finds one expired");
		helper.succeed();
	}

	@GameTest
	public void recordingSweepsExpiredEntriesWithoutAnyQuery(GameTestHelper helper) {
		MusicTracker tracker = new MusicTracker();
		for (int x = 0; x < 500; x++) {
			tracker.mark(new BlockPos(x * 20, 64, 0), 100L, 0L);
		}

		helper.assertValueEqual(tracker.size(), 500, "entries before they expire");
		tracker.mark(new BlockPos(0, 100, 0), 300L, 200L);
		helper.assertValueEqual(tracker.size(), 1, "entries after the next record past their expiry");
		helper.succeed();
	}

	@GameTest
	public void remarkingOnePositionDoesNotGrowTheTracker(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 64, 0);
		MusicTracker tracker = new MusicTracker();
		for (long tick = 0; tick < 1000; tick++) {
			tracker.mark(pos, tick + 2, tick);
		}

		helper.assertValueEqual(tracker.size(), 1, "entries for one source marked every tick");
		tracker.forget(pos);
		helper.assertValueEqual(tracker.size(), 0, "entries after forgetting it");
		helper.assertFalse(tracker.isActiveNear(pos, 1000L), "a forgotten source no longer counts");
		helper.succeed();
	}

	@GameTest
	public void levelsKeepSeparateTrackers(GameTestHelper helper) {
		ServerLevel overworld = helper.getLevel();
		ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "the nether should exist");

		playNote(helper, SOURCE);
		BlockPos pos = helper.absolutePos(SOURCE);
		long now = overworld.getGameTime();
		helper.assertTrue(MusicGrowthFeature.tracker(overworld) == MusicGrowthFeature.tracker(overworld), "a level keeps one tracker");
		helper.assertTrue(MusicGrowthFeature.tracker(overworld).isActiveNear(pos, now), "the note counts in its own level");
		helper.assertFalse(MusicGrowthFeature.tracker(nether).isActiveNear(pos, now), "the same coordinates in the nether hear nothing");
		helper.succeed();
	}

	// The real random tick

	@GameTest
	public void withoutMusicGrowthChanceIsOneInFive(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		helper.assertFalse(musicAt(helper, BUDDING), "this test needs silence");
		assertChanceIsExactlyOneIn(helper, VANILLA_CHANCE);
		helper.succeed();
	}

	@GameTest
	public void withMusicGrowthChanceIsOneInTwo(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		playNote(helper, SOURCE);
		assertChanceIsExactlyOneIn(helper, MusicGrowthFeature.BOOSTED_GROWTH_CHANCE);
		helper.succeed();
	}

	@GameTest
	public void musicNeverSlowsAFasterGrowthChance(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		playNote(helper, SOURCE);
		helper.assertValueEqual(MusicGrowthFeature.growthChance(helper.getLevel(), helper.absolutePos(BUDDING), 1), 1, "chance another mod set to every tick");
		helper.succeed();
	}

	@GameTest
	public void randomTickUsesTheRadiusBoundary(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);

		BlockPos inRange = BUDDING.above(8);
		playNote(helper, inRange);
		helper.assertValueEqual(randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP).bounds.get(0), MusicGrowthFeature.BOOSTED_GROWTH_CHANCE, "chance with a note 8 blocks away");
		tracker(helper).forget(helper.absolutePos(inRange));

		BlockPos outOfRange = BUDDING.above(9);
		playNote(helper, outOfRange);
		helper.assertValueEqual(randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP).bounds.get(0), VANILLA_CHANCE, "chance with a note 9 blocks away");
		helper.succeed();
	}

	@GameTest
	public void musicKeepsVanillaGrowthSideAndStages(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		playNote(helper, SOURCE);

		for (Direction side : Direction.values()) {
			BlockPos target = BUDDING.relative(side);
			helper.setBlock(target, Blocks.AIR);
			randomTick(helper, BUDDING, 0, side);
			helper.assertBlockPresent(Blocks.SMALL_AMETHYST_BUD, target);
			helper.assertBlockProperty(target, AmethystClusterBlock.FACING, side);
		}

		for (Block next : List.of(Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER)) {
			randomTick(helper, BUDDING, 0, Direction.UP);
			helper.assertBlockPresent(next, ABOVE_BUDDING);
		}

		randomTick(helper, BUDDING, 0, Direction.UP);
		helper.assertBlockPresent(Blocks.AMETHYST_CLUSTER, ABOVE_BUDDING);

		helper.setBlock(ABOVE_BUDDING, Blocks.SMALL_AMETHYST_BUD.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.NORTH));
		randomTick(helper, BUDDING, 0, Direction.UP);
		helper.assertBlockPresent(Blocks.SMALL_AMETHYST_BUD, ABOVE_BUDDING);

		helper.setBlock(ABOVE_BUDDING, Blocks.STONE);
		randomTick(helper, BUDDING, 0, Direction.UP);
		helper.assertBlockPresent(Blocks.STONE, ABOVE_BUDDING);

		helper.setBlock(ABOVE_BUDDING, Blocks.WATER);
		randomTick(helper, BUDDING, 0, Direction.UP);
		helper.assertBlockProperty(ABOVE_BUDDING, AmethystClusterBlock.WATERLOGGED, true);
		helper.succeed();
	}

	// Note blocks

	@GameTest(maxTicks = 120)
	public void noteCountsForFiveSeconds(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		playNote(helper, SOURCE);
		long played = now(helper);
		BlockPos pos = helper.absolutePos(SOURCE);

		helper.assertTrue(tracker(helper).isActiveNear(pos, played + 99), "a note counts through its 100th tick");
		helper.assertTrue(musicAt(helper, SOURCE), "a note counts at once");

		helper.runAfterDelay(98, () -> {
			helper.assertTrue(now(helper) < played + 100, "this check must run inside the 5 seconds");
			helper.assertValueEqual(randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP).bounds.get(0), MusicGrowthFeature.BOOSTED_GROWTH_CHANCE, "chance just before the note expires");
		});
		helper.runAfterDelay(102, () -> {
			helper.assertTrue(now(helper) >= played + 100, "this check must run after the 5 seconds");
			helper.assertValueEqual(randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP).bounds.get(0), VANILLA_CHANCE, "chance after the note expires");
			helper.succeed();
		});
	}

	@GameTest
	public void tuningClickUnderABlockRecordsNothing(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(SOURCE, Blocks.NOTE_BLOCK);
		helper.setBlock(SOURCE.above(), Blocks.STONE);
		helper.useBlock(SOURCE, player);
		helper.assertBlockProperty(SOURCE, NoteBlock.NOTE, 1);

		helper.runAfterDelay(2, () -> {
			helper.assertFalse(musicAt(helper, SOURCE), "a click that only tunes plays nothing");

			helper.setBlock(SOURCE.above(), Blocks.AIR);
			helper.useBlock(SOURCE, player);
			helper.assertBlockProperty(SOURCE, NoteBlock.NOTE, 2);
			helper.assertFalse(musicAt(helper, SOURCE), "the note has not played until its block event runs");
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(musicAt(helper, SOURCE), "a click with open air above plays a note");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void headWithNoSoundRecordsNothing(GameTestHelper helper) {
		helper.setBlock(SOURCE, Blocks.NOTE_BLOCK);
		helper.setBlock(SOURCE.above(), Blocks.PLAYER_HEAD);
		helper.useBlock(SOURCE, helper.makeMockPlayer(GameType.SURVIVAL));

		helper.runAfterDelay(2, () -> {
			helper.assertFalse(musicAt(helper, SOURCE), "a player head with no note sound plays nothing");
			helper.succeed();
		});
	}

	// Jukeboxes

	private static JukeboxBlockEntity placeJukebox(GameTestHelper helper) {
		helper.setBlock(SOURCE, Blocks.JUKEBOX);
		return helper.getBlockEntity(SOURCE, JukeboxBlockEntity.class);
	}

	@GameTest
	public void jukeboxCountsOnlyWhileADiscPlays(GameTestHelper helper) {
		JukeboxBlockEntity jukebox = placeJukebox(helper);
		helper.assertFalse(musicAt(helper, SOURCE), "an empty jukebox is silent");

		jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
		helper.assertTrue(musicAt(helper, SOURCE), "a jukebox counts as soon as its disc starts");

		helper.runAfterDelay(10, () -> {
			helper.assertTrue(musicAt(helper, SOURCE), "a playing jukebox keeps counting long past one mark");
			jukebox.popOutTheItem();
			helper.assertFalse(musicAt(helper, SOURCE), "taking the disc out stops it at once");
			helper.runAfterDelay(2, () -> {
				helper.assertFalse(musicAt(helper, SOURCE), "an emptied jukebox stays silent");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void jukeboxStopsCountingWhenTheSongEnds(GameTestHelper helper) {
		JukeboxBlockEntity jukebox = placeJukebox(helper);
		ItemStack disc = new ItemStack(Items.MUSIC_DISC_CAT);
		jukebox.setTheItem(disc);
		Holder<JukeboxSong> song = JukeboxSong.fromStack(disc).orElseThrow();
		jukebox.getSongPlayer().setSongWithoutPlaying(song, song.value().lengthInTicks() + 19);

		helper.runAfterDelay(4, () -> {
			helper.assertFalse(jukebox.getSongPlayer().isPlaying(), "the song should have ended");
			helper.assertFalse(jukebox.getTheItem().isEmpty(), "the disc stays in the jukebox");
			helper.assertTrue(helper.getBlockState(SOURCE).getValue(JukeboxBlock.HAS_RECORD), "the jukebox still has its record");
			helper.assertFalse(musicAt(helper, SOURCE), "a finished disc is not music");
			jukebox.setTheItem(ItemStack.EMPTY);
			helper.succeed();
		});
	}

	@GameTest
	public void breakingAJukeboxStopsItCounting(GameTestHelper helper) {
		JukeboxBlockEntity jukebox = placeJukebox(helper);
		jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
		helper.assertTrue(musicAt(helper, SOURCE), "the jukebox plays before it is broken");

		helper.getLevel().destroyBlock(helper.absolutePos(SOURCE), false);
		helper.assertFalse(musicAt(helper, SOURCE), "a broken jukebox stops at once");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(musicAt(helper, SOURCE), "a broken jukebox stays silent");
			helper.succeed();
		});
	}

	@GameTest
	public void removingAJukeboxBlockEntityStopsItCounting(GameTestHelper helper) {
		JukeboxBlockEntity jukebox = placeJukebox(helper);
		jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
		helper.assertTrue(musicAt(helper, SOURCE), "the jukebox plays before it is removed");

		helper.getLevel().removeBlockEntity(helper.absolutePos(SOURCE));
		helper.assertTrue(jukebox.getSongPlayer().isPlaying(), "this path never stops the song, as when a chunk unloads");
		helper.assertFalse(musicAt(helper, SOURCE), "a removed block entity stops counting at once");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(musicAt(helper, SOURCE), "a removed block entity stays silent");
			helper.succeed();
		});
	}

	@GameTest
	public void loadedJukeboxRemarksItselfOnItsNextTick(GameTestHelper helper) {
		helper.setBlock(SOURCE, Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		JukeboxBlockEntity jukebox = helper.getBlockEntity(SOURCE, JukeboxBlockEntity.class);
		jukebox.setSongItemWithoutPlaying(new ItemStack(Items.MUSIC_DISC_CAT));
		helper.assertFalse(musicAt(helper, SOURCE), "a jukebox loaded mid-song has no mark yet, as after a restart");

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(musicAt(helper, SOURCE), "its next tick marks it");
			jukebox.setTheItem(ItemStack.EMPTY);
			helper.assertFalse(musicAt(helper, SOURCE), "and it stops as normal");
			helper.succeed();
		});
	}

	// Particles

	private static List<ClientboundLevelParticlesPacket> noteParticlesAt(PacketSpy spy, BlockPos pos) {
		return spy.sent(ClientboundLevelParticlesPacket.class).stream()
			.filter(packet -> packet.getParticle() == ParticleTypes.NOTE)
			.filter(packet -> pos.closerToCenterThan(new Vec3(packet.getX(), packet.getY(), packet.getZ()), 2.0))
			.toList();
	}

	@GameTest
	public void boostedTickSendsNoteParticlesToNearbyPlayers(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		BlockPos budding = helper.absolutePos(BUDDING);
		PacketSpy spy = PacketSpy.at(helper, budding.north(5));
		try {
			randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP);
			helper.assertTrue(noteParticlesAt(spy, budding).isEmpty(), "an unboosted tick shows nothing");

			playNote(helper, SOURCE);
			randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP);
			List<ClientboundLevelParticlesPacket> packets = noteParticlesAt(spy, budding);
			helper.assertValueEqual(packets.size(), 1, "particle packets on a boosted tick");
			helper.assertTrue(packets.get(0).getCount() > 1, "a boosted tick shows a few notes");

			spy.clear();
			spy.player.setPos(budding.getX(), budding.getY(), budding.getZ() + 40);
			randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP);
			helper.assertTrue(noteParticlesAt(spy, budding).isEmpty(), "a player out of particle range gets no packet");
			helper.assertValueEqual(randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP).bounds.get(0), MusicGrowthFeature.BOOSTED_GROWTH_CHANCE, "the boost does not depend on anyone seeing it");
		} finally {
			spy.close();
		}

		helper.succeed();
	}

	@GameTest
	public void boostParticlesAppearOnAnExposedFace(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		BlockPos budding = helper.absolutePos(BUDDING);
		for (Direction side : Direction.values()) {
			helper.setBlock(BUDDING.relative(side), Blocks.STONE);
		}

		playNote(helper, SOURCE);
		PacketSpy spy = PacketSpy.at(helper, budding.north(5));
		try {
			randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP);
			helper.assertTrue(noteParticlesAt(spy, budding).isEmpty(), "nothing is sent for a block with no open face");

			helper.setBlock(BUDDING.relative(Direction.EAST), Blocks.AIR);
			randomTick(helper, BUDDING, NO_GROWTH_ROLL, Direction.UP);
			List<ClientboundLevelParticlesPacket> packets = noteParticlesAt(spy, budding);
			helper.assertValueEqual(packets.size(), 1, "particle packets with one open face");
			helper.assertTrue(packets.get(0).getX() > budding.getX() + 1.0, "the notes appear outside the open east face");
		} finally {
			spy.close();
		}

		helper.succeed();
	}

	/**
	 * Answers nextInt(bound) with a set roll, then a set side. Everything else throws, which proves the tick draws nothing else from it.
	 */
	static final class ScriptedRandom implements RandomSource {
		final List<Integer> bounds = new ArrayList<>();
		private final int roll;
		private final int side;

		ScriptedRandom(int roll, int side) {
			this.roll = roll;
			this.side = side;
		}

		@Override
		public int nextInt(int bound) {
			this.bounds.add(bound);
			return this.bounds.size() == 1 ? this.roll : this.side;
		}

		@Override
		public RandomSource fork() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PositionalRandomFactory forkPositional() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setSeed(long seed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextInt() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long nextLong() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean nextBoolean() {
			throw new UnsupportedOperationException();
		}

		@Override
		public float nextFloat() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double nextDouble() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double nextGaussian() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * A connected player that records what the server sends it, for checking what really reaches clients.
	 * Built like GameTestHelper.makeMockServerPlayerInLevel, whose connection can't be read.
	 */
	static final class PacketSpy implements AutoCloseable {
		final ServerPlayer player;
		private final List<Packet<?>> sent = new ArrayList<>();

		private PacketSpy(GameTestHelper helper, BlockPos pos) {
			ServerLevel level = helper.getLevel();
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-packet-spy"), false);
			this.player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
				@Override
				public GameType gameMode() {
					return GameType.CREATIVE;
				}
			};
			Connection connection = new Connection(PacketFlow.SERVERBOUND) {
				@Override
				public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
					PacketSpy.this.sent.add(packet);
					super.send(packet, listener, flush);
				}
			};
			new EmbeddedChannel(connection);
			level.getServer().getPlayerList().placeNewPlayer(connection, this.player, cookie);
			this.player.setPos(pos.getX(), pos.getY(), pos.getZ());
			this.clear();
		}

		static PacketSpy at(GameTestHelper helper, BlockPos pos) {
			return new PacketSpy(helper, pos);
		}

		<T> List<T> sent(Class<T> type) {
			return this.sent.stream().filter(type::isInstance).map(type::cast).toList();
		}

		void clear() {
			this.sent.clear();
		}

		@Override
		public void close() {
			this.player.level().getServer().getPlayerList().remove(this.player);
		}
	}
}
