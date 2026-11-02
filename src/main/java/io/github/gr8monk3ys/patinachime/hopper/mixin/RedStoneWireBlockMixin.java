package io.github.gr8monk3ys.patinachime.hopper.mixin;

import io.github.gr8monk3ys.patinachime.hopper.CopperHopperBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla names Blocks.HOPPER here, so dust would pop off a copper hopper without this.
@Mixin(RedStoneWireBlock.class)
abstract class RedStoneWireBlockMixin {
	@Inject(method = "canSurviveOn", at = @At("HEAD"), cancellable = true)
	private void patinachime$surviveOnCopperHopper(BlockGetter level, BlockPos relativePos, BlockState relativeState, CallbackInfoReturnable<Boolean> cir) {
		if (relativeState.getBlock() instanceof CopperHopperBlock) {
			cir.setReturnValue(true);
		}
	}
}
