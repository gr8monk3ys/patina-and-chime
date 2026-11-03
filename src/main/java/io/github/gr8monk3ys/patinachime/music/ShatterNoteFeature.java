package io.github.gr8monk3ys.patinachime.music;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The top note of an amethyst note block breaks every grown cluster in range. With no tool, the vanilla loot table gives
 * 2 shards instead of a pickaxe's 4, so the note is a hands-free harvest at half yield.
 */
public final class ShatterNoteFeature {
	public static final int TOP_NOTE = 24;
	public static final int COOLDOWN_TICKS = 20;

	private static final int RANGE = MusicTracker.RADIUS;
	private static final Predicate<BlockState> IS_CLUSTER = state -> state.is(Blocks.AMETHYST_CLUSTER);

	// Which note blocks searched recently. Not persistent, like music, and held by the level object.
	private static final AttachmentType<MusicTracker> COOLDOWNS = AttachmentRegistry.createDefaulted(PatinaChime.id("shatter_cooldowns"), MusicTracker::new);

	private ShatterNoteFeature() {
	}

	public static void init() {
	}

	public static MusicTracker cooldowns(ServerLevel level) {
		return level.getAttachedOrCreate(COOLDOWNS);
	}

	public static void onNotePlayed(ServerLevel level, BlockPos note) {
		BlockState state = level.getBlockState(note);
		if (state.is(Blocks.NOTE_BLOCK) && state.getValue(NoteBlock.NOTE) == TOP_NOTE && AmethystNoteFeature.isAmethystNote(level, note)) {
			MusicTracker cooldowns = cooldowns(level);
			long now = level.getGameTime();
			if (cooldowns.isActiveAt(note, now)) {
				return;
			}

			cooldowns.mark(note, now + COOLDOWN_TICKS, now);
			// Found first and broken after, so that block updates from breaking can't disturb the search.
			for (BlockPos cluster : grownClustersAround(level, note)) {
				if (IS_CLUSTER.test(level.getBlockState(cluster))) {
					level.destroyBlock(cluster, true);
				}
			}
		}
	}

	public static List<BlockPos> grownClustersAround(ServerLevel level, BlockPos note) {
		List<BlockPos> clusters = new ArrayList<>();
		int minSectionX = SectionPos.blockToSectionCoord(note.getX() - RANGE);
		int maxSectionX = SectionPos.blockToSectionCoord(note.getX() + RANGE);
		int minSectionY = SectionPos.blockToSectionCoord(Math.max(note.getY() - RANGE, level.getMinY()));
		int maxSectionY = SectionPos.blockToSectionCoord(Math.min(note.getY() + RANGE, level.getMaxY()));
		int minSectionZ = SectionPos.blockToSectionCoord(note.getZ() - RANGE);
		int maxSectionZ = SectionPos.blockToSectionCoord(note.getZ() + RANGE);

		for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
			for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
				// getChunkNow only reads what is already loaded; it never schedules a load or generation.
				LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
				if (chunk == null) {
					continue;
				}

				LevelChunkSection[] sections = chunk.getSections();
				for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
					LevelChunkSection section = sections[level.getSectionIndexFromSectionY(sectionY)];
					if (section.maybeHas(IS_CLUSTER)) {
						collectFromSection(level, section, sectionX, sectionY, sectionZ, note, clusters);
					}
				}
			}
		}

		return clusters;
	}

	private static void collectFromSection(ServerLevel level, LevelChunkSection section, int sectionX, int sectionY, int sectionZ, BlockPos note, List<BlockPos> clusters) {
		int originX = SectionPos.sectionToBlockCoord(sectionX);
		int originY = SectionPos.sectionToBlockCoord(sectionY);
		int originZ = SectionPos.sectionToBlockCoord(sectionZ);

		for (int y = Math.max(originY, note.getY() - RANGE); y <= Math.min(originY + 15, note.getY() + RANGE); y++) {
			for (int z = Math.max(originZ, note.getZ() - RANGE); z <= Math.min(originZ + 15, note.getZ() + RANGE); z++) {
				for (int x = Math.max(originX, note.getX() - RANGE); x <= Math.min(originX + 15, note.getX() + RANGE); x++) {
					BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
					if (IS_CLUSTER.test(state)) {
						BlockPos pos = new BlockPos(x, y, z);
						if (MusicTracker.isWithinRadius(pos, note) && isOnBuddingAmethyst(level, pos, state)) {
							clusters.add(pos);
						}
					}
				}
			}
		}
	}

	// isLoaded first, because getBlockState would load the chunk a cluster on a chunk edge is backed by.
	private static boolean isOnBuddingAmethyst(ServerLevel level, BlockPos cluster, BlockState state) {
		BlockPos backing = cluster.relative(state.getValue(AmethystClusterBlock.FACING).getOpposite());
		return level.isLoaded(backing) && level.getBlockState(backing).is(Blocks.BUDDING_AMETHYST);
	}
}
