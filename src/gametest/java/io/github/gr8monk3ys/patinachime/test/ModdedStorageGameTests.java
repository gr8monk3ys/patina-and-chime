package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.hopper.CopperHopperBlock;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * A machine from another mod can offer only a Fabric item storage, with no Container behind it. Fabric API teaches
 * vanilla hoppers to reach those, so copper hoppers have to as well, at their own pace and by their stage's rules.
 */
public class ModdedStorageGameTests {
	private static final String QUIET = "patinachime-test:hopper_no_random_ticks";
	private static final Block MACHINE_BLOCK = Blocks.LODESTONE;
	private static final Item CARGO = Items.COBBLESTONE;
	private static final Map<BlockPos, FakeMachine> MACHINES = new ConcurrentHashMap<>();

	static {
		ItemStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, side) -> MACHINES.get(pos), MACHINE_BLOCK);
	}

	@GameTest(environment = QUIET, maxTicks = 400)
	public void hoppersPushIntoAMachineAtTheirOwnRate(GameTestHelper helper) {
		List<Arrivals> arrivals = new ArrayList<>();
		List<Block> blocks = List.of(Blocks.HOPPER, unwaxed(WeatherState.UNAFFECTED), unwaxed(WeatherState.EXPOSED), unwaxed(WeatherState.WEATHERED), unwaxed(WeatherState.OXIDIZED));
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos hopper = slot(i);
			FakeMachine machine = placeMachine(helper, hopper.below(), ItemStack.EMPTY, 64);
			placeHopper(helper, hopper, blocks.get(i));
			container(helper, hopper).setItem(0, new ItemStack(CARGO, 16));
			arrivals.add(new Arrivals(blocks.get(i), () -> (int) machine.getAmount()));
		}

		assertAllSteady(helper, arrivals);
	}

	@GameTest(environment = QUIET, maxTicks = 400)
	public void stagesThatPullTakeFromAMachineAboveAtTheirOwnRate(GameTestHelper helper) {
		List<Arrivals> arrivals = new ArrayList<>();
		List<Block> blocks = List.of(Blocks.HOPPER, unwaxed(WeatherState.UNAFFECTED), unwaxed(WeatherState.WEATHERED), unwaxed(WeatherState.OXIDIZED));
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos hopper = slot(i);
			BlockPos sink = hopper.below();
			placeMachine(helper, hopper.above(), new ItemStack(CARGO, 16), 64);
			placeHopper(helper, hopper, blocks.get(i));
			helper.setBlock(sink, Blocks.CHEST);
			arrivals.add(new Arrivals(blocks.get(i), () -> container(helper, sink).countItem(CARGO)));
		}

		assertAllSteady(helper, arrivals);
	}

	@GameTest(environment = QUIET, maxTicks = 120)
	public void exposedLeavesAMachineAboveAlone(GameTestHelper helper) {
		BlockPos hopper = slot(0);
		FakeMachine machine = placeMachine(helper, hopper.above(), new ItemStack(CARGO, 16), 64);
		placeHopper(helper, hopper, unwaxed(WeatherState.EXPOSED));
		helper.startSequence().thenExecuteAfter(60, () -> {
			helper.assertValueEqual(machine.getAmount(), 16L, "items left in the machine");
			helper.assertTrue(container(helper, hopper).isEmpty(), "exposed pulled from the machine");
		}).thenSucceed();
	}

	// A hopper with one item empties for a moment while Fabric tests a move. That must not look like a first item arriving.
	@GameTest(environment = QUIET, maxTicks = 120)
	public void aRefusedPushDoesNotPauseTheHopper(GameTestHelper helper) {
		BlockPos hopper = slot(0);
		FakeMachine machine = placeMachine(helper, hopper.below(), ItemStack.EMPTY, 0);
		placeHopper(helper, hopper, unwaxed(WeatherState.OXIDIZED));
		container(helper, hopper).setItem(0, new ItemStack(CARGO));
		helper.startSequence()
			.thenExecuteAfter(50, () -> machine.capacity = 64)
			.thenExecuteAfter(3, () -> helper.assertValueEqual(machine.getAmount(), 1L, "items moved just after the machine made room"))
			.thenSucceed();
	}

	@GameTest(environment = QUIET, maxTicks = 100)
	public void aFullPipeTestingAHopperDoesNotFreezeIt(GameTestHelper helper) {
		BlockPos hopper = slot(0);
		BlockPos sink = hopper.below();
		placeHopper(helper, hopper, unwaxed(WeatherState.UNAFFECTED));
		helper.setBlock(sink, Blocks.CHEST);
		container(helper, hopper).setItem(0, new ItemStack(CARGO));
		Storage<ItemVariant> side = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(hopper), Direction.UP);
		FakeMachine fullPipe = new FakeMachine(ItemStack.EMPTY, 0);
		helper.onEachTick(() -> StorageUtil.move(side, fullPipe, variant -> true, 1, null));
		helper.succeedWhen(() -> helper.assertValueEqual(container(helper, sink).countItem(CARGO), 1, "items in the chest"));
	}

	@GameTest(environment = QUIET, maxTicks = 200)
	public void aPipeFeedingAHopperCannotSpeedItUp(GameTestHelper helper) {
		BlockPos hopper = slot(0);
		BlockPos sink = hopper.below();
		Block block = unwaxed(WeatherState.UNAFFECTED);
		placeHopper(helper, hopper, block);
		helper.setBlock(sink, Blocks.CHEST);
		Storage<ItemVariant> side = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(hopper), Direction.UP);
		helper.onEachTick(() -> {
			try (Transaction transaction = Transaction.openOuter()) {
				side.insert(ItemVariant.of(CARGO), 1, transaction);
				transaction.commit();
			}
		});
		assertAllSteady(helper, List.of(new Arrivals(block, () -> container(helper, sink).countItem(CARGO))));
	}

	private static void assertAllSteady(GameTestHelper helper, List<Arrivals> arrivals) {
		helper.succeedWhen(() -> {
			arrivals.forEach(entry -> entry.sample(helper.getTick()));
			arrivals.forEach(entry -> entry.assertSteady(helper));
		});
	}

	private static BlockPos slot(int index) {
		return new BlockPos(3 * (index % 3), 2, 3 * (index / 3));
	}

	private static FakeMachine placeMachine(GameTestHelper helper, BlockPos pos, ItemStack contents, int capacity) {
		FakeMachine machine = new FakeMachine(contents, capacity);
		MACHINES.put(helper.absolutePos(pos), machine);
		helper.setBlock(pos, MACHINE_BLOCK);
		return machine;
	}

	private static void placeHopper(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		if (block instanceof CopperHopperBlock copper && copper.stage().runsWhen(true)) {
			helper.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
		}
	}

	private static Block unwaxed(WeatherState state) {
		return CopperHopperFeature.BLOCKS.weathering().pick(state);
	}

	private static BaseContainerBlockEntity container(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
	}

	private static final class FakeMachine extends SingleStackStorage {
		private ItemStack stack;
		private int capacity;

		private FakeMachine(ItemStack stack, int capacity) {
			this.stack = stack;
			this.capacity = capacity;
		}

		@Override
		protected ItemStack getStack() {
			return this.stack;
		}

		@Override
		protected void setStack(ItemStack stack) {
			this.stack = stack;
		}

		@Override
		protected int getCapacity(ItemVariant variant) {
			return this.capacity;
		}
	}

	private static final class Arrivals {
		private final Block block;
		private final int ticksPerItem;
		private final IntSupplier delivered;
		private final List<Long> ticks = new ArrayList<>();
		private int seen;

		private Arrivals(Block block, IntSupplier delivered) {
			this.block = block;
			this.ticksPerItem = block instanceof CopperHopperBlock copper ? copper.stage().ticksPerItem() : HopperBlockEntity.MOVE_ITEM_SPEED;
			this.delivered = delivered;
		}

		private void sample(long tick) {
			int now = this.delivered.getAsInt();
			if (now > this.seen) {
				this.seen = now;
				this.ticks.add(tick);
			}
		}

		private void assertSteady(GameTestHelper helper) {
			helper.assertTrue(this.ticks.size() >= 4, this.block + " only delivered at " + this.ticks);
			for (int i = 1; i < this.ticks.size(); i++) {
				helper.assertValueEqual(this.ticks.get(i) - this.ticks.get(i - 1), (long) this.ticksPerItem, this.block + " ticks per item in " + this.ticks);
			}
		}
	}
}
