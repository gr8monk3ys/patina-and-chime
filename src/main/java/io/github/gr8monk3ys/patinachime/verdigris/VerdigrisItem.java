package io.github.gr8monk3ys.patinachime.verdigris;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;

public class VerdigrisItem extends Item {
	public VerdigrisItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState oldState = level.getBlockState(pos);
		Optional<BlockState> aged = WeatheringCopper.getNext(oldState.getBlock()).map(next -> next.withPropertiesOf(oldState));
		if (aged.isEmpty()) {
			return InteractionResult.PASS;
		}

		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
		}

		stack.consume(1, player);
		// The immediate shape update is what carries the change to the other half of a door or double chest.
		level.setBlock(pos, aged.get(), Block.UPDATE_ALL_IMMEDIATE);
		level.playSound(player, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
		announceChange(level, player, pos, aged.get());
		if (oldState.getBlock() instanceof ChestBlock && oldState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			announceChange(level, player, ChestBlock.getConnectedBlockPos(pos, oldState), aged.get());
		}

		return InteractionResult.SUCCESS;
	}

	private static void announceChange(final Level level, final Player player, final BlockPos pos, final BlockState state) {
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
		level.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0);
	}

	@Override
	public void appendHoverText(
		final ItemStack itemStack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag tooltipFlag
	) {
		builder.accept(Component.translatable("tooltip.patinachime.verdigris").withStyle(ChatFormatting.GRAY));
	}
}
