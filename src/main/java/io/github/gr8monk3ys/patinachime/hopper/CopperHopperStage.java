package io.github.gr8monk3ys.patinachime.hopper;

import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

public enum CopperHopperStage {
	FRESH(12, true, true, false, "fresh"),
	EXPOSED(16, false, true, false, "exposed"),
	WEATHERED(24, true, true, true, "weathered"),
	OXIDIZED(32, true, false, false, "oxidized");

	private final int ticksPerItem;
	private final boolean pullsFromAbove;
	private final boolean picksUpItems;
	private final boolean runsOnlyWhilePowered;
	private final String tooltipKey;

	CopperHopperStage(int ticksPerItem, boolean pullsFromAbove, boolean picksUpItems, boolean runsOnlyWhilePowered, String tooltipKey) {
		this.ticksPerItem = ticksPerItem;
		this.pullsFromAbove = pullsFromAbove;
		this.picksUpItems = picksUpItems;
		this.runsOnlyWhilePowered = runsOnlyWhilePowered;
		this.tooltipKey = tooltipKey;
	}

	public static CopperHopperStage of(WeatherState weatherState) {
		return switch (weatherState) {
			case UNAFFECTED -> FRESH;
			case EXPOSED -> EXPOSED;
			case WEATHERED -> WEATHERED;
			case OXIDIZED -> OXIDIZED;
		};
	}

	public int ticksPerItem() {
		return this.ticksPerItem;
	}

	public boolean pullsFromAbove() {
		return this.pullsFromAbove;
	}

	public boolean picksUpItems() {
		return this.picksUpItems;
	}

	public boolean runsWhen(boolean powered) {
		return powered == this.runsOnlyWhilePowered;
	}

	public String tooltipKey() {
		return "tooltip.patinachime.hopper." + this.tooltipKey;
	}
}
