package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class VerdigrisGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);
	private static final BlockPos TOP = TARGET.above();
	private static final double NEARBY = 2.5;

	private static ServerPlayer holding(GameTestHelper helper, GameType gameType, ItemStack stack) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static ItemStack flakes(int count) {
		return new ItemStack(VerdigrisFeature.VERDIGRIS, count);
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos relative, Direction face) {
		BlockPos absolute = helper.absolutePos(relative);
		return new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false);
	}

	private static InteractionResult rightClick(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction face) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, relative, face));
	}

	private static InteractionResult rightClick(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
		return rightClick(helper, player, relative, Direction.UP);
	}

	private static void placeDoor(GameTestHelper helper, Block door) {
		helper.setBlock(TARGET.below(), Blocks.STONE);
		helper.setBlock(TARGET, door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(TOP, door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static void assertDoor(GameTestHelper helper, Block door, boolean open) {
		helper.assertBlockPresent(door, TARGET);
		helper.assertBlockPresent(door, TOP);
		helper.assertBlockProperty(TARGET, DoorBlock.HALF, DoubleBlockHalf.LOWER);
		helper.assertBlockProperty(TOP, DoorBlock.HALF, DoubleBlockHalf.UPPER);
		helper.assertBlockProperty(TARGET, DoorBlock.OPEN, open);
		helper.assertBlockProperty(TOP, DoorBlock.OPEN, open);
	}

	private static void assertCount(GameTestHelper helper, ItemStack stack, int expected) {
		helper.assertValueEqual(stack.getCount(), expected, "stack count");
	}

	@GameTest
	public void scrapingOxidationDropsOneFlakeAtTheClickedFace(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().weathered());
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, TARGET, Direction.EAST);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().exposed(), TARGET);
		helper.assertItemEntityCountIs(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY, 1);
		ItemEntity flake = helper.getEntities(EntityTypes.ITEM, TARGET, NEARBY).getFirst();
		helper.assertTrue(flake.getX() > helper.absolutePos(TARGET).getX() + 1.0, "flake should spawn outside the clicked east face");
		helper.succeed();
	}

	@GameTest
	public void removingWaxDropsNothing(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.waxed().weathered());
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().weathered(), TARGET);
		helper.assertItemEntityNotPresent(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY);
		helper.succeed();
	}

	@GameTest
	public void strippingALogDropsNothing(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.OAK_LOG);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, TARGET);
		helper.assertItemEntityNotPresent(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY);
		helper.succeed();
	}

	@GameTest
	public void scrapingFreshCopperChangesNothingAndDropsNothing(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().unaffected());
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().unaffected(), TARGET);
		helper.assertItemEntityNotPresent(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY);
		helper.succeed();
	}

	@GameTest
	public void scrapingInCreativeDropsNothing(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().oxidized());
		ServerPlayer player = holding(helper, GameType.CREATIVE, new ItemStack(Items.IRON_AXE));

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().weathered(), TARGET);
		helper.assertItemEntityNotPresent(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY);
		helper.succeed();
	}

	@GameTest
	public void scrapingADoorChangesBothHalvesForOneFlake(GameTestHelper helper) {
		placeDoor(helper, Blocks.COPPER_DOOR.weathering().oxidized());
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));
		player.setShiftKeyDown(true);

		rightClick(helper, player, TOP);

		assertDoor(helper, Blocks.COPPER_DOOR.weathering().weathered(), false);
		helper.assertItemEntityCountIs(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY + 1.0, 1);
		helper.succeed();
	}

	@GameTest
	public void scrapeThenReapplyNetsToZero(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().exposed());
		ServerPlayer scraper = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));

		rightClick(helper, scraper, TARGET);

		ItemEntity flake = helper.getEntities(EntityTypes.ITEM, TARGET, NEARBY).getFirst();
		ServerPlayer ager = holding(helper, GameType.SURVIVAL, flake.getItem());
		rightClick(helper, ager, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().exposed(), TARGET);
		helper.assertTrue(ager.getMainHandItem().isEmpty(), "the only flake should be spent");
		helper.succeed();
	}

	@GameTest
	public void flakeAgesOneStageAndConsumesOne(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		helper.assertTrue(rightClick(helper, player, TARGET).consumesAction(), "aging should consume the action");
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().exposed(), TARGET);
		assertCount(helper, stack, 4);

		rightClick(helper, player, TARGET);
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().weathered(), TARGET);
		assertCount(helper, stack, 3);

		rightClick(helper, player, TARGET);
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().oxidized(), TARGET);
		assertCount(helper, stack, 2);
		helper.succeed();
	}

	@GameTest
	public void flakeInCreativeAgesWithoutBeingConsumed(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.CREATIVE, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().exposed(), TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeDoesNothingOnWaxedCopper(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.waxed().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		helper.assertFalse(rightClick(helper, player, TARGET).consumesAction(), "waxed copper should not consume the action");

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.waxed().unaffected(), TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeDoesNothingOnFullyOxidizedCopper(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().oxidized());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		helper.assertFalse(rightClick(helper, player, TARGET).consumesAction(), "oxidized copper should not consume the action");

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().oxidized(), TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeDoesNothingOnOtherBlocks(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.STONE);
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.STONE, TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeAgesEveryBlockInTheWeatheringMap(GameTestHelper helper) {
		WeatheringCopper.NEXT_BY_BLOCK.get().forEach((block, next) -> {
			// Another mod's copper rail needs a floor, which this test doesn't build.
			String namespace = BuiltInRegistries.BLOCK.getKey(block).getNamespace();
			if (!namespace.equals("minecraft") && !namespace.equals(PatinaChime.MOD_ID)) {
				return;
			}

			helper.setBlock(TARGET, block);
			ItemStack stack = flakes(2);
			ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

			rightClick(helper, player, TARGET);

			helper.assertBlockPresent(next, TARGET);
			assertCount(helper, stack, 1);
		});
		helper.succeed();
	}

	@GameTest
	public void adventurePlayersCannotAge(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.ADVENTURE, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().unaffected(), TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void spectatorsCannotAge(GameTestHelper helper) {
		helper.setBlock(TARGET, Blocks.COPPER_BLOCK.weathering().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SPECTATOR, stack);

		InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit(helper, TARGET, Direction.UP));

		helper.assertValueEqual(result, InteractionResult.PASS, "callback result");
		helper.assertBlockPresent(Blocks.COPPER_BLOCK.weathering().unaffected(), TARGET);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeAgesBothDoorHalvesFromEitherHalfWithoutOpeningIt(GameTestHelper helper) {
		placeDoor(helper, Blocks.COPPER_DOOR.weathering().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);
		assertDoor(helper, Blocks.COPPER_DOOR.weathering().exposed(), false);
		assertCount(helper, stack, 4);

		rightClick(helper, player, TOP);
		assertDoor(helper, Blocks.COPPER_DOOR.weathering().weathered(), false);
		assertCount(helper, stack, 3);
		helper.succeed();
	}

	@GameTest
	public void flakeOnAWaxedDoorIsSpentOnNothing(GameTestHelper helper) {
		placeDoor(helper, Blocks.COPPER_DOOR.waxed().unaffected());
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_DOOR.waxed().unaffected(), TARGET);
		helper.assertBlockPresent(Blocks.COPPER_DOOR.waxed().unaffected(), TOP);
		assertCount(helper, stack, 5);
		helper.succeed();
	}

	@GameTest
	public void flakeKeepsAnOpenTrapdoorOpen(GameTestHelper helper) {
		Block trapdoor = Blocks.COPPER_TRAPDOOR.weathering().unaffected();
		helper.setBlock(TARGET, trapdoor.defaultBlockState().setValue(TrapDoorBlock.OPEN, true));
		ItemStack stack = flakes(2);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_TRAPDOOR.weathering().exposed(), TARGET);
		helper.assertBlockProperty(TARGET, TrapDoorBlock.OPEN, true);
		assertCount(helper, stack, 1);
		helper.succeed();
	}

	@GameTest
	public void flakeAgesBothHalvesOfADoubleChestAndKeepsItsContents(GameTestHelper helper) {
		BlockState right = Blocks.COPPER_CHEST.weathering().unaffected().defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.NORTH)
			.setValue(ChestBlock.TYPE, ChestType.RIGHT);
		BlockPos leftPos = TARGET.relative(ChestBlock.getConnectedDirection(right));
		helper.setBlock(TARGET, right);
		helper.setBlock(leftPos, right.setValue(ChestBlock.TYPE, ChestType.LEFT));
		ChestBlockEntity rightEntity = helper.getBlockEntity(TARGET, ChestBlockEntity.class);
		ChestBlockEntity leftEntity = helper.getBlockEntity(leftPos, ChestBlockEntity.class);
		rightEntity.setItem(0, new ItemStack(Items.DIAMOND, 5));
		leftEntity.setItem(26, new ItemStack(Items.EMERALD, 7));
		ItemStack stack = flakes(5);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);
		assertChest(helper, leftPos, Blocks.COPPER_CHEST.weathering().exposed(), rightEntity, leftEntity);
		assertCount(helper, stack, 4);

		rightClick(helper, player, leftPos);
		assertChest(helper, leftPos, Blocks.COPPER_CHEST.weathering().weathered(), rightEntity, leftEntity);
		assertCount(helper, stack, 3);
		helper.succeed();
	}

	private static void assertChest(GameTestHelper helper, BlockPos leftPos, Block chest, ChestBlockEntity rightEntity, ChestBlockEntity leftEntity) {
		helper.assertBlockPresent(chest, TARGET);
		helper.assertBlockPresent(chest, leftPos);
		helper.assertBlockProperty(TARGET, ChestBlock.TYPE, ChestType.RIGHT);
		helper.assertBlockProperty(leftPos, ChestBlock.TYPE, ChestType.LEFT);
		helper.assertTrue(helper.getBlockEntity(TARGET, ChestBlockEntity.class) == rightEntity, "right chest entity should be kept");
		helper.assertTrue(helper.getBlockEntity(leftPos, ChestBlockEntity.class) == leftEntity, "left chest entity should be kept");
		helper.assertValueEqual(rightEntity.getItem(0).getCount(), 5, "diamonds in the right half");
		helper.assertTrue(rightEntity.getItem(0).is(Items.DIAMOND), "right half should still hold diamonds");
		helper.assertValueEqual(leftEntity.getItem(26).getCount(), 7, "emeralds in the left half");
		helper.assertTrue(leftEntity.getItem(26).is(Items.EMERALD), "left half should still hold emeralds");
	}

	@GameTest
	public void scrapingADoubleChestKeepsContentsForOneFlake(GameTestHelper helper) {
		BlockState right = Blocks.COPPER_CHEST.weathering().oxidized().defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.NORTH)
			.setValue(ChestBlock.TYPE, ChestType.RIGHT);
		BlockPos leftPos = TARGET.relative(ChestBlock.getConnectedDirection(right));
		helper.setBlock(TARGET, right);
		helper.setBlock(leftPos, right.setValue(ChestBlock.TYPE, ChestType.LEFT));
		ChestBlockEntity rightEntity = helper.getBlockEntity(TARGET, ChestBlockEntity.class);
		ChestBlockEntity leftEntity = helper.getBlockEntity(leftPos, ChestBlockEntity.class);
		rightEntity.setItem(0, new ItemStack(Items.DIAMOND, 5));
		leftEntity.setItem(26, new ItemStack(Items.EMERALD, 7));
		ServerPlayer player = holding(helper, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));
		player.setShiftKeyDown(true);

		rightClick(helper, player, TARGET);

		assertChest(helper, leftPos, Blocks.COPPER_CHEST.weathering().weathered(), rightEntity, leftEntity);
		helper.assertItemEntityCountIs(VerdigrisFeature.VERDIGRIS, TARGET, NEARBY + 1.0, 1);
		helper.succeed();
	}

	@GameTest
	public void flakeAgesAGolemStatueWithoutChangingItsPose(GameTestHelper helper) {
		BlockState statue = Blocks.COPPER_GOLEM_STATUE.weathering().unaffected().defaultBlockState()
			.setValue(CopperGolemStatueBlock.FACING, Direction.EAST)
			.setValue(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.SITTING);
		helper.setBlock(TARGET, statue);
		CopperGolemStatueBlockEntity entity = helper.getBlockEntity(TARGET, CopperGolemStatueBlockEntity.class);
		ItemStack stack = flakes(2);
		ServerPlayer player = holding(helper, GameType.SURVIVAL, stack);

		rightClick(helper, player, TARGET);

		helper.assertBlockPresent(Blocks.COPPER_GOLEM_STATUE.weathering().exposed(), TARGET);
		helper.assertBlockProperty(TARGET, CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.SITTING);
		helper.assertBlockProperty(TARGET, CopperGolemStatueBlock.FACING, Direction.EAST);
		helper.assertTrue(helper.getBlockEntity(TARGET, CopperGolemStatueBlockEntity.class) == entity, "statue entity should be kept");
		assertCount(helper, stack, 1);
		helper.succeed();
	}

	@GameTest
	public void flakeIsListedAfterCopperIngotInTheIngredientsTab(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());

		List<Item> items = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INGREDIENTS).getDisplayItems().stream().map(ItemStack::getItem).toList();

		helper.assertTrue(items.contains(VerdigrisFeature.VERDIGRIS), "ingredients tab should list verdigris");
		helper.assertTrue(items.indexOf(VerdigrisFeature.VERDIGRIS) > items.indexOf(Items.COPPER_INGOT), "verdigris should follow the copper ingot");
		helper.succeed();
	}

	private static ResourceKey<Recipe<?>> recipeKey(String path) {
		return ResourceKey.create(Registries.RECIPE, PatinaChime.id(path));
	}

	private static RecipeHolder<CraftingRecipe> craftingRecipe(GameTestHelper helper, CraftingInput input, String path) {
		ServerLevel level = helper.getLevel();
		RecipeHolder<CraftingRecipe> holder = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
			.orElseThrow(() -> helper.assertionException("no crafting recipe matched for " + path));
		helper.assertValueEqual(holder.id(), recipeKey(path), "recipe id");
		helper.assertTrue(level.recipeAccess().byKey(recipeKey(path)).isPresent(), "recipe manager should know " + path);
		return holder;
	}

	private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count) {
		helper.assertTrue(stack.is(item), "expected " + item + " but got " + stack);
		helper.assertValueEqual(stack.getCount(), count, "count of " + item);
	}

	@GameTest
	public void copperAndWaterBucketMakeTwoFlakesAndReturnTheBucket(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.WATER_BUCKET)));
		RecipeHolder<CraftingRecipe> holder = craftingRecipe(helper, input, "verdigris");

		assertStack(helper, holder.value().assemble(input), VerdigrisFeature.VERDIGRIS, 2);
		NonNullList<ItemStack> remainder = holder.value().getRemainingItems(input);
		helper.assertTrue(remainder.get(0).isEmpty(), "the copper ingot should be used up");
		assertStack(helper, remainder.get(1), Items.BUCKET, 1);
		helper.succeed();
	}

	@GameTest
	public void oneFlakeMakesOneCyanDye(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(1, 1, List.of(flakes(1)));
		RecipeHolder<CraftingRecipe> holder = craftingRecipe(helper, input, "cyan_dye_from_verdigris");

		assertStack(helper, holder.value().assemble(input), Items.DYE.pick(DyeColor.CYAN), 1);
		helper.assertTrue(holder.value().getRemainingItems(input).get(0).isEmpty(), "the flake should be used up");
		helper.succeed();
	}
}
