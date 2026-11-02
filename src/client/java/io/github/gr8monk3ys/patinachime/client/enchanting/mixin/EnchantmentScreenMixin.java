package io.github.gr8monk3ys.patinachime.client.enchanting.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {
	@WrapOperation(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"
		)
	)
	private void patinachime$revealOffer(
		GuiGraphicsExtractor graphics,
		Font font,
		List<Component> lines,
		int x,
		int y,
		Operation<Void> original,
		@Local(name = "i") int offer
	) {
		EnchantmentScreen screen = (EnchantmentScreen) (Object) this;
		List<Component> revealed = ShardOffers.tooltip(screen.getMenu(), Minecraft.getInstance().level.registryAccess(), offer, lines);
		original.call(graphics, font, revealed, x, y);
	}
}
