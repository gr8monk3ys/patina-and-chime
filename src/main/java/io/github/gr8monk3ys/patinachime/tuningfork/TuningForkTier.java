package io.github.gr8monk3ys.patinachime.tuningfork;

import net.minecraft.sounds.SoundEvent;

public enum TuningForkTier {
	NONE(TuningForkFeature.SOUND_NONE, "none", 0.8F, 0.7F),
	FAINT(TuningForkFeature.SOUND_FAINT, "faint", 0.6F, 0.6F),
	STEADY(TuningForkFeature.SOUND_STEADY, "steady", 0.8F, 1.0F),
	STRONG(TuningForkFeature.SOUND_STRONG, "strong", 1.0F, 1.6F);

	private final SoundEvent sound;
	private final String messageKey;
	private final float volume;
	private final float pitch;

	TuningForkTier(final SoundEvent sound, final String name, final float volume, final float pitch) {
		this.sound = sound;
		this.messageKey = "message.patinachime.tuning_fork." + name;
		this.volume = volume;
		this.pitch = pitch;
	}

	public static TuningForkTier forDistance(final int distance) {
		if (distance <= 8) {
			return STRONG;
		} else if (distance <= 16) {
			return STEADY;
		} else {
			return distance <= 32 ? FAINT : NONE;
		}
	}

	public SoundEvent sound() {
		return this.sound;
	}

	public String messageKey() {
		return this.messageKey;
	}

	public float volume() {
		return this.volume;
	}

	public float pitch() {
		return this.pitch;
	}
}
