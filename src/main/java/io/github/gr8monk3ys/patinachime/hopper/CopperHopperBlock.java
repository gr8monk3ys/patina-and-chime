package io.github.gr8monk3ys.patinachime.hopper;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class CopperHopperBlock extends HopperBlock {
	public static final MapCodec<CopperHopperBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperHopperBlock::weatherState), propertiesCodec())
			.apply(instance, CopperHopperBlock::new)
	);

	private final WeatherState weatherState;

	public CopperHopperBlock(WeatherState weatherState, Properties properties) {
		super(properties);
		this.weatherState = weatherState;
	}

	public WeatherState weatherState() {
		return this.weatherState;
	}

	public CopperHopperStage stage() {
		return CopperHopperStage.of(this.weatherState);
	}

	@SuppressWarnings("unchecked")
	@Override
	public MapCodec<HopperBlock> codec() {
		return (MapCodec<HopperBlock>) (MapCodec<?>) CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopperHopperBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, CopperHopperFeature.BLOCK_ENTITY, CopperHopperBlockEntity::tick);
	}

	// The default result opens the menu first, so axes, honeycomb and verdigris would only work while sneaking.
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return changesStage(stack, state.getBlock(), player, hand) ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	private static boolean changesStage(ItemStack stack, Block block, Player player, InteractionHand hand) {
		if (stack.is(ItemTags.AXES)) {
			// AxeItem leaves copper alone when an offhand shield means the player wants to block, so the menu opens instead.
			boolean wantsToBlock = hand == InteractionHand.MAIN_HAND && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
			return !wantsToBlock && (WeatheringCopper.getPrevious(block).isPresent() || HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(block));
		}

		if (stack.is(Items.HONEYCOMB)) {
			return HoneycombItem.WAXABLES.get().containsKey(block);
		}

		return stack.is(VerdigrisFeature.VERDIGRIS) && WeatheringCopper.getNext(block).isPresent();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CopperHopperBlockEntity hopper) {
			player.openMenu(hopper);
			player.awardStat(Stats.INSPECT_HOPPER);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (level.getBlockEntity(pos) instanceof CopperHopperBlockEntity hopper) {
			CopperHopperBlockEntity.entityInside(level, pos, state, entity, hopper);
		}
	}

	@Override
	public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
		return oldState.getBlock() instanceof CopperHopperBlock;
	}
}
