package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.hopper.CopperHopperBlockEntity;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrossFeatureGameTests {
	private static final String QUIET = "patinachime-test:hopper_no_random_ticks";
	private static final BlockPos HOPPER = new BlockPos(3, 2, 3);

	private static Block unwaxed(WeatherState state) {
		return CopperHopperFeature.BLOCKS.weathering().pick(state);
	}

	private static Block waxed(WeatherState state) {
		return CopperHopperFeature.BLOCKS.waxed().pick(state);
	}

	private static CopperHopperBlockEntity placeLoaded(GameTestHelper helper, Block block) {
		helper.setBlock(HOPPER, block.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		CopperHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, CopperHopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(Items.DIAMOND, 7));
		hopper.setItem(3, new ItemStack(Items.COBBLESTONE, 64));
		return hopper;
	}

	private static ServerPlayer holding(GameTestHelper helper, ItemStack stack) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static BlockHitResult hit(GameTestHelper helper) {
		BlockPos absolute = helper.absolutePos(HOPPER);
		return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
	}

	private static void rightClick(GameTestHelper helper, ServerPlayer player) {
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper));
	}

	// A flake with nothing to age falls through to opening the menu, and a mock player has no connection for that.
	private static void useFlakeWithNothingToAge(GameTestHelper helper, ServerPlayer player) {
		InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper)));
		helper.assertTrue(result instanceof InteractionResult.Pass, "a flake with nothing to age returned " + result);
		helper.assertTrue(helper.getBlockState(HOPPER).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper)) instanceof InteractionResult.TryEmptyHandInteraction, "the hopper should open its menu instead");
	}

	private static void assertKept(GameTestHelper helper, Block block, CopperHopperBlockEntity original, String step) {
		helper.assertBlockPresent(block, HOPPER);
		CopperHopperBlockEntity current = helper.getBlockEntity(HOPPER, CopperHopperBlockEntity.class);
		helper.assertTrue(current == original, step + " replaced the block entity");
		helper.assertTrue(ItemStack.matches(current.getItem(0), new ItemStack(Items.DIAMOND, 7)), step + " changed slot 0 to " + current.getItem(0));
		helper.assertTrue(ItemStack.matches(current.getItem(3), new ItemStack(Items.COBBLESTONE, 64)), step + " changed slot 3 to " + current.getItem(3));
		helper.assertValueEqual(helper.getBlockState(HOPPER).getValue(HopperBlock.FACING), Direction.NORTH, step + " facing");
	}

	private static int droppedFlakes(GameTestHelper helper) {
		List<ItemEntity> items = helper.getEntities(EntityTypes.ITEM);
		int flakes = 0;
		for (ItemEntity item : items) {
			helper.assertTrue(item.getItem().is(VerdigrisFeature.VERDIGRIS), "unexpected drop " + item.getItem());
			flakes += item.getItem().getCount();
		}

		return flakes;
	}

	@GameTest(environment = QUIET)
	public void flakeAgesAHopperThroughEveryStageAndKeepsItsContents(GameTestHelper helper) {
		CopperHopperBlockEntity original = placeLoaded(helper, unwaxed(WeatherState.UNAFFECTED));
		ItemStack flakes = new ItemStack(VerdigrisFeature.VERDIGRIS, 5);
		ServerPlayer player = holding(helper, flakes);

		int left = 5;
		for (WeatherState next : List.of(WeatherState.EXPOSED, WeatherState.WEATHERED, WeatherState.OXIDIZED)) {
			rightClick(helper, player);
			assertKept(helper, unwaxed(next), original, "aging to " + next);
			helper.assertValueEqual(flakes.getCount(), --left, "flakes after aging to " + next);
		}

		useFlakeWithNothingToAge(helper, player);
		assertKept(helper, unwaxed(WeatherState.OXIDIZED), original, "using a flake on an oxidized hopper");
		helper.assertValueEqual(flakes.getCount(), left, "flakes after using one on an oxidized hopper");
		helper.assertValueEqual(droppedFlakes(helper), 0, "dropped flakes");
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void flakeDoesNothingOnAWaxedHopper(GameTestHelper helper) {
		CopperHopperBlockEntity original = placeLoaded(helper, waxed(WeatherState.UNAFFECTED));
		ItemStack flakes = new ItemStack(VerdigrisFeature.VERDIGRIS, 3);

		useFlakeWithNothingToAge(helper, holding(helper, flakes));

		assertKept(helper, waxed(WeatherState.UNAFFECTED), original, "using a flake on a waxed hopper");
		helper.assertValueEqual(flakes.getCount(), 3, "flakes after using one on a waxed hopper");
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void scrapingAHopperDropsOneFlakePerStageAndKeepsItsContents(GameTestHelper helper) {
		CopperHopperBlockEntity original = placeLoaded(helper, unwaxed(WeatherState.OXIDIZED));
		ServerPlayer player = holding(helper, new ItemStack(Items.IRON_AXE));

		int expected = 0;
		for (WeatherState previous : List.of(WeatherState.WEATHERED, WeatherState.EXPOSED, WeatherState.UNAFFECTED)) {
			rightClick(helper, player);
			assertKept(helper, unwaxed(previous), original, "scraping back to " + previous);
			helper.assertValueEqual(droppedFlakes(helper), ++expected, "dropped flakes after scraping back to " + previous);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void removingWaxFromAHopperDropsNoFlake(GameTestHelper helper) {
		CopperHopperBlockEntity original = placeLoaded(helper, waxed(WeatherState.OXIDIZED));

		rightClick(helper, holding(helper, new ItemStack(Items.IRON_AXE)));

		assertKept(helper, unwaxed(WeatherState.OXIDIZED), original, "removing wax");
		helper.assertValueEqual(droppedFlakes(helper), 0, "dropped flakes after removing wax");
		helper.succeed();
	}
}
