package io.github.gr8monk3ys.patinachime;

import io.github.gr8monk3ys.patinachime.enchanting.EnchantingFeature;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import io.github.gr8monk3ys.patinachime.music.AmethystNoteFeature;
import io.github.gr8monk3ys.patinachime.music.MusicGrowthFeature;
import io.github.gr8monk3ys.patinachime.tuningfork.TuningForkFeature;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatinaChime implements ModInitializer {
	public static final String MOD_ID = "patinachime";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CopperHopperFeature.init();
		VerdigrisFeature.init();
		TuningForkFeature.init();
		EnchantingFeature.init();
		MusicGrowthFeature.init();
		AmethystNoteFeature.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
