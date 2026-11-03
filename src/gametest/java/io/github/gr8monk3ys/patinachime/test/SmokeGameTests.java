package io.github.gr8monk3ys.patinachime.test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

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

	private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
		ServerLevel level = helper.getLevel();
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
			.orElseThrow(() -> helper.assertionException("no crafting recipe matched " + input))
			.value().assemble(input);
	}

	@GameTest
	public void anAmethystBlockAndFourShardsCraftIntoEachOther(GameTestHelper helper) {
		ItemStack shards = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(Items.AMETHYST_BLOCK))));
		helper.assertTrue(shards.is(Items.AMETHYST_SHARD), "a block should make shards, not " + shards);
		helper.assertValueEqual(shards.getCount(), 4, "shards from a block");

		// Vanilla's own recipe is the way back, so the two directions have to stay equal.
		ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
		ItemStack block = craft(helper, CraftingInput.of(2, 2, List.of(shard, shard, shard, shard)));
		helper.assertTrue(block.is(Items.AMETHYST_BLOCK), "four shards should make a block, not " + block);
		helper.assertValueEqual(block.getCount(), 1, "blocks from four shards");
		helper.succeed();
	}
}
