package io.github.gr8monk3ys.patinachime.music.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BuddingAmethystBlock.class)
abstract class BuddingAmethystBlockMixin {
	@ModifyArg(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0))
	private int patinachime$musicGrowthChance(int vanillaChance, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
		return MusicGrowthFeature.growthChance(level, pos, vanillaChance);
	}
}
