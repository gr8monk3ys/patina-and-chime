package io.github.gr8monk3ys.patinachime.music.mixin;

import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
abstract class JukeboxBlockEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private static void patinachime$recordWhilePlaying(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo ci) {
		MusicGrowthFeature.syncJukebox(level, pos, jukebox.getSongPlayer().isPlaying());
	}

	@Inject(method = "onSongChanged", at = @At("TAIL"))
	private void patinachime$recordSongChange(CallbackInfo ci) {
		JukeboxBlockEntity jukebox = (JukeboxBlockEntity) (Object) this;
		MusicGrowthFeature.syncJukebox(jukebox.getLevel(), jukebox.getBlockPos(), jukebox.getSongPlayer().isPlaying());
	}

	@Inject(method = "setRemoved", at = @At("TAIL"))
	private void patinachime$forgetWhenRemoved(CallbackInfo ci) {
		JukeboxBlockEntity jukebox = (JukeboxBlockEntity) (Object) this;
		MusicGrowthFeature.syncJukebox(jukebox.getLevel(), jukebox.getBlockPos(), false);
	}
}
