package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import io.github.gr8monk3ys.patinachime.music.ShatterNoteFeature;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;

// Tests come in two kinds. Those that take several ticks, or count dropped items, run in their own environment, so no
// other test's crystals share their batch, and keep their blocks on one column; test areas are at least 13 blocks apart,
// so a note can't reach another test's cluster. The rest build and clear their blocks inside one server tick, in chunks
// far from every test structure. Those chunks hold no entity tickets, so dropped items there can't be seen or counted.
public class ShatterNoteGameTests {
	private static final String SHATTER = "patinachime-test:shatter_note";
	private static final int TOP_NOTE = 24;

	private static final BlockPos NOTE = new BlockPos(3, 2, 3);
	private static final BlockPos BELOW = NOTE.below();
	private static final BlockPos ABOVE = NOTE.above();
	private static final BlockPos BESIDE = NOTE.east();
	private static final BlockPos BUDDING = new BlockPos(3, 7, 3);
	private static final BlockPos CLUSTER = BUDDING.below();
	private static final BlockPos UNDER_CLUSTER = CLUSTER.below();

	private static final BlockPos FAR_NOTE = new BlockPos(288_007, 200, 288_007);
	private static final int FAR_CLEAR_RANGE = 10;

	private static void placeNote(GameTestHelper helper, int pitch) {
		helper.setBlock(NOTE, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, pitch));
		helper.setBlock(BELOW, Blocks.AMETHYST_BLOCK);
	}

	private static void placeCluster(GameTestHelper helper) {
		helper.setBlock(BUDDING, Blocks.BUDDING_AMETHYST);
		helper.setBlock(CLUSTER, crystal(Blocks.AMETHYST_CLUSTER, Direction.DOWN));
	}

	private static void playTopNote(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(NOTE);
		helper.assertTrue(helper.getBlockState(NOTE).triggerEvent(helper.getLevel(), pos, 0, 0), "the note should sound");
	}

	// Far enough to count a stack that rolled off the hopper down to the floor, and short of the next test's column.
	private static int shards(GameTestHelper helper) {
		return helper.getEntities(EntityTypes.ITEM, CLUSTER, 6.0).stream()
			.map(ItemEntity::getItem)
			.filter(stack -> stack.is(Items.AMETHYST_SHARD))
			.mapToInt(stack -> stack.getCount())
			.sum();
	}

	private static BaseContainerBlockEntity hopperUnderCluster(GameTestHelper helper) {
		return helper.getBlockEntity(UNDER_CLUSTER, BaseContainerBlockEntity.class);
	}

	private static BlockState crystal(Block block, Direction facing) {
		return block.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
	}

	@GameTest(environment = SHATTER)
	public void topNoteBreaksAClusterOnBuddingAmethyst(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		playTopNote(helper);

		helper.assertBlockPresent(Blocks.AIR, CLUSTER);
		helper.assertBlockPresent(Blocks.BUDDING_AMETHYST, BUDDING);
		helper.assertValueEqual(shards(helper), 2, "shards from one cluster with no tool");
		helper.succeed();
	}

	@GameTest(environment = SHATTER)
	public void everyClusterInRangeBreaksAndDropsTwoShards(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);
		helper.setBlock(BUDDING.east(), crystal(Blocks.AMETHYST_CLUSTER, Direction.EAST));
		helper.setBlock(BUDDING.west(), crystal(Blocks.AMETHYST_CLUSTER, Direction.WEST));
		helper.setBlock(BUDDING.north(), crystal(Blocks.AMETHYST_CLUSTER, Direction.NORTH));

		playTopNote(helper);

		for (BlockPos pos : List.of(CLUSTER, BUDDING.east(), BUDDING.west(), BUDDING.north())) {
			helper.assertBlockPresent(Blocks.AIR, pos);
		}

		helper.assertValueEqual(shards(helper), 8, "shards from four clusters");
		helper.succeed();
	}

	@GameTest(environment = SHATTER)
	public void blockDropsOffBreaksTheClusterAndDropsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GameRules rules = level.getGameRules();
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		rules.set(GameRules.BLOCK_DROPS, false, level.getServer());
		try {
			playTopNote(helper);
		} finally {
			rules.set(GameRules.BLOCK_DROPS, true, level.getServer());
		}

		helper.assertBlockPresent(Blocks.AIR, CLUSTER);
		helper.assertValueEqual(shards(helper), 0, "shards with block drops off");
		helper.succeed();
	}

	@GameTest(environment = SHATTER)
	public void topNoteStillCountsAsMusic(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		playTopNote(helper);

		BlockPos budding = helper.absolutePos(BUDDING);
		helper.assertTrue(MusicGrowthFeature.tracker(helper.getLevel()).isActiveNear(budding, helper.getLevel().getGameTime()), "the top note should still grow crystals");
		helper.succeed();
	}

	@GameTest(environment = SHATTER)
	public void redstoneFiresIt(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		helper.setBlock(BESIDE, Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(3, () -> {
			helper.assertBlockPresent(Blocks.AIR, CLUSTER);
			helper.assertValueEqual(shards(helper), 2, "shards from a redstone-played note");
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER)
	public void leftClickAtTheTopNoteFiresIt(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		helper.getBlockState(NOTE).attack(helper.getLevel(), helper.absolutePos(NOTE), player);

		helper.runAfterDelay(3, () -> {
			helper.assertBlockProperty(NOTE, NoteBlock.NOTE, TOP_NOTE);
			helper.assertBlockPresent(Blocks.AIR, CLUSTER);
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER)
	public void rightClickUpToTheTopNoteFiresIt(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeNote(helper, TOP_NOTE - 1);
		placeCluster(helper);

		helper.useBlock(NOTE, player);

		helper.runAfterDelay(3, () -> {
			helper.assertBlockProperty(NOTE, NoteBlock.NOTE, TOP_NOTE);
			helper.assertBlockPresent(Blocks.AIR, CLUSTER);
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER)
	public void rightClickAtTheTopNoteWrapsAndBreaksNothing(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);

		helper.useBlock(NOTE, player);

		helper.runAfterDelay(3, () -> {
			helper.assertBlockProperty(NOTE, NoteBlock.NOTE, 0);
			helper.assertBlockPresent(Blocks.AMETHYST_CLUSTER, CLUSTER);
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER)
	public void aBlockAboveTheNoteBlockStopsIt(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);
		helper.setBlock(ABOVE, Blocks.STONE);

		helper.setBlock(BESIDE, Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(3, () -> {
			helper.assertBlockProperty(NOTE, NoteBlock.POWERED, true);
			helper.assertBlockPresent(Blocks.AMETHYST_CLUSTER, CLUSTER);
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER, maxTicks = 40)
	public void aNoteBlockCanOnlyShatterOncePerSecond(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);
		playTopNote(helper);
		helper.assertBlockPresent(Blocks.AIR, CLUSTER);

		placeCluster(helper);
		playTopNote(helper);
		helper.assertBlockPresent(Blocks.AMETHYST_CLUSTER, CLUSTER);

		helper.runAfterDelay(ShatterNoteFeature.COOLDOWN_TICKS - 2, () -> {
			playTopNote(helper);
			helper.assertBlockPresent(Blocks.AMETHYST_CLUSTER, CLUSTER);
		});
		helper.runAfterDelay(ShatterNoteFeature.COOLDOWN_TICKS + 2, () -> {
			playTopNote(helper);
			helper.assertBlockPresent(Blocks.AIR, CLUSTER);
			helper.succeed();
		});
	}

	@GameTest(environment = SHATTER, maxTicks = 100)
	public void aFreshCopperHopperCollectsTheShards(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);
		helper.setBlock(UNDER_CLUSTER, CopperHopperFeature.BLOCKS.weathering().pick(WeatherState.UNAFFECTED));

		playTopNote(helper);

		helper.succeedWhen(() -> helper.assertValueEqual(hopperUnderCluster(helper).countItem(Items.AMETHYST_SHARD), 2, "shards in the copper hopper"));
	}

	@GameTest(environment = SHATTER, maxTicks = 100)
	public void anOxidizedCopperHopperLeavesTheShards(GameTestHelper helper) {
		placeNote(helper, TOP_NOTE);
		placeCluster(helper);
		helper.setBlock(UNDER_CLUSTER, CopperHopperFeature.BLOCKS.weathering().pick(WeatherState.OXIDIZED));

		playTopNote(helper);

		helper.runAfterDelay(60, () -> {
			helper.assertTrue(hopperUnderCluster(helper).isEmpty(), "an oxidized hopper picked up the shards");
			helper.assertValueEqual(shards(helper), 2, "shards left on the ground");
			helper.succeed();
		});
	}

	private static void farLoad(ServerLevel level) {
		for (int chunkX = SectionPos.blockToSectionCoord(FAR_NOTE.getX() - FAR_CLEAR_RANGE - 1); chunkX <= SectionPos.blockToSectionCoord(FAR_NOTE.getX() + FAR_CLEAR_RANGE + 1); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(FAR_NOTE.getZ() - FAR_CLEAR_RANGE - 1); chunkZ <= SectionPos.blockToSectionCoord(FAR_NOTE.getZ() + FAR_CLEAR_RANGE + 1); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
	}

	// UPDATE_CLIENTS alone, so that clearing a block can't make a crystal next to it break and drop.
	private static void farClear(ServerLevel level) {
		BlockPos min = FAR_NOTE.offset(-FAR_CLEAR_RANGE, -FAR_CLEAR_RANGE, -FAR_CLEAR_RANGE);
		BlockPos max = FAR_NOTE.offset(FAR_CLEAR_RANGE, FAR_CLEAR_RANGE, FAR_CLEAR_RANGE);
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (!level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	// The note block goes down first, so that the block placed under it updates its instrument the way play does.
	private static void farNote(ServerLevel level, Block below, int pitch) {
		level.setBlock(FAR_NOTE, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, pitch), Block.UPDATE_ALL);
		level.setBlock(FAR_NOTE.below(), below.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static void farCrystal(ServerLevel level, BlockPos pos, Block crystal, Direction facing, Block backing) {
		level.setBlock(pos.relative(facing.getOpposite()), backing.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(pos, crystal(crystal, facing), Block.UPDATE_ALL);
	}

	// Far tests share one note block position inside a tick, so each play starts with its cooldown cleared.
	private static void farFire(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ShatterNoteFeature.cooldowns(level).forget(FAR_NOTE);
		helper.assertTrue(level.getBlockState(FAR_NOTE).triggerEvent(level, FAR_NOTE, 0, 0), "the far note should sound");
	}

	private static void assertFarBlock(GameTestHelper helper, BlockPos pos, Block expected, String what) {
		helper.assertTrue(helper.getLevel().getBlockState(pos).is(expected), what + ": expected " + expected + " at " + pos.subtract(FAR_NOTE));
	}

	@GameTest
	public void clusterExactlyEightBlocksAwayBreaks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		farLoad(level);
		for (Direction direction : Direction.values()) {
			BlockPos cluster = FAR_NOTE.relative(direction, 8);
			farNote(level, Blocks.AMETHYST_BLOCK, TOP_NOTE);
			farCrystal(level, cluster, Blocks.AMETHYST_CLUSTER, direction.getOpposite(), Blocks.BUDDING_AMETHYST);

			farFire(helper);

			assertFarBlock(helper, cluster, Blocks.AIR, "8 blocks " + direction);
			farClear(level);
		}

		helper.succeed();
	}

	@GameTest
	public void clusterJustOutsideTheSphereStays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		farLoad(level);
		for (BlockPos offset : List.of(new BlockPos(9, 0, 0), new BlockPos(8, 1, 0), new BlockPos(6, 6, 0), new BlockPos(0, -8, 1))) {
			BlockPos cluster = FAR_NOTE.offset(offset);
			farNote(level, Blocks.AMETHYST_BLOCK, TOP_NOTE);
			farCrystal(level, cluster, Blocks.AMETHYST_CLUSTER, Direction.UP, Blocks.BUDDING_AMETHYST);

			farFire(helper);

			assertFarBlock(helper, cluster, Blocks.AMETHYST_CLUSTER, "outside the sphere at " + offset);
			farClear(level);
		}

		helper.succeed();
	}

	@GameTest
	public void budsOfEverySizeStay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		farLoad(level);
		for (Block bud : List.of(Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD)) {
			BlockPos pos = FAR_NOTE.offset(3, 0, 0);
			farNote(level, Blocks.AMETHYST_BLOCK, TOP_NOTE);
			farCrystal(level, pos, bud, Direction.UP, Blocks.BUDDING_AMETHYST);

			farFire(helper);

			assertFarBlock(helper, pos, bud, "a bud");
			assertFarBlock(helper, pos.below(), Blocks.BUDDING_AMETHYST, "the bud's budding amethyst");
			farClear(level);
		}

		helper.succeed();
	}

	@GameTest
	public void clusterOnAnythingButBuddingAmethystStays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		farLoad(level);
		for (Block backing : List.of(Blocks.STONE, Blocks.AMETHYST_BLOCK)) {
			BlockPos cluster = FAR_NOTE.offset(3, 0, 0);
			farNote(level, Blocks.AMETHYST_BLOCK, TOP_NOTE);
			farCrystal(level, cluster, Blocks.AMETHYST_CLUSTER, Direction.UP, backing);

			farFire(helper);

			assertFarBlock(helper, cluster, Blocks.AMETHYST_CLUSTER, "a cluster on " + backing);
			farClear(level);
		}

		helper.succeed();
	}

	private static void assertFarClusterStaysWithNote(GameTestHelper helper, Block below, int pitch, Block onTop, String what) {
		ServerLevel level = helper.getLevel();
		farLoad(level);
		BlockPos cluster = FAR_NOTE.offset(3, 0, 0);
		farNote(level, below, pitch);
		level.setBlock(FAR_NOTE.above(), onTop.defaultBlockState(), Block.UPDATE_ALL);
		farCrystal(level, cluster, Blocks.AMETHYST_CLUSTER, Direction.UP, Blocks.BUDDING_AMETHYST);

		farFire(helper);

		assertFarBlock(helper, cluster, Blocks.AMETHYST_CLUSTER, what);
		farClear(level);
	}

	@GameTest
	public void noteTwentyThreeBreaksNothing(GameTestHelper helper) {
		assertFarClusterStaysWithNote(helper, Blocks.AMETHYST_BLOCK, TOP_NOTE - 1, Blocks.AIR, "note 23");
		helper.succeed();
	}

	@GameTest
	public void topNoteOnAnotherBlockBreaksNothing(GameTestHelper helper) {
		assertFarClusterStaysWithNote(helper, Blocks.DIRT, TOP_NOTE, Blocks.AIR, "a harp note on dirt");
		assertFarClusterStaysWithNote(helper, Blocks.BUDDING_AMETHYST, TOP_NOTE, Blocks.AIR, "a harp note on budding amethyst");
		assertFarClusterStaysWithNote(helper, Blocks.STONE, TOP_NOTE, Blocks.AIR, "a drum note on stone");
		helper.succeed();
	}

	@GameTest
	public void mobHeadOnTopBreaksNothing(GameTestHelper helper) {
		assertFarClusterStaysWithNote(helper, Blocks.AMETHYST_BLOCK, TOP_NOTE, Blocks.ZOMBIE_HEAD, "a zombie head on top");
		helper.succeed();
	}

	// Blocks go straight into the chunk here: an ordinary setBlock's neighbour updates would load the chunk next door.
	private static LevelChunk loneChunk(GameTestHelper helper, int chunkX, int chunkZ) {
		ServerLevel level = helper.getLevel();
		helper.assertFalse(level.getChunkSource().hasChunk(chunkX, chunkZ), "test chunk should start unloaded");
		return level.getChunk(chunkX, chunkZ);
	}

	private static void assertNeighboursUnloaded(GameTestHelper helper, int chunkX, int chunkZ) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.assertFalse(helper.getLevel().getChunkSource().hasChunk(chunkX + dx, chunkZ + dz), "loaded chunk " + dx + ", " + dz + " from the note");
				}
			}
		}
	}

	private static BlockPos loneNote(LevelChunk chunk, int x, int z) {
		BlockPos note = new BlockPos(SectionPos.sectionToBlockCoord(chunk.getPos().x()) + x, 200, SectionPos.sectionToBlockCoord(chunk.getPos().z()) + z);
		chunk.setBlockState(note, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, TOP_NOTE), 0);
		chunk.setBlockState(note.below(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 0);
		return note;
	}

	@GameTest
	public void clusterWhoseBuddingAmethystIsInAnUnloadedChunkStays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int chunkX = -20_000;
		int chunkZ = -20_000;
		LevelChunk chunk = loneChunk(helper, chunkX, chunkZ);
		BlockPos note = loneNote(chunk, 8, 8);
		BlockPos cluster = note.offset(7, 0, 0);
		chunk.setBlockState(cluster, crystal(Blocks.AMETHYST_CLUSTER, Direction.WEST), 0);

		helper.assertTrue(level.getBlockState(note).triggerEvent(level, note, 0, 0), "the note should sound");

		helper.assertTrue(level.getBlockState(cluster).is(Blocks.AMETHYST_CLUSTER), "a cluster with unknown backing should stay");
		assertNeighboursUnloaded(helper, chunkX, chunkZ);
		helper.succeed();
	}

	@GameTest
	public void sphereReachingUnloadedChunksStillBreaksWhatIsLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int chunkX = -21_000;
		int chunkZ = -21_000;
		LevelChunk chunk = loneChunk(helper, chunkX, chunkZ);
		BlockPos note = loneNote(chunk, 15, 15);
		BlockPos cluster = note.offset(-5, 0, -5);
		chunk.setBlockState(cluster.below(), Blocks.BUDDING_AMETHYST.defaultBlockState(), 0);
		chunk.setBlockState(cluster, crystal(Blocks.AMETHYST_CLUSTER, Direction.UP), 0);

		helper.assertTrue(level.getBlockState(note).triggerEvent(level, note, 0, 0), "the note should sound");

		helper.assertTrue(level.getBlockState(cluster).isAir(), "a cluster in the loaded chunk should still break");
		assertNeighboursUnloaded(helper, chunkX, chunkZ);
		helper.succeed();
	}

	private static final int MEASURED_RUNS = 500;
	private static final int SEARCH_RANGE = 8;
	private static final int BUSIEST_NOTE_BLOCKS = 16;

	// Three cases, far from every test structure, with their blocks left in place because nothing else searches there.
	// Stale palettes: every section in range once held a cluster, so none can be skipped, yet nothing matches.
	// Full sphere: layers of budding amethyst and clusters, so every cluster's backing is checked and it's collected.
	// Untouched: no section in range has ever held a cluster, so all of them are skipped.
	@GameTest
	public void worstCaseShatterSearchFitsInATick(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos stale = new BlockPos(320_007, 200, 320_007);
		BlockPos full = new BlockPos(352_007, 200, 352_007);
		BlockPos untouched = new BlockPos(384_007, 200, 384_007);
		for (BlockPos center : List.of(stale, full, untouched)) {
			loadForSearch(level, center);
		}

		for (BlockPos pos : BlockPos.betweenClosed(stale.offset(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE), stale.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE))) {
			level.setBlock(pos, Blocks.AMETHYST_CLUSTER.defaultBlockState(), Block.UPDATE_NONE);
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
		}

		for (BlockPos pos : BlockPos.betweenClosed(full.offset(-SEARCH_RANGE, -SEARCH_RANGE - 1, -SEARCH_RANGE), full.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE))) {
			boolean budding = (pos.getY() - full.getY()) % 2 != 0;
			level.setBlock(pos, budding ? Blocks.BUDDING_AMETHYST.defaultBlockState() : crystal(Blocks.AMETHYST_CLUSTER, Direction.UP), Block.UPDATE_NONE);
		}

		helper.assertValueEqual(ShatterNoteFeature.grownClustersAround(level, stale).size(), 0, "clusters found with only stale palettes");
		int collected = ShatterNoteFeature.grownClustersAround(level, full).size();
		helper.assertTrue(collected > 500, "the full sphere should collect hundreds of clusters, found " + collected);
		helper.assertValueEqual(ShatterNoteFeature.grownClustersAround(level, untouched).size(), 0, "clusters found in untouched chunks");

		long[] staleNanos = timeSearch(level, stale);
		long[] fullNanos = timeSearch(level, full);
		long[] untouchedNanos = timeSearch(level, untouched);
		long worstMedian = Math.max(staleNanos[MEASURED_RUNS / 2], fullNanos[MEASURED_RUNS / 2]);
		long busiestPerTick = worstMedian * BUSIEST_NOTE_BLOCKS / ShatterNoteFeature.COOLDOWN_TICKS;

		PatinaChime.LOGGER.info(
			"Shatter note search: stale palettes median {} us, p99 {} us, max {} us; full sphere ({} clusters) median {} us, p99 {} us, max {} us; untouched median {} us, max {} us; {} note blocks searching once per cooldown cost {} us per game tick",
			staleNanos[MEASURED_RUNS / 2] / 1_000,
			staleNanos[MEASURED_RUNS * 99 / 100] / 1_000,
			staleNanos[MEASURED_RUNS - 1] / 1_000,
			collected,
			fullNanos[MEASURED_RUNS / 2] / 1_000,
			fullNanos[MEASURED_RUNS * 99 / 100] / 1_000,
			fullNanos[MEASURED_RUNS - 1] / 1_000,
			untouchedNanos[MEASURED_RUNS / 2] / 1_000,
			untouchedNanos[MEASURED_RUNS - 1] / 1_000,
			BUSIEST_NOTE_BLOCKS,
			busiestPerTick / 1_000
		);
		helper.assertTrue(worstMedian < 50_000_000L, "worst case median should stay under one 50 ms tick");
		helper.succeed();
	}

	private static void loadForSearch(ServerLevel level, BlockPos center) {
		for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - SEARCH_RANGE); chunkX <= SectionPos.blockToSectionCoord(center.getX() + SEARCH_RANGE); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - SEARCH_RANGE); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + SEARCH_RANGE); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
	}

	private static long[] timeSearch(ServerLevel level, BlockPos center) {
		long[] nanos = new long[MEASURED_RUNS];
		for (int run = 0; run < MEASURED_RUNS; run++) {
			long start = System.nanoTime();
			ShatterNoteFeature.grownClustersAround(level, center);
			nanos[run] = System.nanoTime() - start;
		}

		Arrays.sort(nanos);
		return nanos;
	}
}
