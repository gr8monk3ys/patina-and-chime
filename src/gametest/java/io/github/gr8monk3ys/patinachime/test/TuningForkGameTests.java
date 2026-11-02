package io.github.gr8monk3ys.patinachime.test;

import com.mojang.authlib.GameProfile;
import io.github.gr8monk3ys.patinachime.PatinaChime;
import io.github.gr8monk3ys.patinachime.tuningfork.BuddingAmethystScan;
import io.github.gr8monk3ys.patinachime.tuningfork.TuningForkFeature;
import io.github.gr8monk3ys.patinachime.tuningfork.TuningForkTier;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

// Scan tests build and clear their blocks inside one server tick, in chunks far from every test structure,
// so no other test's budding amethyst can land in range.
public class TuningForkGameTests {
	private static final BlockPos CENTER = new BlockPos(160_007, 100, 160_007);
	private static final BlockPos WORST_CASE_CENTER = new BlockPos(192_007, 100, 192_007);
	private static final BlockPos UNTOUCHED_CENTER = new BlockPos(256_007, 100, 256_007);
	private static final int LOAD_RANGE = BuddingAmethystScan.RANGE + 1;

	private static void loadAround(ServerLevel level, BlockPos center) {
		for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - LOAD_RANGE); chunkX <= SectionPos.blockToSectionCoord(center.getX() + LOAD_RANGE); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - LOAD_RANGE); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + LOAD_RANGE); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
	}

	private static void assertTierWith(GameTestHelper helper, BlockPos center, TuningForkTier expected, BlockPos... budding) {
		ServerLevel level = helper.getLevel();
		for (BlockPos pos : budding) {
			level.setBlock(pos, Blocks.BUDDING_AMETHYST.defaultBlockState(), Block.UPDATE_ALL);
		}

		TuningForkTier actual = BuddingAmethystScan.scan(level, center);

		for (BlockPos pos : budding) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}

		helper.assertValueEqual(actual, expected, "tier from " + center + " with budding amethyst at " + Arrays.toString(budding));
	}

	private static void assertTierAtDistance(GameTestHelper helper, int distance, TuningForkTier expected) {
		loadAround(helper.getLevel(), CENTER);
		for (Direction direction : Direction.values()) {
			assertTierWith(helper, CENTER, expected, CENTER.relative(direction, distance));
		}
	}

	@GameTest
	public void strongAtZeroToEight(GameTestHelper helper) {
		assertTierAtDistance(helper, 0, TuningForkTier.STRONG);
		assertTierAtDistance(helper, 1, TuningForkTier.STRONG);
		assertTierAtDistance(helper, 8, TuningForkTier.STRONG);
		helper.succeed();
	}

	@GameTest
	public void steadyAtNineToSixteen(GameTestHelper helper) {
		assertTierAtDistance(helper, 9, TuningForkTier.STEADY);
		assertTierAtDistance(helper, 16, TuningForkTier.STEADY);
		helper.succeed();
	}

	@GameTest
	public void faintAtSeventeenToThirtyTwo(GameTestHelper helper) {
		assertTierAtDistance(helper, 17, TuningForkTier.FAINT);
		assertTierAtDistance(helper, 32, TuningForkTier.FAINT);
		helper.succeed();
	}

	@GameTest
	public void noneAtThirtyThree(GameTestHelper helper) {
		assertTierAtDistance(helper, 33, TuningForkTier.NONE);
		helper.succeed();
	}

	@GameTest
	public void noneInAnEmptyRegion(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		loadAround(level, CENTER);
		helper.assertValueEqual(BuddingAmethystScan.scan(level, CENTER), TuningForkTier.NONE, "tier with nothing placed");
		helper.succeed();
	}

	@GameTest
	public void cubeCornersCountAsInRange(GameTestHelper helper) {
		loadAround(helper.getLevel(), CENTER);
		assertTierWith(helper, CENTER, TuningForkTier.STRONG, CENTER.offset(8, -8, 8));
		assertTierWith(helper, CENTER, TuningForkTier.STEADY, CENTER.offset(9, 1, -1));
		assertTierWith(helper, CENTER, TuningForkTier.STEADY, CENTER.offset(-16, 16, -16));
		assertTierWith(helper, CENTER, TuningForkTier.FAINT, CENTER.offset(32, 32, 32));
		assertTierWith(helper, CENTER, TuningForkTier.FAINT, CENTER.offset(-32, -32, -32));
		assertTierWith(helper, CENTER, TuningForkTier.NONE, CENTER.offset(33, 32, 32));
		assertTierWith(helper, CENTER, TuningForkTier.NONE, CENTER.offset(32, -33, 32));
		assertTierWith(helper, CENTER, TuningForkTier.NONE, CENTER.offset(32, 32, -33));
		helper.succeed();
	}

	@GameTest
	public void nearestWinsWhicheverIsFoundFirst(GameTestHelper helper) {
		loadAround(helper.getLevel(), CENTER);
		assertTierWith(helper, CENTER, TuningForkTier.STRONG, CENTER.offset(-30, -30, -30), CENTER.offset(5, 0, 0));
		assertTierWith(helper, CENTER, TuningForkTier.STRONG, CENTER.offset(30, 30, 30), CENTER.offset(-5, 0, 0));
		assertTierWith(helper, CENTER, TuningForkTier.STEADY, CENTER.offset(0, 0, 25), CENTER.offset(0, 0, 12), CENTER.offset(0, 0, -33));
		helper.succeed();
	}

	@GameTest
	public void onlyBuddingAmethystCounts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		loadAround(level, CENTER);
		BlockPos pos = CENTER.offset(3, 0, 0);
		for (Block lookalike : List.of(Blocks.AMETHYST_BLOCK, Blocks.AMETHYST_CLUSTER, Blocks.SMALL_AMETHYST_BUD)) {
			level.setBlock(pos, lookalike.defaultBlockState(), Block.UPDATE_ALL);
			TuningForkTier actual = BuddingAmethystScan.scan(level, CENTER);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertValueEqual(actual, TuningForkTier.NONE, "tier next to " + lookalike);
		}

		helper.succeed();
	}

	@GameTest
	public void worldLimitsAreClamped(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		loadAround(level, CENTER);
		BlockPos bottom = new BlockPos(CENTER.getX(), level.getMinY(), CENTER.getZ());
		BlockPos top = new BlockPos(CENTER.getX(), level.getMaxY(), CENTER.getZ());

		assertTierWith(helper, bottom, TuningForkTier.STRONG, bottom);
		assertTierWith(helper, top, TuningForkTier.STRONG, top);
		assertTierWith(helper, bottom.below(5), TuningForkTier.STEADY, bottom.above(5));
		assertTierWith(helper, top.above(5), TuningForkTier.STEADY, top.below(5));
		helper.assertValueEqual(BuddingAmethystScan.scan(level, top.above(500)), TuningForkTier.NONE, "tier far above the world");
		helper.assertValueEqual(BuddingAmethystScan.scan(level, bottom.below(500)), TuningForkTier.NONE, "tier far below the world");
		helper.succeed();
	}

	@GameTest
	public void scanNeverLoadsAChunk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = new BlockPos(-224_007, 100, -224_007);
		ChunkPos centerChunk = ChunkPos.containing(center);
		BlockPos farCorner = center.offset(32, 0, 32);
		helper.assertFalse(level.getChunkSource().hasChunk(centerChunk.x(), centerChunk.z()), "test region should start unloaded");

		helper.assertValueEqual(BuddingAmethystScan.scan(level, center), TuningForkTier.NONE, "tier with every chunk unloaded");

		for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - 32); chunkX <= SectionPos.blockToSectionCoord(farCorner.getX()); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - 32); chunkZ <= SectionPos.blockToSectionCoord(farCorner.getZ()); chunkZ++) {
				helper.assertFalse(level.getChunkSource().hasChunk(chunkX, chunkZ), "scan loaded chunk " + chunkX + ", " + chunkZ);
			}
		}

		level.getChunk(centerChunk.x(), centerChunk.z());
		assertTierWith(helper, center, TuningForkTier.STRONG, center);
		helper.assertFalse(level.getChunkSource().hasChunk(centerChunk.x() + 2, centerChunk.z() + 2), "scan loaded a chunk beside the loaded one");
		helper.succeed();
	}

	private record ConnectedPlayer(ServerPlayer player, List<Component> overlay) {
		void disconnect() {
			this.player.level().getServer().getPlayerList().remove(this.player);
		}
	}

	private static ConnectedPlayer connectPlayerAt(GameTestHelper helper, BlockPos pos) {
		ServerLevel level = helper.getLevel();
		List<Component> overlay = new ArrayList<>();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "fork-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public void sendOverlayMessage(Component message) {
				overlay.add(message);
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return new ConnectedPlayer(player, overlay);
	}

	private static String translationKey(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : component.getString();
	}

	@GameTest
	public void serverUseAppliesTheOneSecondCooldown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		loadAround(level, CENTER);
		ConnectedPlayer connected = connectPlayerAt(helper, CENTER);
		ServerPlayer player = connected.player();
		ItemStack fork = new ItemStack(TuningForkFeature.TUNING_FORK);
		player.setItemInHand(InteractionHand.MAIN_HAND, fork);

		try {
			helper.assertTrue(player.gameMode.useItem(player, level, fork, InteractionHand.MAIN_HAND).consumesAction(), "first strike should consume the action");
			helper.assertValueEqual(connected.overlay().size(), 1, "messages after the first strike");
			helper.assertTrue(player.getCooldowns().isOnCooldown(fork), "fork should be cooling down");

			InteractionResult second = player.gameMode.useItem(player, level, fork, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(second, InteractionResult.PASS, "strike during the cooldown");
			helper.assertValueEqual(connected.overlay().size(), 1, "messages after striking during the cooldown");

			for (int tick = 1; tick < 20; tick++) {
				player.getCooldowns().tick();
				helper.assertTrue(player.getCooldowns().isOnCooldown(fork), "fork should still cool down at tick " + tick);
			}

			player.getCooldowns().tick();
			helper.assertFalse(player.getCooldowns().isOnCooldown(fork), "fork should be ready after 20 ticks");
			helper.assertTrue(player.gameMode.useItem(player, level, fork, InteractionHand.MAIN_HAND).consumesAction(), "strike after the cooldown should work");
			helper.assertValueEqual(connected.overlay().size(), 2, "messages after the cooldown ended");
		} finally {
			connected.disconnect();
		}

		helper.succeed();
	}

	@GameTest
	public void serverUseShowsTheMessageForEachTier(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		loadAround(level, CENTER);
		ConnectedPlayer connected = connectPlayerAt(helper, CENTER);
		ServerPlayer player = connected.player();
		ItemStack fork = new ItemStack(TuningForkFeature.TUNING_FORK);
		player.setItemInHand(InteractionHand.MAIN_HAND, fork);

		try {
			assertMessage(helper, connected, fork, 33, "message.patinachime.tuning_fork.none");
			assertMessage(helper, connected, fork, 32, "message.patinachime.tuning_fork.faint");
			assertMessage(helper, connected, fork, 16, "message.patinachime.tuning_fork.steady");
			assertMessage(helper, connected, fork, 8, "message.patinachime.tuning_fork.strong");
		} finally {
			connected.disconnect();
		}

		helper.succeed();
	}

	private static void assertMessage(GameTestHelper helper, ConnectedPlayer connected, ItemStack fork, int distance, String key) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = connected.player();
		BlockPos budding = CENTER.relative(Direction.NORTH, distance);
		level.setBlock(budding, Blocks.BUDDING_AMETHYST.defaultBlockState(), Block.UPDATE_ALL);
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(fork));

		player.gameMode.useItem(player, level, fork, InteractionHand.MAIN_HAND);

		level.setBlock(budding, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertValueEqual(translationKey(connected.overlay().getLast()), key, "message at distance " + distance);
	}

	@GameTest
	public void everyTierHasItsOwnRegisteredSoundWithPitchRisingWithCloseness(GameTestHelper helper) {
		for (TuningForkTier tier : TuningForkTier.values()) {
			helper.assertValueEqual(
				BuiltInRegistries.SOUND_EVENT.getKey(tier.sound()), PatinaChime.id("tuning_fork." + tier.name().toLowerCase(Locale.ROOT)), "registered sound id for " + tier
			);
		}

		helper.assertTrue(TuningForkTier.FAINT.pitch() < TuningForkTier.STEADY.pitch(), "faint should be lower than steady");
		helper.assertTrue(TuningForkTier.STEADY.pitch() < TuningForkTier.STRONG.pitch(), "steady should be lower than strong");
		helper.succeed();
	}

	@GameTest
	public void forkIsAnUnbreakableSingleStack(GameTestHelper helper) {
		ItemStack fork = new ItemStack(TuningForkFeature.TUNING_FORK);
		helper.assertValueEqual(fork.getMaxStackSize(), 1, "max stack size");
		helper.assertFalse(fork.isDamageableItem(), "fork should have no durability");
		helper.succeed();
	}

	@GameTest
	public void forkIsListedAfterSpyglassInTheToolsTab(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());

		List<Item> items = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.TOOLS_AND_UTILITIES).getDisplayItems().stream().map(ItemStack::getItem).toList();

		helper.assertTrue(items.contains(TuningForkFeature.TUNING_FORK), "tools tab should list the tuning fork");
		helper.assertTrue(items.indexOf(TuningForkFeature.TUNING_FORK) > items.indexOf(Items.SPYGLASS), "tuning fork should follow the spyglass");
		helper.succeed();
	}

	@GameTest
	public void forkRecipeExistsAndLeavesTheSpyglassAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack copper = new ItemStack(Items.COPPER_INGOT);
		ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
		ItemStack empty = ItemStack.EMPTY;

		CraftingInput forkShape = CraftingInput.of(3, 3, List.of(copper, empty, copper, empty, shard, empty, empty, copper, empty));
		RecipeHolder<CraftingRecipe> fork = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, forkShape, level)
			.orElseThrow(() -> helper.assertionException("fork shape matched no recipe"));
		helper.assertValueEqual(fork.id().identifier(), PatinaChime.id("tuning_fork"), "fork recipe id");
		ItemStack forkResult = fork.value().assemble(forkShape);
		helper.assertTrue(forkResult.is(TuningForkFeature.TUNING_FORK), "fork shape should craft a tuning fork");
		helper.assertValueEqual(forkResult.getCount(), 1, "forks crafted");

		CraftingInput spyglassShape = CraftingInput.of(1, 3, List.of(shard, copper, copper));
		ItemStack spyglassResult = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, spyglassShape, level)
			.orElseThrow(() -> helper.assertionException("spyglass shape matched no recipe"))
			.value().assemble(spyglassShape);
		helper.assertTrue(spyglassResult.is(Items.SPYGLASS), "spyglass shape should still craft a spyglass");
		helper.succeed();
	}

	// Worst case: every one of the 125 sections the cube touches keeps budding amethyst in its palette, so none can be
	// skipped, yet no block matches, so the search never narrows and reads all 65^3 positions.
	@GameTest
	public void worstCaseScanFitsInOneTick(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = WORST_CASE_CENTER;
		loadAround(level, center);
		int sections = dirtyEverySectionPalette(helper, center);
		helper.assertValueEqual(sections, 125, "sections touched by the cube");

		long coldNanos = timeScan(helper, center);
		long[] worst = new long[500];
		for (int run = 0; run < worst.length; run++) {
			worst[run] = timeScan(helper, center);
		}
		Arrays.sort(worst);

		loadAround(level, UNTOUCHED_CENTER);
		long[] skipped = new long[500];
		for (int run = 0; run < skipped.length; run++) {
			skipped[run] = timeScan(helper, UNTOUCHED_CENTER);
		}
		Arrays.sort(skipped);

		PatinaChime.LOGGER.info(
			"Tuning fork scan: worst case (65^3 reads) cold {} us, median {} us, p99 {} us, max {} us; palette-skip case median {} us, max {} us",
			coldNanos / 1_000,
			worst[worst.length / 2] / 1_000,
			worst[worst.length * 99 / 100] / 1_000,
			worst[worst.length - 1] / 1_000,
			skipped[skipped.length / 2] / 1_000,
			skipped[skipped.length - 1] / 1_000
		);
		helper.assertTrue(worst[worst.length / 2] < 50_000_000L, "worst case median should stay under one 50 ms tick");
		helper.succeed();
	}

	private static long timeScan(GameTestHelper helper, BlockPos center) {
		long start = System.nanoTime();
		TuningForkTier tier = BuddingAmethystScan.scan(helper.getLevel(), center);
		long elapsed = System.nanoTime() - start;
		helper.assertValueEqual(tier, TuningForkTier.NONE, "tier during the timing run");
		return elapsed;
	}

	private static int dirtyEverySectionPalette(GameTestHelper helper, BlockPos center) {
		ServerLevel level = helper.getLevel();
		BlockState budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();
		int range = BuddingAmethystScan.RANGE;
		int sections = 0;

		for (int sectionX = SectionPos.blockToSectionCoord(center.getX() - range); sectionX <= SectionPos.blockToSectionCoord(center.getX() + range); sectionX++) {
			for (int sectionZ = SectionPos.blockToSectionCoord(center.getZ() - range); sectionZ <= SectionPos.blockToSectionCoord(center.getZ() + range); sectionZ++) {
				for (int sectionY = SectionPos.blockToSectionCoord(center.getY() - range); sectionY <= SectionPos.blockToSectionCoord(center.getY() + range); sectionY++) {
					BlockPos corner = SectionPos.of(sectionX, sectionY, sectionZ).origin();
					level.setBlock(corner, budding, Block.UPDATE_ALL);
					level.setBlock(corner, stone, Block.UPDATE_ALL);
					LevelChunkSection section = level.getChunk(sectionX, sectionZ).getSection(level.getSectionIndexFromSectionY(sectionY));
					helper.assertTrue(section.maybeHas(state -> state.is(Blocks.BUDDING_AMETHYST)), "section palette should keep budding amethyst");
					sections++;
				}
			}
		}

		return sections;
	}
}
