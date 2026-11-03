package io.github.gr8monk3ys.patinachime.hopper;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.WeatheringCopper;

public class CopperHopperItem extends BlockItem {
	public CopperHopperItem(CopperHopperBlock block, Properties properties) {
		super(block, properties);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, builder, flag);
		CopperHopperBlock block = (CopperHopperBlock) this.getBlock();
		builder.accept(Component.translatable(block.stage().tooltipKey()).withStyle(ChatFormatting.GRAY));
		if (WeatheringCopper.getNext(block).isPresent()) {
			builder.accept(Component.translatable("tooltip.patinachime.hopper.ages").withStyle(ChatFormatting.GOLD));
		} else if (!(block instanceof WeatheringCopper)) {
			builder.accept(Component.translatable("tooltip.patinachime.hopper.waxed").withStyle(ChatFormatting.GRAY));
		}
	}
}
