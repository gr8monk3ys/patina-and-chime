package io.github.gr8monk3ys.patinachime.hopper;

import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Not a HopperBlockEntity subclass: that class fixes its type and its 8 tick cooldown, and Lithium casts and patches it.
public class CopperHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
	private static final int NO_COOLDOWN = -1;
	private static final Component DEFAULT_NAME = Component.translatable("container.patinachime.copper_hopper");
	private static final int[][] SLOT_RANGES = new int[54][];

	private NonNullList<ItemStack> items = NonNullList.withSize(HopperBlockEntity.HOPPER_CONTAINER_SIZE, ItemStack.EMPTY);
	private int cooldownTime = NO_COOLDOWN;
	private long tickedGameTime;

	public CopperHopperBlockEntity(BlockPos pos, BlockState state) {
		super(CopperHopperFeature.BLOCK_ENTITY, pos, state);
	}

	static void tick(Level level, BlockPos pos, BlockState state, CopperHopperBlockEntity hopper) {
		hopper.cooldownTime--;
		hopper.tickedGameTime = level.getGameTime();
		if (hopper.cooldownTime <= 0) {
			hopper.cooldownTime = 0;
			hopper.tryMoveItems(level, pos, state, () -> hopper.takeIn(level, stageOf(state)));
		}
	}

	static void entityInside(Level level, BlockPos pos, BlockState state, Entity entity, CopperHopperBlockEntity hopper) {
		if (stageOf(state).picksUpItems()
			&& entity instanceof ItemEntity item
			&& !item.getItem().isEmpty()
			&& entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(hopper.getSuckAabb())) {
			hopper.tryMoveItems(level, pos, state, () -> HopperBlockEntity.addItem(hopper, item));
		}
	}

	private static CopperHopperStage stageOf(BlockState state) {
		return ((CopperHopperBlock) state.getBlock()).stage();
	}

	private void tryMoveItems(Level level, BlockPos pos, BlockState state, BooleanSupplier intake) {
		CopperHopperStage stage = stageOf(state);
		if (level.isClientSide() || this.cooldownTime > 0 || !stage.runsWhen(!state.getValue(HopperBlock.ENABLED))) {
			return;
		}

		boolean changed = !this.isEmpty() && this.eject(level, pos, state);
		if (!this.isInventoryFull()) {
			changed |= intake.getAsBoolean();
		}

		if (changed) {
			this.cooldownTime = stage.ticksPerItem();
			setChanged(level, pos, state);
		}
	}

	private boolean eject(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(HopperBlock.FACING);
		BlockPos targetPos = pos.relative(facing);
		Direction side = facing.getOpposite();
		Container target = HopperBlockEntity.getContainerAt(level, targetPos);
		if (target == null) {
			Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, targetPos, side);
			return storage != null && StorageUtil.move(ContainerStorage.of(this, facing), storage, variant -> true, 1, null) == 1;
		}

		if (isFull(target, side)) {
			return false;
		}

		for (int slot = 0; slot < this.items.size(); slot++) {
			ItemStack stack = this.items.get(slot);
			if (!stack.isEmpty() && HopperBlockEntity.addItem(this, target, stack.copyWithCount(1), side).isEmpty()) {
				this.removeItem(slot, 1);
				target.setChanged();
				return true;
			}
		}

		return false;
	}

	private boolean takeIn(Level level, CopperHopperStage stage) {
		BlockPos above = this.getBlockPos().above();
		Container source = HopperBlockEntity.getContainerAt(level, above);
		if (source != null) {
			return stage.pullsFromAbove() && this.pullFrom(source);
		}

		Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, above, Direction.DOWN);
		if (storage != null) {
			return stage.pullsFromAbove() && StorageUtil.move(storage, ContainerStorage.of(this, Direction.UP), variant -> true, 1, null) == 1;
		}

		if (!stage.picksUpItems()) {
			return false;
		}

		BlockState aboveState = level.getBlockState(above);
		if (aboveState.isCollisionShapeFullBlock(level, above) && !aboveState.is(BlockTags.DOES_NOT_BLOCK_HOPPERS)) {
			return false;
		}

		for (ItemEntity item : HopperBlockEntity.getItemsAtAndAbove(level, this)) {
			if (HopperBlockEntity.addItem(this, item)) {
				return true;
			}
		}

		return false;
	}

	private boolean pullFrom(Container source) {
		for (int slot : slotsOf(source, Direction.DOWN)) {
			ItemStack stack = source.getItem(slot);
			if (!stack.isEmpty()
				&& source.canTakeItem(this, slot, stack)
				&& (!(source instanceof WorldlyContainer worldly) || worldly.canTakeItemThroughFace(slot, stack, Direction.DOWN))) {
				int count = stack.getCount();
				if (HopperBlockEntity.addItem(source, this, source.removeItem(slot, 1), null).isEmpty()) {
					source.setChanged();
					return true;
				}

				// Puts back an item that didn't fit, the way vanilla does.
				stack.setCount(count);
				if (count == 1) {
					source.setItem(slot, stack);
				}
			}
		}

		return false;
	}

	private boolean isInventoryFull() {
		for (ItemStack stack : this.items) {
			if (stack.getCount() < stack.getMaxStackSize()) {
				return false;
			}
		}

		return true;
	}

	private static boolean isFull(Container container, Direction side) {
		for (int slot : slotsOf(container, side)) {
			ItemStack stack = container.getItem(slot);
			if (stack.getCount() < stack.getMaxStackSize()) {
				return false;
			}
		}

		return true;
	}

	private static int[] slotsOf(Container container, Direction side) {
		if (container instanceof WorldlyContainer worldly) {
			return worldly.getSlotsForFace(side);
		}

		int size = container.getContainerSize();
		if (size >= SLOT_RANGES.length) {
			return IntStream.range(0, size).toArray();
		}

		if (SLOT_RANGES[size] == null) {
			SLOT_RANGES[size] = IntStream.range(0, size).toArray();
		}

		return SLOT_RANGES[size];
	}

	// Vanilla paces a hopper that just got its first item, but only when the receiver is a HopperBlockEntity.
	// Items arrive here through setItem both in vanilla's code and in Lithium's, so a chain keeps its rate under either.
	// A Fabric transaction also empties and refills slots just to test a move, so nothing in one counts as an arrival.
	@Override
	public void setItem(int slot, ItemStack stack) {
		boolean wasEmpty = this.isEmpty();
		super.setItem(slot, stack);
		if (wasEmpty && !stack.isEmpty() && this.level != null && !Transaction.isOpen()) {
			int ticksPerItem = stageOf(this.getBlockState()).ticksPerItem();
			if (this.cooldownTime <= ticksPerItem) {
				this.cooldownTime = this.tickedGameTime >= this.level.getGameTime() ? ticksPerItem - 1 : ticksPerItem;
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, this.items);
		}

		this.cooldownTime = input.getIntOr("TransferCooldown", NO_COOLDOWN);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, this.items);
		}

		output.putInt("TransferCooldown", this.cooldownTime);
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new HopperMenu(containerId, inventory, this);
	}

	@Override
	public double getLevelX() {
		return this.worldPosition.getX() + 0.5;
	}

	@Override
	public double getLevelY() {
		return this.worldPosition.getY() + 0.5;
	}

	@Override
	public double getLevelZ() {
		return this.worldPosition.getZ() + 0.5;
	}

	@Override
	public boolean isGridAligned() {
		return true;
	}
}
