package io.github.gr8monk3ys.patinachime.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

public class SmokeGameTests {
	@GameTest
	public void modIsLoaded(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded("patinachime"), Component.literal("patinachime is not loaded"));
		helper.succeed();
	}
}
