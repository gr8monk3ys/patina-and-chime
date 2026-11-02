package io.github.gr8monk3ys.patinachime.hopper;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperHopperBlock extends CopperHopperBlock implements WeatheringCopper {
	public static final MapCodec<WeatheringCopperHopperBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperHopperBlock::weatherState), propertiesCodec())
			.apply(instance, WeatheringCopperHopperBlock::new)
	);

	public WeatheringCopperHopperBlock(WeatherState weatherState, Properties properties) {
		super(weatherState, properties);
	}

	@SuppressWarnings("unchecked")
	@Override
	public MapCodec<HopperBlock> codec() {
		return (MapCodec<HopperBlock>) (MapCodec<?>) CODEC;
	}

	@Override
	public WeatherState getAge() {
		return this.weatherState();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.changeOverTime(state, level, pos, random);
	}
}
