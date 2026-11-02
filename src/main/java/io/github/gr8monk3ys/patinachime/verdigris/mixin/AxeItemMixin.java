package io.github.gr8monk3ys.patinachime.verdigris.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AxeItem.class)
abstract class AxeItemMixin {
	@WrapOperation(
		method = "useOn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
	)
	private boolean patinachime$dropVerdigris(
		final Level level, final BlockPos pos, final BlockState newState, final int flags, final Operation<Boolean> original, @Local(argsOnly = true) final UseOnContext context
	) {
		BlockState oldState = level.getBlockState(pos);
		boolean changed = original.call(level, pos, newState, flags);
		if (changed) {
			VerdigrisFeature.onAxeChangedBlock(context, oldState, newState);
		}

		return changed;
	}
}
