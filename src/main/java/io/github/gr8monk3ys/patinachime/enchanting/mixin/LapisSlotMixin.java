package io.github.gr8monk3ys.patinachime.enchanting.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.inventory.EnchantmentMenu$3")
public abstract class LapisSlotMixin {
	@ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
	private boolean patinachime$acceptShards(boolean mayPlace, @Local(argsOnly = true) ItemStack stack) {
		return mayPlace || ShardOffers.isShards(stack);
	}
}
