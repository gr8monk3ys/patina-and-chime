package io.github.gr8monk3ys.patinachime.test;

import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SmokeGameTests {
	@GameTest
	public void modIsLoaded(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded("patinachime"), Component.literal("patinachime is not loaded"));
		helper.succeed();
	}

	@GameTest
	public void everyRecipeUnlocksInTheRecipeBook(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		Set<ResourceKey<Recipe<?>>> unlocked = server.getAdvancements().getAllAdvancements().stream()
			.filter(advancement -> advancement.id().getNamespace().equals("patinachime"))
			.flatMap(advancement -> advancement.value().rewards().recipes().stream())
			.collect(Collectors.toSet());
		for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
			if (recipe.id().identifier().getNamespace().equals("patinachime")) {
				helper.assertTrue(unlocked.contains(recipe.id()), Component.literal(recipe.id().identifier() + " has no unlock"));
			}
		}

		helper.succeed();
	}
}
