package io.github.gr8monk3ys.patinachime.enchanting;

import net.minecraft.world.inventory.DataSlot;

/**
 * Carries the high 16 bits of the enchantment seed to the client. Menu data travels as shorts
 * (ClientboundContainerSetDataPacket), so vanilla's seed slot only delivers the low 16 bits. That is enough for its
 * rune text, but rerunning the enchantment roll with it reveals the wrong enchantments.
 */
public final class SeedHighBitsSlot extends DataSlot {
	private final DataSlot seed;
	private boolean received;
	private int receivedBits;

	public SeedHighBitsSlot(DataSlot seed) {
		this.seed = seed;
	}

	@Override
	public int get() {
		return this.received ? this.receivedBits : this.seed.get() >> 16;
	}

	@Override
	public void set(int bits) {
		this.receivedBits = bits;
		this.received = true;
	}

	public int fullSeed(int syncedSeed) {
		return this.get() << 16 | syncedSeed & 0xFFFF;
	}
}
