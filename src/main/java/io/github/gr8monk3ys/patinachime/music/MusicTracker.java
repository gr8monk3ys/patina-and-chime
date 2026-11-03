package io.github.gr8monk3ys.patinachime.music;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;

/**
 * Where music is playing in one level: packed block position to the first tick it no longer counts.
 * Entries are only added by recording a source, so recording is also what clears out expired ones.
 */
public final class MusicTracker {
	public static final int RADIUS = 8;
	private static final long RADIUS_SQUARED = RADIUS * RADIUS;
	private static final long SWEEP_INTERVAL_TICKS = 100L;

	private final Long2LongOpenHashMap expiries = new Long2LongOpenHashMap();
	private long nextSweepTick;

	public void mark(BlockPos pos, long expiryTick, long now) {
		if (now >= this.nextSweepTick) {
			this.nextSweepTick = now + SWEEP_INTERVAL_TICKS;
			this.expiries.long2LongEntrySet().removeIf(entry -> entry.getLongValue() <= now);
		}

		this.expiries.put(pos.asLong(), expiryTick);
	}

	public void forget(BlockPos pos) {
		this.expiries.remove(pos.asLong());
	}

	public boolean isActiveAt(BlockPos pos, long now) {
		return this.expiries.get(pos.asLong()) > now;
	}

	public boolean isActiveNear(BlockPos pos, long now) {
		ObjectIterator<Long2LongMap.Entry> entries = this.expiries.long2LongEntrySet().fastIterator();
		while (entries.hasNext()) {
			Long2LongMap.Entry entry = entries.next();
			if (entry.getLongValue() <= now) {
				entries.remove();
			} else if (isWithinRadius(pos, entry.getLongKey())) {
				return true;
			}
		}

		return false;
	}

	public int size() {
		return this.expiries.size();
	}

	public static boolean isWithinRadius(BlockPos pos, BlockPos source) {
		return isWithinRadius(pos, source.getX(), source.getY(), source.getZ());
	}

	private static boolean isWithinRadius(BlockPos pos, long source) {
		return isWithinRadius(pos, BlockPos.getX(source), BlockPos.getY(source), BlockPos.getZ(source));
	}

	private static boolean isWithinRadius(BlockPos pos, int sourceX, int sourceY, int sourceZ) {
		long dx = pos.getX() - sourceX;
		long dy = pos.getY() - sourceY;
		long dz = pos.getZ() - sourceZ;
		return dx * dx + dy * dy + dz * dz <= RADIUS_SQUARED;
	}
}
