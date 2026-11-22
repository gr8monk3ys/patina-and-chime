package io.github.gr8monk3ys.patinachime.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class MusicTrackerTest {
	private static final BlockPos SOURCE = new BlockPos(10, 64, 10);

	@Test
	void markedSourceIsActiveUntilItsExpiryTick() {
		MusicTracker tracker = new MusicTracker();
		tracker.mark(SOURCE, 100, 0);

		assertTrue(tracker.isActiveAt(SOURCE, 99));
		assertFalse(tracker.isActiveAt(SOURCE, 100));
	}

	@Test
	void forgetRemovesASource() {
		MusicTracker tracker = new MusicTracker();
		tracker.mark(SOURCE, 100, 0);
		tracker.forget(SOURCE);

		assertFalse(tracker.isActiveAt(SOURCE, 1));
	}

	@Test
	void musicCountsWithinTheRadiusSphereInclusive() {
		MusicTracker tracker = new MusicTracker();
		tracker.mark(SOURCE, 100, 0);

		assertTrue(tracker.isActiveNear(SOURCE.offset(MusicTracker.RADIUS, 0, 0), 1));
		assertFalse(tracker.isActiveNear(SOURCE.offset(MusicTracker.RADIUS + 1, 0, 0), 1));
		// The corner of the cube is outside the sphere.
		assertFalse(tracker.isActiveNear(SOURCE.offset(MusicTracker.RADIUS, MusicTracker.RADIUS, MusicTracker.RADIUS), 1));
	}

	@Test
	void expiredSourcesAreDroppedWhenSearching() {
		MusicTracker tracker = new MusicTracker();
		tracker.mark(SOURCE, 10, 0);

		assertFalse(tracker.isActiveNear(SOURCE, 10));
		assertEquals(0, tracker.size());
	}

	@Test
	void markingSweepsExpiredSourcesOnceTheSweepIntervalHasPassed() {
		MusicTracker tracker = new MusicTracker();
		tracker.mark(SOURCE, 10, 0);
		tracker.mark(SOURCE.above(), 500, 1);
		assertEquals(2, tracker.size());

		tracker.mark(SOURCE.above(2), 500, 200);
		assertEquals(2, tracker.size());
		assertFalse(tracker.isActiveAt(SOURCE, 200));
	}
}
