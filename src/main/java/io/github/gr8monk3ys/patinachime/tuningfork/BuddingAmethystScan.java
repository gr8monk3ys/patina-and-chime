package io.github.gr8monk3ys.patinachime.tuningfork;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class BuddingAmethystScan {
	public static final int RANGE = 32;

	private static final Predicate<BlockState> IS_BUDDING = state -> state.is(Blocks.BUDDING_AMETHYST);

	private BuddingAmethystScan() {
	}

	public static TuningForkTier scan(final Level level, final BlockPos center) {
		return TuningForkTier.forDistance(nearestDistance(level, center));
	}

	private static int nearestDistance(final Level level, final BlockPos center) {
		int minSectionX = SectionPos.blockToSectionCoord(center.getX() - RANGE);
		int maxSectionX = SectionPos.blockToSectionCoord(center.getX() + RANGE);
		int minSectionY = SectionPos.blockToSectionCoord(Math.max(center.getY() - RANGE, level.getMinY()));
		int maxSectionY = SectionPos.blockToSectionCoord(Math.min(center.getY() + RANGE, level.getMaxY()));
		int minSectionZ = SectionPos.blockToSectionCoord(center.getZ() - RANGE);
		int maxSectionZ = SectionPos.blockToSectionCoord(center.getZ() + RANGE);
		int nearest = Integer.MAX_VALUE;

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
					if (section.maybeHas(IS_BUDDING)) {
						nearest = Math.min(nearest, nearestInSection(section, sectionX, sectionY, sectionZ, center));
					}
				}
			}
		}

		return nearest;
	}

	private static int nearestInSection(final LevelChunkSection section, final int sectionX, final int sectionY, final int sectionZ, final BlockPos center) {
		int originX = SectionPos.sectionToBlockCoord(sectionX);
		int originY = SectionPos.sectionToBlockCoord(sectionY);
		int originZ = SectionPos.sectionToBlockCoord(sectionZ);
		int nearest = Integer.MAX_VALUE;

		for (int y = Math.max(originY, center.getY() - RANGE); y <= Math.min(originY + 15, center.getY() + RANGE); y++) {
			for (int z = Math.max(originZ, center.getZ() - RANGE); z <= Math.min(originZ + 15, center.getZ() + RANGE); z++) {
				for (int x = Math.max(originX, center.getX() - RANGE); x <= Math.min(originX + 15, center.getX() + RANGE); x++) {
					if (IS_BUDDING.test(section.getBlockState(x & 15, y & 15, z & 15))) {
						nearest = Math.min(nearest, distance(x - center.getX(), y - center.getY(), z - center.getZ()));
					}
				}
			}
		}

		return nearest;
	}

	// Chebyshev distance, so every block in the search cube, corners included, falls in a tier.
	private static int distance(final int dx, final int dy, final int dz) {
		return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
	}
}
