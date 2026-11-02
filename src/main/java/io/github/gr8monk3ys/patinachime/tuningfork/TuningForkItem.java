package io.github.gr8monk3ys.patinachime.tuningfork;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class TuningForkItem extends Item {
	public TuningForkItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!level.isClientSide()) {
			TuningForkTier tier = BuddingAmethystScan.scan(level, player.blockPosition());
			level.playSound(null, player.getX(), player.getY(), player.getZ(), tier.sound(), SoundSource.PLAYERS, tier.volume(), tier.pitch());
			player.sendOverlayMessage(Component.translatable(tier.messageKey()));
		}

		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(
		final ItemStack itemStack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag tooltipFlag
	) {
		builder.accept(Component.translatable("tooltip.patinachime.tuning_fork").withStyle(ChatFormatting.GRAY));
	}
}
