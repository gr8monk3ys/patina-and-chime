package io.github.gr8monk3ys.patinachime.music;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MusicGrowthFeature {
	public static final int BOOSTED_GROWTH_CHANCE = 2;
	public static final int NOTE_ACTIVE_TICKS = 100;
	// Random ticks run before block entities, so a mark must outlive the next tick's random ticks.
	private static final int JUKEBOX_MARK_TICKS = 2;
	private static final int BOOST_PARTICLES = 3;
	private static final double BOOST_PARTICLE_OFFSET = 0.75;
	private static final double BOOST_PARTICLE_SPREAD = 0.2;
	private static final double BOOST_PARTICLE_COLOR_RANGE = 0.5;

	// Not persistent, and held by the level object, so it is gone as soon as the level is.
	private static final AttachmentType<MusicTracker> TRACKER = AttachmentRegistry.createDefaulted(PatinaChime.id("music_tracker"), MusicTracker::new);

	private MusicGrowthFeature() {
	}

	public static void init() {
	}

	public static MusicTracker tracker(ServerLevel level) {
		return level.getAttachedOrCreate(TRACKER);
	}

	public static void onNotePlayed(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			long now = level.getGameTime();
			tracker(serverLevel).mark(pos, now + NOTE_ACTIVE_TICKS, now);
		}
	}

	public static void syncJukebox(@Nullable Level level, BlockPos pos, boolean playing) {
		if (level instanceof ServerLevel serverLevel) {
			MusicTracker tracker = tracker(serverLevel);
			if (playing) {
				long now = level.getGameTime();
				tracker.mark(pos, now + JUKEBOX_MARK_TICKS, now);
			} else {
				tracker.forget(pos);
			}
		}
	}

	public static int growthChance(ServerLevel level, BlockPos pos, int vanillaChance) {
		if (!tracker(level).isActiveNear(pos, level.getGameTime())) {
			return vanillaChance;
		}

		showBoost(level, pos);
		return BOOSTED_GROWTH_CHANCE;
	}

	private static void showBoost(ServerLevel level, BlockPos pos) {
		for (Direction face : Direction.allShuffled(level.getRandom())) {
			if (!level.getBlockState(pos.relative(face)).isSolidRender()) {
				Vec3 at = Vec3.atCenterOf(pos).relative(face, BOOST_PARTICLE_OFFSET);
				level.sendParticles(
					ParticleTypes.NOTE,
					at.x(),
					at.y(),
					at.z(),
					BOOST_PARTICLES,
					BOOST_PARTICLE_SPREAD,
					BOOST_PARTICLE_SPREAD,
					BOOST_PARTICLE_SPREAD,
					BOOST_PARTICLE_COLOR_RANGE
				);
				return;
			}
		}
	}
}
