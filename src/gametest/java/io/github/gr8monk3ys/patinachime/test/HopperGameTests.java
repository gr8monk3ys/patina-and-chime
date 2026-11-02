package io.github.gr8monk3ys.patinachime.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.gr8monk3ys.patinachime.PatinaChime;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperBlock;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperBlockEntity;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperStage;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class HopperGameTests {
	// Random ticks would otherwise age an unwaxed hopper mid-test now and then.
	private static final String QUIET = "patinachime-test:hopper_no_random_ticks";
	private static final String FAST_AGING = "patinachime-test:hopper_fast_random_ticks";
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos MIDDLE = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);
	private static final BlockPos CENTER = new BlockPos(3, 3, 3);
	private static final Item CARGO = Items.COBBLESTONE;

	@GameTest(environment = QUIET, maxTicks = 300)
	public void vanillaHopperBaselineMeasuresEight(GameTestHelper helper) {
		assertRate(helper, Blocks.HOPPER, HopperBlockEntity.MOVE_ITEM_SPEED);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void freshMovesAnItemEveryTwelveTicks(GameTestHelper helper) {
		assertRate(helper, unwaxed(WeatherState.UNAFFECTED), 12);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void exposedMovesAnItemEverySixteenTicks(GameTestHelper helper) {
		assertRate(helper, unwaxed(WeatherState.EXPOSED), 16);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void weatheredMovesAnItemEveryTwentyFourTicks(GameTestHelper helper) {
		assertRate(helper, unwaxed(WeatherState.WEATHERED), 24);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void oxidizedMovesAnItemEveryThirtyTwoTicks(GameTestHelper helper) {
		assertRate(helper, unwaxed(WeatherState.OXIDIZED), 32);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void waxedFreshKeepsItsRate(GameTestHelper helper) {
		assertRate(helper, waxed(WeatherState.UNAFFECTED), 12);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void waxedExposedKeepsItsRate(GameTestHelper helper) {
		assertRate(helper, waxed(WeatherState.EXPOSED), 16);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void waxedWeatheredKeepsItsRate(GameTestHelper helper) {
		assertRate(helper, waxed(WeatherState.WEATHERED), 24);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void waxedOxidizedKeepsItsRate(GameTestHelper helper) {
		assertRate(helper, waxed(WeatherState.OXIDIZED), 32);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void pullingThroughFreshKeepsItsRate(GameTestHelper helper) {
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(BOTTOM, Blocks.CHEST);
		container(helper, TOP).setItem(0, new ItemStack(CARGO, 32));
		assertArrivalIntervals(helper, BOTTOM, 12);
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void chainOfCopperHoppersKeepsItsRate(GameTestHelper helper) {
		BlockPos sink = new BlockPos(1, 0, 1);
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(BOTTOM, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(sink, Blocks.CHEST);
		container(helper, TOP).setItem(0, new ItemStack(CARGO, 32));
		assertArrivalIntervals(helper, sink, 12);
	}

	// Exposed so the first hop can only be a push, and side by side because a hopper below would pull straight away, as vanilla's does.
	@GameTest(environment = QUIET, maxTicks = 200)
	public void anItemPushedIntoAnIdleHopperWaitsAFullCycle(GameTestHelper helper) {
		BlockPos first = new BlockPos(1, 2, 1);
		BlockPos second = first.east();
		BlockPos sink = second.below();
		helper.setBlock(first.above(), Blocks.HOPPER);
		helper.setBlock(first, unwaxed(WeatherState.EXPOSED).defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(second, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(sink, Blocks.CHEST);
		long[] entered = {-1, -1, -1};
		helper.startSequence().thenExecuteAfter(20, () -> container(helper, first.above()).setItem(0, new ItemStack(CARGO))).thenWaitUntil(() -> {
			if (entered[0] < 0 && count(helper, first) == 1) {
				entered[0] = helper.getTick();
			}

			if (entered[1] < 0 && count(helper, second) == 1) {
				entered[1] = helper.getTick();
			}

			if (entered[2] < 0 && count(helper, sink) == 1) {
				entered[2] = helper.getTick();
			}

			helper.assertTrue(entered[2] >= 0, "item has not reached the chest yet");
			helper.assertTrue(entered[1] - entered[0] >= 15, "hop out of exposed took " + (entered[1] - entered[0]) + " ticks");
			helper.assertTrue(entered[2] - entered[1] >= 11, "hop out of fresh took " + (entered[2] - entered[1]) + " ticks");
		}).thenSucceed();
	}

	@GameTest(environment = QUIET)
	public void noStageIsFasterThanVanilla(GameTestHelper helper) {
		for (CopperHopperStage stage : CopperHopperStage.values()) {
			helper.assertTrue(stage.ticksPerItem() >= HopperBlockEntity.MOVE_ITEM_SPEED, stage + " is faster than vanilla");
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void rateFollowsTheNewStageAfterAStageChange(GameTestHelper helper) {
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		container(helper, MIDDLE).setItem(0, new ItemStack(CARGO, 32));

		List<Long> arrivals = new ArrayList<>();
		int[] seen = {0};
		boolean[] changed = {false};
		helper.succeedWhen(() -> {
			int count = count(helper, BOTTOM);
			if (count > seen[0]) {
				seen[0] = count;
				arrivals.add(helper.getTick());
			}

			if (!changed[0] && arrivals.size() == 2) {
				changed[0] = true;
				arrivals.clear();
				helper.setBlock(MIDDLE, unwaxed(WeatherState.OXIDIZED).withPropertiesOf(helper.getBlockState(MIDDLE)));
			}

			helper.assertTrue(changed[0] && arrivals.size() >= 4, "only saw arrivals at " + arrivals);
			for (int i = 1; i < arrivals.size(); i++) {
				helper.assertValueEqual(arrivals.get(i) - arrivals.get(i - 1), 32L, "ticks per item after oxidizing");
			}
		});
	}

	@GameTest(environment = QUIET, maxTicks = 120)
	public void exposedDoesNotPullButPicksUpDroppedItems(GameTestHelper helper) {
		List<Block> blocks = List.of(unwaxed(WeatherState.EXPOSED), waxed(WeatherState.EXPOSED));
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos underChest = new BlockPos(1, 1, 1 + i * 3);
			helper.setBlock(underChest, blocks.get(i));
			helper.setBlock(underChest.above(), Blocks.CHEST);
			container(helper, underChest.above()).setItem(0, new ItemStack(CARGO, 16));

			BlockPos underItem = new BlockPos(5, 1, 1 + i * 3);
			helper.setBlock(underItem, blocks.get(i));
			helper.spawnItem(Items.IRON_INGOT, underItem.above());
		}

		helper.startSequence().thenExecuteAfter(60, () -> {
			for (int i = 0; i < blocks.size(); i++) {
				BlockPos underChest = new BlockPos(1, 1, 1 + i * 3);
				helper.assertValueEqual(count(helper, underChest.above()), 16, "items left in the chest above");
				helper.assertTrue(container(helper, underChest).isEmpty(), "exposed pulled from the chest above");
				helper.assertValueEqual(container(helper, new BlockPos(5, 1, 1 + i * 3)).countItem(Items.IRON_INGOT), 1, "dropped items picked up");
			}

			helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "a dropped item was not picked up");
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 120)
	public void oxidizedIgnoresDroppedItemsButPulls(GameTestHelper helper) {
		List<Block> blocks = List.of(unwaxed(WeatherState.OXIDIZED), waxed(WeatherState.OXIDIZED));
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos underChest = new BlockPos(1, 1, 1 + i * 3);
			helper.setBlock(underChest, blocks.get(i));
			helper.setBlock(underChest.above(), Blocks.CHEST);
			container(helper, underChest.above()).setItem(0, new ItemStack(CARGO, 16));

			BlockPos underItem = new BlockPos(5, 1, 1 + i * 3);
			helper.setBlock(underItem, blocks.get(i));
			helper.spawnItem(Items.IRON_INGOT, underItem.above());
		}

		helper.startSequence().thenExecuteAfter(70, () -> {
			for (int i = 0; i < blocks.size(); i++) {
				BlockPos underChest = new BlockPos(1, 1, 1 + i * 3);
				int pulled = count(helper, underChest);
				helper.assertTrue(pulled >= 1, "oxidized did not pull from the chest above");
				helper.assertValueEqual(pulled + count(helper, underChest.above()), 16, "items across the chest and hopper");
				helper.assertTrue(container(helper, new BlockPos(5, 1, 1 + i * 3)).isEmpty(), "oxidized picked up a dropped item");
			}

			helper.assertValueEqual(helper.getEntities(EntityTypes.ITEM).size(), 2, "dropped items still lying there");
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void weatheredRunsOnlyWhilePowered(GameTestHelper helper) {
		List<Block> blocks = List.of(unwaxed(WeatherState.WEATHERED), waxed(WeatherState.WEATHERED));
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos between = new BlockPos(1, 2, 1 + i * 3);
			helper.setBlock(between.above(), Blocks.CHEST);
			helper.setBlock(between, blocks.get(i));
			helper.setBlock(between.below(), Blocks.CHEST);
			container(helper, between.above()).setItem(0, new ItemStack(CARGO, 16));

			BlockPos collector = new BlockPos(5, 2, 1 + i * 3);
			helper.setBlock(collector, blocks.get(i));
			helper.spawnItem(Items.IRON_INGOT, collector.above());
		}

		helper.startSequence().thenExecuteAfter(60, () -> {
			for (int i = 0; i < blocks.size(); i++) {
				BlockPos between = new BlockPos(1, 2, 1 + i * 3);
				BlockPos collector = new BlockPos(5, 2, 1 + i * 3);
				helper.assertValueEqual(count(helper, between.above()), 16, "items left above an unpowered weathered hopper");
				helper.assertTrue(container(helper, between).isEmpty(), "unpowered weathered pulled");
				helper.assertTrue(container(helper, collector).isEmpty(), "unpowered weathered picked up an item");
				helper.setBlock(between.south(), Blocks.REDSTONE_BLOCK);
				helper.setBlock(collector.south(), Blocks.REDSTONE_BLOCK);
			}
		}).thenWaitUntil(() -> {
			for (int i = 0; i < blocks.size(); i++) {
				BlockPos between = new BlockPos(1, 2, 1 + i * 3);
				helper.assertTrue(count(helper, between.below()) >= 2, "powered weathered is not moving items");
				helper.assertValueEqual(container(helper, new BlockPos(5, 2, 1 + i * 3)).countItem(Items.IRON_INGOT), 1, "items picked up while powered");
			}
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void otherStagesLockWhilePowered(GameTestHelper helper) {
		List<Block> blocks = List.of(
			unwaxed(WeatherState.UNAFFECTED),
			unwaxed(WeatherState.EXPOSED),
			unwaxed(WeatherState.OXIDIZED),
			waxed(WeatherState.UNAFFECTED),
			waxed(WeatherState.EXPOSED),
			waxed(WeatherState.OXIDIZED)
		);
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos pos = column(i).above();
			helper.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
			helper.setBlock(pos.above(), Blocks.CHEST);
			helper.setBlock(pos, blocks.get(i));
			helper.setBlock(pos.below(), Blocks.CHEST);
			container(helper, pos.above()).setItem(0, new ItemStack(CARGO, 16));
			container(helper, pos).setItem(0, new ItemStack(CARGO, 2));
		}

		helper.startSequence().thenExecuteAfter(60, () -> {
			for (int i = 0; i < blocks.size(); i++) {
				BlockPos pos = column(i).above();
				helper.assertFalse(helper.getBlockState(pos).getValue(HopperBlock.ENABLED), "powered hopper is still enabled");
				helper.assertValueEqual(count(helper, pos.above()), 16, "items above a powered hopper");
				helper.assertValueEqual(count(helper, pos), 2, "items in a powered hopper");
				helper.assertValueEqual(count(helper, pos.below()), 0, "items below a powered hopper");
				helper.setBlock(pos.south(), Blocks.AIR);
			}
		}).thenWaitUntil(() -> {
			for (int i = 0; i < blocks.size(); i++) {
				helper.assertTrue(count(helper, column(i)) >= 1, "unlocked hopper is not pushing");
			}
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void everyStagePushesIntoAChestAndAcceptsAVanillaHopperPush(GameTestHelper helper) {
		List<Block> blocks = allBlocks();
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos base = column(i);
			helper.setBlock(base, Blocks.CHEST);
			helper.setBlock(base.above(), blocks.get(i));
			powerIfNeeded(helper, blocks.get(i), base.above());
			helper.setBlock(base.above(2), Blocks.HOPPER);
			container(helper, base.above(2)).setItem(0, new ItemStack(CARGO, 3));
		}

		helper.succeedWhen(() -> {
			for (int i = 0; i < blocks.size(); i++) {
				helper.assertValueEqual(count(helper, column(i)), 3, "items that reached the chest under " + blocks.get(i));
			}
		});
	}

	@GameTest(environment = QUIET, maxTicks = 120)
	public void lockedHoppersStillAcceptPushes(GameTestHelper helper) {
		List<Block> blocks = allBlocks();
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos pos = column(i).above();
			helper.setBlock(pos, blocks.get(i));
			if (((CopperHopperBlock) blocks.get(i)).stage().runsWhen(false)) {
				helper.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
			}

			helper.setBlock(pos.above(), Blocks.HOPPER);
			container(helper, pos.above()).setItem(0, new ItemStack(CARGO, 2));
		}

		helper.succeedWhen(() -> {
			for (int i = 0; i < blocks.size(); i++) {
				helper.assertValueEqual(count(helper, column(i).above()), 2, "items pushed into a locked " + blocks.get(i));
			}
		});
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void vanillaHopperResumesOnceAFullCopperHopperDrains(GameTestHelper helper) {
		helper.setBlock(MIDDLE.south(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(TOP, Blocks.HOPPER);
		for (int slot = 0; slot < HopperBlockEntity.HOPPER_CONTAINER_SIZE; slot++) {
			container(helper, MIDDLE).setItem(slot, new ItemStack(CARGO, 64));
		}

		container(helper, TOP).setItem(0, new ItemStack(CARGO, 1));
		helper.startSequence().thenExecuteAfter(40, () -> {
			helper.assertValueEqual(count(helper, TOP), 1, "items a full hopper should have refused");
			helper.setBlock(MIDDLE.south(), Blocks.AIR);
		}).thenWaitUntil(() -> {
			helper.assertValueEqual(count(helper, TOP), 0, "items stuck in the vanilla hopper after the copper hopper drained");
			helper.assertValueEqual(count(helper, MIDDLE) + count(helper, BOTTOM), 321, "items across the copper hopper and chest");
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 200)
	public void vanillaHopperBelowCanDrainACopperHopper(GameTestHelper helper) {
		helper.setBlock(MIDDLE.south(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(BOTTOM, Blocks.HOPPER);
		container(helper, MIDDLE).setItem(2, new ItemStack(CARGO, 3));
		helper.succeedWhen(() -> helper.assertValueEqual(count(helper, BOTTOM), 3, "items a vanilla hopper pulled out of a locked copper hopper"));
	}

	@GameTest(environment = QUIET, maxTicks = 200)
	public void copperHopperPushesIntoAndPullsFromVanillaHoppers(GameTestHelper helper) {
		helper.setBlock(TOP.south(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(TOP, Blocks.HOPPER);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(BOTTOM, Blocks.HOPPER);
		container(helper, TOP).setItem(0, new ItemStack(CARGO, 3));
		helper.succeedWhen(() -> helper.assertValueEqual(count(helper, BOTTOM), 3, "items moved from one vanilla hopper to another through copper"));
	}

	@GameTest(environment = QUIET, maxTicks = 200)
	public void sideFacingHopperPushesSideways(GameTestHelper helper) {
		BlockPos side = CENTER.east();
		helper.setBlock(side, Blocks.CHEST);
		helper.setBlock(CENTER, unwaxed(WeatherState.UNAFFECTED).defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		container(helper, CENTER).setItem(0, new ItemStack(CARGO, 1));
		helper.succeedWhen(() -> helper.assertValueEqual(count(helper, side), 1, "items pushed out of the side"));
	}

	@GameTest(environment = QUIET, maxTicks = 200)
	public void comparatorReadsEveryStage(GameTestHelper helper) {
		List<Block> blocks = new ArrayList<>(List.of(
			unwaxed(WeatherState.UNAFFECTED),
			unwaxed(WeatherState.EXPOSED),
			unwaxed(WeatherState.WEATHERED),
			unwaxed(WeatherState.OXIDIZED),
			Blocks.HOPPER
		));
		List<BlockPos> hoppers = new ArrayList<>();
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos pos = new BlockPos(1 + 3 * (i % 2), 2, 1 + 3 * (i / 2));
			hoppers.add(pos);
			helper.setBlock(pos, blocks.get(i));
			helper.setBlock(pos.east().below(), Blocks.STONE);
			helper.setBlock(pos.east(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
			BaseContainerBlockEntity hopper = container(helper, pos);
			hopper.setItem(0, new ItemStack(CARGO, 32));
			hopper.setChanged();
		}

		helper.startSequence().thenExecuteAfter(5, () -> {
			for (BlockPos pos : hoppers) {
				helper.assertValueEqual(comparator(helper, pos), 2, "comparator reading half a stack");
				BaseContainerBlockEntity hopper = container(helper, pos);
				hopper.removeItem(0, 31);
				hopper.setChanged();
			}
		}).thenExecuteAfter(5, () -> {
			for (int i = 0; i < hoppers.size(); i++) {
				BlockPos pos = hoppers.get(i);
				helper.assertValueEqual(comparator(helper, pos), 1, "comparator reading one item");
				helper.setBlock(pos.below(), Blocks.CHEST);
				powerIfNeeded(helper, blocks.get(i), pos);
			}
		}).thenWaitUntil(() -> {
			for (BlockPos pos : hoppers) {
				helper.assertValueEqual(comparator(helper, pos), 0, "comparator after the hopper pushed its last item");
			}
		}).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 100)
	public void inventorySurvivesEveryStageChange(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(CENTER);
		helper.setBlock(CENTER, unwaxed(WeatherState.UNAFFECTED).defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
		CopperHopperBlockEntity original = helper.getBlockEntity(CENTER, CopperHopperBlockEntity.class);
		List<ItemStack> contents = seedDistinctStacks(original);

		for (WeatherState next : List.of(WeatherState.EXPOSED, WeatherState.WEATHERED, WeatherState.OXIDIZED)) {
			ageOneStage(helper, CENTER);
			assertKept(helper, CENTER, unwaxed(next), original, contents, "natural aging");
		}

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		for (WeatherState previous : List.of(WeatherState.WEATHERED, WeatherState.EXPOSED, WeatherState.UNAFFECTED)) {
			helper.useBlock(CENTER, player);
			assertKept(helper, CENTER, unwaxed(previous), original, contents, "axe scraping");
		}

		Player waxer = helper.makeMockPlayer(GameType.SURVIVAL);
		waxer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 8));
		for (WeatherState state : WeatherState.values()) {
			helper.useBlock(CENTER, waxer);
			assertKept(helper, CENTER, waxed(state), original, contents, "honeycomb waxing");
			helper.useBlock(CENTER, player);
			assertKept(helper, CENTER, unwaxed(state), original, contents, "wax removal");
			BlockState current = helper.getBlockState(CENTER);
			WeatheringCopper.getNext(current.getBlock()).ifPresent(next -> level.setBlock(absolute, next.withPropertiesOf(current), 11));
		}

		helper.assertValueEqual(waxer.getItemInHand(InteractionHand.MAIN_HAND).getCount(), 4, "honeycomb left after waxing four times");
		helper.assertValueEqual(helper.getBlockState(CENTER).getValue(HopperBlock.FACING), Direction.NORTH, "facing after every stage change");
		helper.startSequence().thenExecuteAfter(20, () -> assertKept(helper, CENTER, unwaxed(WeatherState.OXIDIZED), original, contents, "waiting")).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void itemsAreConservedWhileStageChangesUnderLoad(GameTestHelper helper) {
		List<Block> blocks = allBlocks();
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(MIDDLE, blocks.get(0));
		helper.setBlock(BOTTOM, Blocks.CHEST);
		container(helper, TOP).setItem(0, new ItemStack(CARGO, 64));
		int[] step = {0};
		helper.startSequence().thenExecuteFor(240, () -> {
			if (helper.getTick() % 9 == 0) {
				step[0]++;
				helper.setBlock(MIDDLE, blocks.get(step[0] % blocks.size()).withPropertiesOf(helper.getBlockState(MIDDLE)));
			}

			helper.assertValueEqual(count(helper, TOP) + count(helper, MIDDLE) + count(helper, BOTTOM), 64, "items across the column");
			helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "a stage change dropped items");
		}).thenExecute(() -> helper.assertTrue(count(helper, BOTTOM) > 0, "nothing moved during the run")).thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 300)
	public void vanillaHoppersKeepWorkingAcrossAStageChange(GameTestHelper helper) {
		BlockPos sink = new BlockPos(1, 0, 1);
		helper.setBlock(sink, Blocks.CHEST);
		helper.setBlock(BOTTOM, Blocks.HOPPER);
		helper.setBlock(MIDDLE, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(TOP, Blocks.HOPPER);
		container(helper, TOP).setItem(0, new ItemStack(CARGO, 12));
		helper.startSequence().thenExecuteAfter(30, () -> {
			helper.assertTrue(count(helper, sink) > 0, "nothing reached the chest before the stage change");
			helper.setBlock(MIDDLE, unwaxed(WeatherState.EXPOSED).withPropertiesOf(helper.getBlockState(MIDDLE)));
		}).thenWaitUntil(() -> helper.assertValueEqual(count(helper, sink), 12, "items that reached the chest")).thenExecute(() ->
			helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "the stage change dropped items")
		).thenSucceed();
	}

	@GameTest(environment = FAST_AGING, maxTicks = 2400)
	public void randomTicksAgeUnwaxedHoppersAndNeverWaxedOnes(GameTestHelper helper) {
		BlockPos aging = new BlockPos(1, 2, 1);
		BlockPos waxedPos = new BlockPos(6, 2, 6);
		helper.setBlock(aging, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(waxedPos, waxed(WeatherState.UNAFFECTED));
		CopperHopperBlockEntity original = helper.getBlockEntity(aging, CopperHopperBlockEntity.class);
		List<ItemStack> contents = seedDistinctStacks(original);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(aging).is(unwaxed(WeatherState.OXIDIZED)), "still " + helper.getBlockState(aging));
			assertKept(helper, aging, unwaxed(WeatherState.OXIDIZED), original, contents, "random ticks");
			helper.assertTrue(helper.getBlockState(waxedPos).is(waxed(WeatherState.UNAFFECTED)), "a waxed hopper aged");
		});
	}

	@GameTest(environment = QUIET)
	public void waxedAndFullyOxidizedHoppersNeverAge(GameTestHelper helper) {
		List<Block> frozen = new ArrayList<>(List.of(unwaxed(WeatherState.OXIDIZED)));
		CopperHopperFeature.BLOCKS.waxed().forEach(frozen::add);
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(CENTER);
		for (Block block : frozen) {
			helper.setBlock(CENTER, block);
			helper.assertFalse(block.defaultBlockState().isRandomlyTicking(), block + " random ticks");
			helper.assertTrue(WeatheringCopper.getNext(block).isEmpty(), block + " has a next stage");
			for (int i = 0; i < 2000; i++) {
				helper.getBlockState(CENTER).randomTick(level, absolute, level.getRandom());
			}

			helper.assertTrue(helper.getBlockState(CENTER).is(block), block + " aged");
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void agingAndWaxingPairsAreRegistered(GameTestHelper helper) {
		List<WeatherState> states = List.of(WeatherState.UNAFFECTED, WeatherState.EXPOSED, WeatherState.WEATHERED, WeatherState.OXIDIZED);
		for (int i = 0; i < states.size(); i++) {
			Block block = unwaxed(states.get(i));
			helper.assertValueEqual(((CopperHopperBlock) block).stage(), CopperHopperStage.of(states.get(i)), "stage");
			helper.assertValueEqual(((CopperHopperBlock) waxed(states.get(i))).stage(), CopperHopperStage.of(states.get(i)), "waxed stage");
			helper.assertValueEqual(HoneycombItem.WAXABLES.get().get(block), waxed(states.get(i)), "waxed form");
			helper.assertValueEqual(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(waxed(states.get(i))), block, "unwaxed form");
			if (i < states.size() - 1) {
				helper.assertTrue(block.defaultBlockState().isRandomlyTicking(), block + " does not random tick");
				helper.assertValueEqual(WeatheringCopper.getNext(block), Optional.of(unwaxed(states.get(i + 1))), "next stage");
			}

			if (i > 0) {
				helper.assertValueEqual(WeatheringCopper.getPrevious(block), Optional.of(unwaxed(states.get(i - 1))), "previous stage");
			}
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void stageChangingItemsSkipTheMenuAndOthersOpenIt(GameTestHelper helper) {
		BlockPos absolute = helper.absolutePos(CENTER);
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		ItemStack honeycomb = new ItemStack(Items.HONEYCOMB);
		ItemStack cobblestone = new ItemStack(CARGO);
		assertUse(helper, unwaxed(WeatherState.EXPOSED), axe, absolute, true);
		assertUse(helper, waxed(WeatherState.UNAFFECTED), axe, absolute, true);
		assertUse(helper, unwaxed(WeatherState.UNAFFECTED), axe, absolute, false);
		assertUse(helper, unwaxed(WeatherState.UNAFFECTED), honeycomb, absolute, true);
		assertUse(helper, waxed(WeatherState.UNAFFECTED), honeycomb, absolute, false);
		assertUse(helper, unwaxed(WeatherState.UNAFFECTED), cobblestone, absolute, false);
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void everyBlockDropsItselfAtItsStagePlusContents(GameTestHelper helper) {
		for (Block block : allBlocks()) {
			helper.setBlock(CENTER, block);
			container(helper, CENTER).setItem(0, new ItemStack(CARGO, 7));
			container(helper, CENTER).setItem(3, new ItemStack(Items.IRON_INGOT, 3));
			helper.getLevel().destroyBlock(helper.absolutePos(CENTER), true);
			helper.assertValueEqual(collectDrops(helper), Map.of(block.asItem(), 1, CARGO, 7, Items.IRON_INGOT, 3), "drops of " + block);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void replacingWithAnotherBlockDropsContents(GameTestHelper helper) {
		for (Block replacement : List.of(Blocks.STONE, Blocks.HOPPER)) {
			helper.setBlock(CENTER, unwaxed(WeatherState.UNAFFECTED));
			container(helper, CENTER).setItem(0, new ItemStack(CARGO, 5));
			helper.setBlock(CENTER, replacement);
			helper.assertValueEqual(collectDrops(helper), Map.of(CARGO, 5), "contents after replacing with " + replacement);
			helper.setBlock(CENTER, Blocks.AIR);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void recipesExistAndCraftTheRightThing(GameTestHelper helper) {
		ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
		List<ItemStack> grid = List.of(ingot, ItemStack.EMPTY, ingot, ingot, new ItemStack(Items.CHEST), ingot, ItemStack.EMPTY, ingot, ItemStack.EMPTY);
		assertCrafts(helper, "copper_hopper", CraftingInput.of(3, 3, grid), unwaxed(WeatherState.UNAFFECTED));

		List<ItemStack> iron = List.of(
			new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT),
			new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CHEST), new ItemStack(Items.IRON_INGOT),
			ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY
		);
		helper.assertFalse(craftingRecipe(helper, "copper_hopper").matches(CraftingInput.of(3, 3, iron), helper.getLevel()), "iron ingots made a copper hopper");

		for (WeatherState state : WeatherState.values()) {
			Block block = unwaxed(state);
			String id = BuiltInRegistries.BLOCK.getKey(waxed(state)).getPath() + "_from_honeycomb";
			CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(block), new ItemStack(Items.HONEYCOMB)));
			assertCrafts(helper, id, input, waxed(state));
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(PatinaChime.id("recipes/redstone/" + id)) != null, "no unlock advancement for " + id);
		}

		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(PatinaChime.id("recipes/redstone/copper_hopper")) != null, "no unlock advancement for copper_hopper");
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void blockPropertiesMatchTheSpec(GameTestHelper helper) {
		BlockPos absolute = helper.absolutePos(CENTER);
		BlockState vanilla = Blocks.HOPPER.defaultBlockState();
		for (Block block : allBlocks()) {
			BlockState state = block.defaultBlockState();
			helper.assertTrue(state.getSoundType() == SoundType.COPPER, block + " does not sound like copper");
			helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), block + " is not pickaxe mineable");
			helper.assertTrue(state.requiresCorrectToolForDrops(), block + " drops without the right tool");
			helper.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state), "a pickaxe cannot harvest " + block);
			helper.assertFalse(new ItemStack(Items.STICK).isCorrectToolForDrops(state), "a stick can harvest " + block);
			helper.assertValueEqual(block.defaultDestroyTime(), Blocks.HOPPER.defaultDestroyTime(), "destroy time");
			helper.assertValueEqual(block.getExplosionResistance(), Blocks.HOPPER.getExplosionResistance(), "explosion resistance");
			helper.assertFalse(
				Shapes.joinIsNotEmpty(state.getShape(helper.getLevel(), absolute), vanilla.getShape(helper.getLevel(), absolute), BooleanOp.NOT_SAME),
				block + " has a different shape from the vanilla hopper"
			);
			helper.assertTrue(CopperHopperFeature.BLOCK_ENTITY.isValid(state), "the block entity type rejects " + block);
			helper.assertTrue(BlockTypes.CODEC.codec().encodeStart(JsonOps.INSTANCE, block).isSuccess(), block + " has no registered block type");
			helper.setBlock(CENTER, block);
			helper.assertValueEqual(container(helper, CENTER).getContainerSize(), 5, "slots");
			helper.setBlock(CENTER, Blocks.AIR);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void redstoneDustSitsOnEveryStage(GameTestHelper helper) {
		BlockPos above = helper.absolutePos(CENTER.above());
		for (Block block : allBlocks()) {
			helper.setBlock(CENTER, block);
			helper.assertTrue(Blocks.REDSTONE_WIRE.defaultBlockState().canSurvive(helper.getLevel(), above), "dust cannot sit on " + block);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void menuUsesTheCopperHopperTitleAndFiveSlots(GameTestHelper helper) {
		helper.setBlock(CENTER, unwaxed(WeatherState.EXPOSED));
		CopperHopperBlockEntity hopper = helper.getBlockEntity(CENTER, CopperHopperBlockEntity.class);
		hopper.setItem(4, new ItemStack(CARGO, 2));
		helper.assertTrue(translationKey(hopper.getDisplayName()).equals("container.patinachime.copper_hopper"), "menu title is " + hopper.getDisplayName());

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		AbstractContainerMenu menu = hopper.createMenu(1, player.getInventory(), player);
		helper.assertTrue(menu instanceof HopperMenu, "menu is " + menu);
		helper.assertTrue(ItemStack.matches(menu.getSlot(4).getItem(), new ItemStack(CARGO, 2)), "menu does not show the hopper's slots");
		helper.assertTrue(helper.getBlockState(CENTER).useWithoutItem(helper.getLevel(), player, hitOn(helper.absolutePos(CENTER))).consumesAction(), "using the hopper did nothing");
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void tooltipsDescribeTheStageAndWarnAboutAging(GameTestHelper helper) {
		Map<WeatherState, String> keys = Map.of(
			WeatherState.UNAFFECTED, "tooltip.patinachime.hopper.fresh",
			WeatherState.EXPOSED, "tooltip.patinachime.hopper.exposed",
			WeatherState.WEATHERED, "tooltip.patinachime.hopper.weathered",
			WeatherState.OXIDIZED, "tooltip.patinachime.hopper.oxidized"
		);
		for (WeatherState state : WeatherState.values()) {
			List<String> unwaxedLines = tooltipKeys(unwaxed(state));
			helper.assertTrue(unwaxedLines.contains(keys.get(state)), state + " tooltip: " + unwaxedLines);
			helper.assertTrue(unwaxedLines.contains("tooltip.patinachime.hopper.ages"), "no aging warning: " + unwaxedLines);
			helper.assertFalse(unwaxedLines.contains("tooltip.patinachime.hopper.waxed"), "unwaxed claims to be waxed: " + unwaxedLines);

			List<String> waxedLines = tooltipKeys(waxed(state));
			helper.assertTrue(waxedLines.contains(keys.get(state)), "waxed " + state + " tooltip: " + waxedLines);
			helper.assertTrue(waxedLines.contains("tooltip.patinachime.hopper.waxed"), "no waxed line: " + waxedLines);
			helper.assertFalse(waxedLines.contains("tooltip.patinachime.hopper.ages"), "waxed warns about aging: " + waxedLines);
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void inventoryAndCooldownSurviveSavingAndLoading(GameTestHelper helper) {
		helper.setBlock(CENTER, unwaxed(WeatherState.WEATHERED));
		CopperHopperBlockEntity hopper = helper.getBlockEntity(CENTER, CopperHopperBlockEntity.class);
		List<ItemStack> contents = seedDistinctStacks(hopper);
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		CompoundTag saved = hopper.saveWithFullMetadata(registries);
		helper.assertTrue(saved.contains("TransferCooldown"), "cooldown was not saved");
		BlockEntity loaded = BlockEntity.loadStatic(hopper.getBlockPos(), hopper.getBlockState(), saved, registries);
		helper.assertTrue(loaded instanceof CopperHopperBlockEntity, "loaded " + loaded);
		for (int slot = 0; slot < contents.size(); slot++) {
			helper.assertTrue(ItemStack.matches(((Container) loaded).getItem(slot), contents.get(slot)), "slot " + slot + " changed on load");
		}

		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void creativeTabListsEveryHopperAfterTheVanillaOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.REDSTONE_BLOCKS);
		List<Item> items = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
		int vanilla = items.indexOf(Items.HOPPER);
		helper.assertTrue(vanilla >= 0, "the vanilla hopper is missing from the tab");
		List<Item> expected = CopperHopperFeature.ITEMS.asList();
		helper.assertValueEqual(items.subList(vanilla + 1, vanilla + 1 + expected.size()), expected, "items after the vanilla hopper");
		helper.succeed();
	}

	@GameTest(environment = QUIET)
	public void resourceFilesAreNamedCorrectly(GameTestHelper helper) {
		JsonObject lang = readJson(helper, "assets/patinachime/lang/en_us.json");
		for (Block block : allBlocks()) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			helper.assertTrue(lang.has("block.patinachime." + name), "no name for " + name);
			helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(block.asItem()).getPath(), name, "item id");

			JsonObject variants = readJson(helper, "assets/patinachime/blockstates/" + name + ".json").getAsJsonObject("variants");
			for (Direction facing : HopperBlock.FACING.getPossibleValues()) {
				String key = "facing=" + facing.getSerializedName();
				helper.assertTrue(variants.has(key), name + " has no variant for " + key);
				assertBlockModel(helper, variants.getAsJsonObject(key).get("model").getAsString());
			}

			JsonObject itemDefinition = readJson(helper, "assets/patinachime/items/" + name + ".json");
			String itemModel = itemDefinition.getAsJsonObject("model").get("model").getAsString();
			JsonObject model = readJson(helper, assetPath(itemModel, "models", "json"));
			assertTexture(helper, model.getAsJsonObject("textures").get("layer0").getAsString());

			helper.assertTrue(modFile("data/patinachime/loot_table/blocks/" + name + ".json").isPresent(), "no loot table for " + name);
			helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).get(ResourceKey.create(Registries.LOOT_TABLE, PatinaChime.id("blocks/" + name))).isPresent(), "loot table for " + name + " did not load");
		}

		for (String key : List.of("container.patinachime.copper_hopper", "tooltip.patinachime.hopper.fresh", "tooltip.patinachime.hopper.exposed", "tooltip.patinachime.hopper.weathered", "tooltip.patinachime.hopper.oxidized", "tooltip.patinachime.hopper.ages", "tooltip.patinachime.hopper.waxed")) {
			helper.assertTrue(lang.has(key), "no text for " + key);
		}

		helper.succeed();
	}

	private static void assertRate(GameTestHelper helper, Block block, int expected) {
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.setBlock(MIDDLE, block);
		powerIfNeeded(helper, block, MIDDLE);
		container(helper, MIDDLE).setItem(0, new ItemStack(CARGO, 16));
		assertArrivalIntervals(helper, BOTTOM, expected);
	}

	private static void assertArrivalIntervals(GameTestHelper helper, BlockPos sink, int expected) {
		List<Long> arrivals = new ArrayList<>();
		int[] seen = {0};
		helper.succeedWhen(() -> {
			int count = count(helper, sink);
			if (count > seen[0]) {
				seen[0] = count;
				arrivals.add(helper.getTick());
			}

			helper.assertTrue(arrivals.size() >= 6, "only saw arrivals at " + arrivals);
			for (int i = 1; i < arrivals.size(); i++) {
				long measured = arrivals.get(i) - arrivals.get(i - 1);
				helper.assertTrue(measured >= HopperBlockEntity.MOVE_ITEM_SPEED, "faster than vanilla: " + arrivals);
				helper.assertValueEqual(measured, (long) expected, "ticks per item in " + arrivals);
			}
		});
	}

	private static void powerIfNeeded(GameTestHelper helper, Block block, BlockPos pos) {
		if (block instanceof CopperHopperBlock copper && copper.stage().runsWhen(true)) {
			helper.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
		}
	}

	private static BlockPos column(int index) {
		return new BlockPos(3 * (index % 3), 1, 3 * (index / 3));
	}

	private static Block unwaxed(WeatherState state) {
		return CopperHopperFeature.BLOCKS.weathering().pick(state);
	}

	private static Block waxed(WeatherState state) {
		return CopperHopperFeature.BLOCKS.waxed().pick(state);
	}

	private static List<Block> allBlocks() {
		return CopperHopperFeature.BLOCKS.asList();
	}

	private static BaseContainerBlockEntity container(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
	}

	private static int count(GameTestHelper helper, BlockPos pos) {
		return container(helper, pos).countItem(CARGO);
	}

	private static int comparator(GameTestHelper helper, BlockPos hopper) {
		return helper.getBlockEntity(hopper.east(), ComparatorBlockEntity.class).getOutputSignal();
	}

	private static List<ItemStack> seedDistinctStacks(Container hopper) {
		ItemStack named = new ItemStack(Items.DIAMOND, 2);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("keepsake"));
		List<ItemStack> contents = List.of(new ItemStack(CARGO, 64), new ItemStack(Items.IRON_INGOT, 23), new ItemStack(Items.REDSTONE, 1), named, new ItemStack(Items.ENDER_PEARL, 16));
		for (int slot = 0; slot < contents.size(); slot++) {
			hopper.setItem(slot, contents.get(slot).copy());
		}

		return contents;
	}

	private static void ageOneStage(GameTestHelper helper, BlockPos pos) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(pos);
		Block before = helper.getBlockState(pos).getBlock();
		for (int i = 0; i < 100_000 && helper.getBlockState(pos).is(before); i++) {
			helper.getBlockState(pos).randomTick(level, absolute, level.getRandom());
		}

		helper.assertFalse(helper.getBlockState(pos).is(before), before + " never aged");
	}

	private static void assertKept(GameTestHelper helper, BlockPos pos, Block block, CopperHopperBlockEntity original, List<ItemStack> contents, String step) {
		BlockState state = helper.getBlockState(pos);
		helper.assertTrue(state.is(block), step + " left " + state + ", expected " + block);
		CopperHopperBlockEntity current = helper.getBlockEntity(pos, CopperHopperBlockEntity.class);
		helper.assertTrue(current == original, step + " replaced the block entity");
		for (int slot = 0; slot < contents.size(); slot++) {
			helper.assertTrue(ItemStack.matches(current.getItem(slot), contents.get(slot)), step + " changed slot " + slot + " to " + current.getItem(slot));
		}

		helper.assertTrue(helper.getEntities(EntityTypes.ITEM).stream().allMatch(item -> item.getItem().is(VerdigrisFeature.VERDIGRIS)), step + " dropped something other than verdigris");
	}

	private static void assertUse(GameTestHelper helper, Block block, ItemStack stack, BlockPos absolute, boolean reachesItem) {
		helper.setBlock(CENTER, block);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		InteractionResult result = helper.getBlockState(CENTER).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hitOn(absolute));
		helper.assertValueEqual(result instanceof InteractionResult.Pass, reachesItem, stack + " on " + block + " reaches the item");
		helper.assertValueEqual(result instanceof InteractionResult.TryEmptyHandInteraction, !reachesItem, stack + " on " + block + " opens the menu");
	}

	private static BlockHitResult hitOn(BlockPos absolute) {
		return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, true);
	}

	private static Map<Item, Integer> collectDrops(GameTestHelper helper) {
		Map<Item, Integer> drops = new HashMap<>();
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM)) {
			drops.merge(entity.getItem().getItem(), entity.getItem().getCount(), Integer::sum);
			entity.discard();
		}

		return drops;
	}

	@SuppressWarnings("unchecked")
	private static Recipe<CraftingInput> craftingRecipe(GameTestHelper helper, String id) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, PatinaChime.id(id));
		Optional<RecipeHolder<?>> holder = helper.getLevel().getServer().getRecipeManager().byKey(key);
		helper.assertTrue(holder.isPresent(), "recipe " + id + " did not load");
		return (Recipe<CraftingInput>) holder.get().value();
	}

	private static void assertCrafts(GameTestHelper helper, String id, CraftingInput input, Block result) {
		Recipe<CraftingInput> recipe = craftingRecipe(helper, id);
		helper.assertTrue(recipe.matches(input, helper.getLevel()), id + " does not match its ingredients");
		helper.assertTrue(recipe.assemble(input).is(result.asItem()), id + " makes " + recipe.assemble(input));
	}

	private static List<String> tooltipKeys(Block block) {
		return new ItemStack(block).getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL).stream().map(HopperGameTests::translationKey).toList();
	}

	private static String translationKey(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : component.getString();
	}

	private static Optional<Path> modFile(String path) {
		return FabricLoader.getInstance().getModContainer(PatinaChime.MOD_ID).orElseThrow().findPath(path);
	}

	private static JsonObject readJson(GameTestHelper helper, String path) {
		Optional<Path> file = modFile(path);
		helper.assertTrue(file.isPresent(), "missing " + path);
		try {
			return JsonParser.parseString(Files.readString(file.get())).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException(path, e);
		}
	}

	private static String assetPath(String id, String folder, String extension) {
		Identifier identifier = Identifier.parse(id);
		return "assets/" + identifier.getNamespace() + "/" + folder + "/" + identifier.getPath() + "." + extension;
	}

	private static void assertBlockModel(GameTestHelper helper, String id) {
		JsonObject model = readJson(helper, assetPath(id, "models", "json"));
		String parent = model.get("parent").getAsString();
		helper.assertTrue(parent.equals("minecraft:block/hopper") || parent.equals("minecraft:block/hopper_side"), id + " has parent " + parent);
		JsonObject textures = model.getAsJsonObject("textures");
		for (String slot : List.of("particle", "top", "side", "inside")) {
			helper.assertTrue(textures.has(slot), id + " has no " + slot + " texture");
		}

		for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
			assertTexture(helper, texture.getValue().getAsString());
		}
	}

	private static void assertTexture(GameTestHelper helper, String id) {
		helper.assertTrue(id.startsWith("patinachime:"), id + " is not one of our textures");
		String path = assetPath(id, "textures", "png");
		Optional<Path> file = modFile(path);
		helper.assertTrue(file.isPresent(), "missing " + path);
		try {
			byte[] header = Arrays.copyOf(Files.readAllBytes(file.get()), 24);
			helper.assertValueEqual(ByteBuffer.wrap(header, 16, 4).getInt(), 16, path + " width");
			helper.assertValueEqual(ByteBuffer.wrap(header, 20, 4).getInt(), 16, path + " height");
		} catch (IOException e) {
			throw new IllegalStateException(path, e);
		}
	}
}
