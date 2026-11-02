package io.github.gr8monk3ys.patinachime.music.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.gr8monk3ys.patinachime.music.AmethystNoteFeature;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
abstract class NoteBlockMixin {
	@ModifyExpressionValue(
		method = "triggerEvent",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/properties/NoteBlockInstrument;getSoundEvent()Lnet/minecraft/core/Holder;")
	)
	private Holder<SoundEvent> patinachime$amethystSound(Holder<SoundEvent> vanillaSound, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
		return AmethystNoteFeature.noteSound(level, pos, vanillaSound);
	}

	// triggerEvent, not playNote: only here is a note certain to sound. A head with no sound returns false, and a block
	// swapped out before the queued event runs never gets here.
	@Inject(method = "triggerEvent", at = @At("RETURN"))
	private void patinachime$recordNote(BlockState state, Level level, BlockPos pos, int b0, int b1, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			MusicGrowthFeature.onNotePlayed(level, pos);
		}
	}
}
