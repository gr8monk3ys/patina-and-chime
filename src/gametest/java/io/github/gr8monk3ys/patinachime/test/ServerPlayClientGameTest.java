package io.github.gr8monk3ys.patinachime.test;

import static io.github.gr8monk3ys.patinachime.test.ClientActions.*;

import io.github.gr8monk3ys.patinachime.hopper.CopperHopperFeature;
import io.github.gr8monk3ys.patinachime.hopper.CopperHopperStage;
import io.github.gr8monk3ys.patinachime.tuningfork.TuningForkFeature;
import io.github.gr8monk3ys.patinachime.tuningfork.TuningForkTier;
import io.github.gr8monk3ys.patinachime.verdigris.VerdigrisFeature;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plays the mod from a survival player's seat, with real keys and clicks, on a dedicated server over a real socket.
 * That is where a menu's data is cut to shorts, sounds and messages arrive as packets, and nothing is shared with the
 * server by accident. It also reopens a saved world, and finishes by rejoining the server.
 */
public class ServerPlayClientGameTest implements FabricClientGameTest {
	// Crafting menu: 0 is the result, 1-9 the grid, 10-36 the main inventory, 37-45 the hotbar.
	private static final int CRAFT_RESULT = 0;
	private static final int CRAFT_HOTBAR = 37;
	// Hopper menu: 0-4 are the hopper, 5-31 the main inventory, 32-40 the hotbar.
	private static final int HOPPER_HOTBAR = 32;
	private static final int HOTBAR_TABLE = 0;
	private static final int HOTBAR_INGOTS = 1;
	private static final int HOTBAR_CHESTS = 2;
	private static final int HOTBAR_SHARD = 3;
	private static final int HOTBAR_WATER = 4;
	private static final int HOTBAR_HOPPER = 5;
	private static final int HOTBAR_FORK = 6;
	private static final int HOTBAR_VERDIGRIS = 7;
	private static final int HOTBAR_SPARE = 8;
	private static final int FORK_COOLDOWN_TICKS = 25;
	private static final float NOTE_VOLUME = 3.0F;
	private static final BlockPos TABLE = new BlockPos(2, Y, 0);
	private static final BlockPos CHEST = new BlockPos(6, Y, 0);
	private static final BlockPos HOPPER = CHEST.above();
	private static final BlockPos LEFT_BEHIND = new BlockPos(40, Y, 0);
	private static final Item FRESH_HOPPER = CopperHopperFeature.ITEMS.weathering().pick(WeatherState.UNAFFECTED);
	private static final Item OXIDIZED_HOPPER = CopperHopperFeature.ITEMS.weathering().pick(WeatherState.OXIDIZED);

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		// Sound events still reach the listener below at zero volume.
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
		savingAndReopening(context);

		SoundLog sounds = new SoundLog();
		context.runOnClient(client -> client.getSoundManager().addListener(sounds));

		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				assertRealSocket(context);
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("difficulty peaceful");
				server.runCommand("gamemode survival @a");
				// The stages are asserted exactly, so nothing may age by itself.
				server.runCommand("gamerule random_tick_speed 0");
				server.runCommand("gamerule spawn_mobs false");
				removeMobs(server);

				crafting(context, server, connection);
				placing(context, server, connection);
				hopperMenu(context, server, connection);
				changingStage(context, server, connection, sounds);
				dustOnTop(context, server, connection);
				breaking(context, server, connection);
				tuningFork(context, server, connection, sounds);
				noteBlocks(context, server, connection, sounds);
				enchanting(context, server, connection);
				music(context, server, connection);
				server.runCommand("gamerule random_tick_speed 0");

				server.runCommand("gamemode survival @a");
				server.runCommand("setblock %d %d %d patinachime:waxed_weathered_copper_hopper[facing=east]".formatted(LEFT_BEHIND.getX(), LEFT_BEHIND.getY(), LEFT_BEHIND.getZ()));
				server.runCommand("item replace block %d %d %d container.1 with minecraft:emerald 9".formatted(LEFT_BEHIND.getX(), LEFT_BEHIND.getY(), LEFT_BEHIND.getZ()));
			}

			// This test keeps the server in the client's JVM, where Forge Config API Port can't sync its configs a second time.
			if (!FabricLoader.getInstance().isModLoaded("forgeconfigapiport")) {
				try (TestDedicatedServerConnection connection = server.connect()) {
					connection.waitForChunksRender();
					rejoining(context, server, connection);
				}
			}
		}

		context.runOnClient(client -> client.getSoundManager().removeListener(sounds));
	}

	private static void assertRealSocket(ClientGameTestContext context) {
		boolean inMemory = context.computeOnClient(client -> client.getConnection().getConnection().isMemoryConnection());
		if (inMemory) {
			throw new AssertionError("this test only means something over a real connection");
		}
	}

	private static void crafting(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		give(server, HOTBAR_TABLE, "crafting_table");
		give(server, HOTBAR_INGOTS, "copper_ingot 64");
		give(server, HOTBAR_CHESTS, "chest 2");
		give(server, HOTBAR_SHARD, "amethyst_shard");
		give(server, HOTBAR_WATER, "water_bucket");

		view(context, connection, server, "2.5 %d 2.2".formatted(Y));
		select(context, HOTBAR_TABLE);
		aimAt(context, topOf(TABLE.below()));
		use(context, connection);
		assertBlock(context, server, connection, TABLE, "minecraft:crafting_table");
		open(context, connection, TABLE, CraftingScreen.class);

		lay(context, connection, CRAFT_HOTBAR + HOTBAR_INGOTS, 1, 3, 4, 6, 8);
		lay(context, connection, CRAFT_HOTBAR + HOTBAR_CHESTS, 5);
		context.waitFor(client -> resultIs(client, FRESH_HOPPER));
		context.takeScreenshot("server-crafting-copper-hopper");
		takeResult(context, connection, FRESH_HOPPER, 1, CRAFT_HOTBAR + HOTBAR_HOPPER);

		lay(context, connection, CRAFT_HOTBAR + HOTBAR_INGOTS, 1, 3, 8);
		lay(context, connection, CRAFT_HOTBAR + HOTBAR_SHARD, 5);
		takeResult(context, connection, TuningForkFeature.TUNING_FORK, 1, CRAFT_HOTBAR + HOTBAR_FORK);

		lay(context, connection, CRAFT_HOTBAR + HOTBAR_INGOTS, 1);
		lay(context, connection, CRAFT_HOTBAR + HOTBAR_WATER, 2);
		takeResult(context, connection, VerdigrisFeature.VERDIGRIS, 2, CRAFT_HOTBAR + HOTBAR_VERDIGRIS);
		close(context);
		settle(context, connection);

		assertHolding(context, server, connection, Items.COPPER_INGOT, 55);
		assertHolding(context, server, connection, Items.CHEST, 1);
		assertHolding(context, server, connection, Items.AMETHYST_SHARD, 0);
		assertHolding(context, server, connection, Items.WATER_BUCKET, 0);
		assertHolding(context, server, connection, Items.BUCKET, 1);
		assertHolding(context, server, connection, FRESH_HOPPER, 1);
		assertHolding(context, server, connection, TuningForkFeature.TUNING_FORK, 1);
		assertHolding(context, server, connection, VerdigrisFeature.VERDIGRIS, 2);
	}

	// Spreads a stack one item per grid slot with right clicks, then puts the rest back.
	private static void lay(ClientGameTestContext context, TestServerConnection connection, int from, int... grid) {
		click(context, from, LEFT_CLICK);
		for (int slot : grid) {
			click(context, slot, RIGHT_CLICK);
		}

		click(context, from, LEFT_CLICK);
		settle(context, connection);
	}

	private static boolean resultIs(Minecraft client, Item item) {
		return client.player.containerMenu.getSlot(CRAFT_RESULT).getItem().is(item);
	}

	private static void takeResult(ClientGameTestContext context, TestServerConnection connection, Item item, int count, int to) {
		context.waitFor(client -> resultIs(client, item));
		click(context, CRAFT_RESULT, LEFT_CLICK);
		click(context, to, LEFT_CLICK);
		settle(context, connection);
		ItemStack taken = context.computeOnClient(client -> client.player.containerMenu.getSlot(to).getItem().copy());
		if (!taken.is(item) || taken.getCount() != count) {
			throw new AssertionError("expected " + count + " " + item + " from the grid but got " + taken);
		}
	}

	private static void placing(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		view(context, connection, server, "6.5 %d 3".formatted(Y));
		select(context, HOTBAR_CHESTS);
		aimAt(context, topOf(CHEST.below()));
		use(context, connection);
		assertBlock(context, server, connection, CHEST, "minecraft:chest");

		select(context, HOTBAR_HOPPER);
		sneaking(context, () -> {
			aimAt(context, new Vec3(CHEST.getX() + 0.5, CHEST.getY() + 0.87, CHEST.getZ() + 0.5));
			use(context, connection);
		});
		assertBlock(context, server, connection, HOPPER, "patinachime:copper_hopper");
		assertFacingDown(context, server, connection);
		assertHolding(context, server, connection, FRESH_HOPPER, 0);
	}

	private static void hopperMenu(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		give(server, HOTBAR_SPARE, "cobblestone 16");
		settle(context, connection);
		select(context, HOTBAR_SPARE);
		open(context, connection, HOPPER, HopperScreen.class);
		move(context, connection, HOPPER_HOTBAR + HOTBAR_SPARE, 0);

		context.waitFor(client -> menuCount(client, 0) <= 14);
		long start = gameTime(server);
		context.waitFor(client -> menuCount(client, 0) <= 6);
		long elapsed = gameTime(server) - start;
		long expected = 8L * CopperHopperStage.FRESH.ticksPerItem();
		if (Math.abs(elapsed - expected) > 10) {
			throw new AssertionError("the menu showed 8 items leave in " + elapsed + " ticks, not about " + expected);
		}

		context.takeScreenshot("server-hopper-menu-flowing");
		context.waitFor(client -> menuCount(client, 0) == 0);
		close(context);

		open(context, connection, CHEST, ContainerScreen.class);
		context.takeScreenshot("server-chest-filled-by-hopper");
		int inChest = context.computeOnClient(client -> menuCount(client, 0));
		if (inChest != 16) {
			throw new AssertionError("the chest menu shows " + inChest + " items, not 16");
		}

		close(context);
	}

	private static void changingStage(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, SoundLog sounds) {
		server.runCommand("item replace block %d %d %d container.0 with minecraft:gold_ingot 20".formatted(HOPPER.getX(), HOPPER.getY(), HOPPER.getZ()));
		give(server, HOTBAR_SHARD, "honeycomb 2");
		give(server, HOTBAR_WATER, "iron_axe");
		settle(context, connection);

		sounds.clear();
		useOnHopper(context, connection, HOTBAR_VERDIGRIS);
		assertBlock(context, server, connection, HOPPER, "patinachime:exposed_copper_hopper");
		assertFacingDown(context, server, connection);
		assertNoScreen(context);
		assertHolding(context, server, connection, VerdigrisFeature.VERDIGRIS, 1);
		context.waitFor(client -> sounds.find("minecraft:block.copper.place").isPresent());

		useOnHopper(context, connection, HOTBAR_SHARD);
		assertBlock(context, server, connection, HOPPER, "patinachime:waxed_exposed_copper_hopper");
		assertNoScreen(context);
		assertHolding(context, server, connection, Items.HONEYCOMB, 1);

		// Wax stops verdigris, which then opens the menu like any other item in hand.
		useOnHopper(context, connection, HOTBAR_VERDIGRIS);
		context.waitForScreen(HopperScreen.class);
		close(context);
		assertBlock(context, server, connection, HOPPER, "patinachime:waxed_exposed_copper_hopper");
		assertHolding(context, server, connection, VerdigrisFeature.VERDIGRIS, 1);

		useOnHopper(context, connection, HOTBAR_WATER);
		assertBlock(context, server, connection, HOPPER, "patinachime:exposed_copper_hopper");
		assertNoScreen(context);
		assertAround(server, connection, HOPPER, VerdigrisFeature.VERDIGRIS, 1);

		useOnHopper(context, connection, HOTBAR_WATER);
		assertBlock(context, server, connection, HOPPER, "patinachime:copper_hopper");
		assertNoScreen(context);
		assertAround(server, connection, HOPPER, VerdigrisFeature.VERDIGRIS, 2);
		assertFacingDown(context, server, connection);

		assertAround(server, connection, HOPPER, Items.GOLD_INGOT, 20);
		server.waitFor(ignored -> containerCount(connection.getServerLevel(), CHEST, Items.GOLD_INGOT) == 20, 500);
	}

	private static void useOnHopper(ClientGameTestContext context, TestServerConnection connection, int hotbar) {
		select(context, hotbar);
		context.getInput().lookAt(HOPPER);
		context.waitTick();
		assertAimingAt(context, HOPPER);
		use(context, connection);
	}

	private static void dustOnTop(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		BlockPos hopper = new BlockPos(10, Y, 0);
		server.runCommand("setblock %d %d %d patinachime:copper_hopper".formatted(hopper.getX(), hopper.getY(), hopper.getZ()));
		give(server, HOTBAR_CHESTS, "redstone 4");
		view(context, connection, server, "10.5 %d 2.4".formatted(Y));
		select(context, HOTBAR_CHESTS);
		sneaking(context, () -> {
			aimAt(context, topOf(hopper));
			use(context, connection);
		});
		assertBlock(context, server, connection, hopper.above(), "minecraft:redstone_wire");
		assertHolding(context, server, connection, Items.REDSTONE, 3);
	}

	private static void breaking(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		BlockPos hopper = new BlockPos(14, Y, 0);
		server.runCommand("setblock %d %d %d patinachime:oxidized_copper_hopper".formatted(hopper.getX(), hopper.getY(), hopper.getZ()));
		server.runCommand("item replace block %d %d %d container.0 with minecraft:diamond 3".formatted(hopper.getX(), hopper.getY(), hopper.getZ()));
		give(server, HOTBAR_TABLE, "iron_pickaxe");
		view(context, connection, server, "14.5 %d 2.4".formatted(Y));
		select(context, HOTBAR_TABLE);
		context.getInput().lookAt(hopper);
		context.waitTick();
		context.getInput().holdKey(options -> options.keyAttack);
		context.waitFor(client -> client.level.getBlockState(hopper).isAir());
		context.getInput().releaseKey(options -> options.keyAttack);
		settle(context, connection);
		assertBlock(context, server, connection, hopper, "minecraft:air");
		assertAround(server, connection, hopper, OXIDIZED_HOPPER, 1);
		assertAround(server, connection, hopper, Items.DIAMOND, 3);
	}

	private static void tuningFork(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, SoundLog sounds) {
		view(context, connection, server, "200.5 %d 200.5".formatted(Y));
		select(context, HOTBAR_FORK);
		context.getInput().lookAt(0.0F, -90.0F);
		context.waitTick();

		server.runCommand("setblock 205 %d 200 budding_amethyst".formatted(Y));
		assertForkSays(context, connection, sounds, TuningForkTier.STRONG);
		context.takeScreenshot("server-tuning-fork-action-bar");

		sounds.clear();
		context.getInput().pressKey(options -> options.keyUse);
		settle(context, connection);
		context.waitTicks(5);
		if (sounds.heard.stream().anyMatch(heard -> heard.id().startsWith("patinachime:tuning_fork."))) {
			throw new AssertionError("the fork rang again during its cooldown");
		}

		context.waitTicks(FORK_COOLDOWN_TICKS);
		server.runCommand("setblock 205 %d 200 air".formatted(Y));
		server.runCommand("setblock 212 %d 200 budding_amethyst".formatted(Y));
		assertForkSays(context, connection, sounds, TuningForkTier.STEADY);

		context.waitTicks(FORK_COOLDOWN_TICKS);
		server.runCommand("setblock 212 %d 200 air".formatted(Y));
		server.runCommand("setblock 225 %d 200 budding_amethyst".formatted(Y));
		assertForkSays(context, connection, sounds, TuningForkTier.FAINT);

		context.waitTicks(FORK_COOLDOWN_TICKS);
		server.runCommand("setblock 225 %d 200 air".formatted(Y));
		assertForkSays(context, connection, sounds, TuningForkTier.NONE);
	}

	private static void assertForkSays(ClientGameTestContext context, TestServerConnection connection, SoundLog sounds, TuningForkTier tier) {
		sounds.clear();
		context.getInput().pressKey(options -> options.keyUse);
		String id = "patinachime:tuning_fork." + tier.name().toLowerCase(Locale.ROOT);
		context.waitFor(client -> sounds.find(id).isPresent());
		settle(context, connection);
		sounds.assertHeard(id, tier.volume(), tier.pitch());
		String shown = context.computeOnClient(ServerPlayClientGameTest::actionBar);
		String expected = Component.translatable(tier.messageKey()).getString();
		if (!shown.equals(expected)) {
			throw new AssertionError("the action bar says '" + shown + "', not '" + expected + "'");
		}
	}

	private static String actionBar(Minecraft client) {
		try {
			Field message = Hud.class.getDeclaredField("overlayMessageString");
			message.setAccessible(true);
			Component text = (Component) message.get(client.gui.hud);
			return text == null ? "" : text.getString();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void noteBlocks(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, SoundLog sounds) {
		BlockPos onAmethyst = new BlockPos(200, Y + 1, 205);
		BlockPos onGrass = new BlockPos(203, Y, 205);
		server.runCommand("setblock %d %d %d amethyst_block".formatted(onAmethyst.getX(), onAmethyst.getY() - 1, onAmethyst.getZ()));
		server.runCommand("setblock %d %d %d note_block".formatted(onAmethyst.getX(), onAmethyst.getY(), onAmethyst.getZ()));
		server.runCommand("setblock %d %d %d note_block".formatted(onGrass.getX(), onGrass.getY(), onGrass.getZ()));
		server.runCommand("item replace entity @a hotbar.%d with minecraft:air".formatted(HOTBAR_SPARE));
		view(context, connection, server, "201.5 %d 207.5".formatted(Y));
		select(context, HOTBAR_SPARE);

		assertTuningPlays(context, connection, sounds, onAmethyst, "patinachime:note_block.amethyst");
		assertTuningPlays(context, connection, sounds, onGrass, "minecraft:block.note_block.harp");
	}

	private static void assertTuningPlays(ClientGameTestContext context, TestServerConnection connection, SoundLog sounds, BlockPos pos, String id) {
		sounds.clear();
		context.getInput().lookAt(pos);
		context.waitTick();
		use(context, connection);
		context.waitFor(client -> sounds.find(id).isPresent());
		sounds.assertHeard(id, NOTE_VOLUME, NoteBlock.getPitchFromNote(1));
	}

	private static void enchanting(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		if (enchantingTableIsReplaced()) {
			assertReplacedTableTakesLapis(context, server, connection);
			return;
		}

		openEnchantingTable(context, server, connection);
		move(context, connection, MENU_SWORD, MENU_ITEM);

		for (int offer = 0; offer < 3; offer++) {
			assertOnlyHalfTheSeedArrived(context, server, connection);
			assertWhatShardsRevealIsWhatYouGet(context, connection, offer);
		}

		close(context);
	}

	// A menu's data travels as shorts, so the vanilla slot only has half the seed. What the offers reveal needs the rest.
	private static void assertOnlyHalfTheSeedArrived(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		int onServer = server.computeOnServer(ignored -> connection.getServerPlayer().getEnchantmentSeed());
		int inMenu = context.computeOnClient(client -> ((EnchantmentMenu) client.player.containerMenu).getEnchantmentSeed());
		if (inMenu != (short) onServer) {
			throw new AssertionError("the server's seed is " + onServer + " and the menu holds " + inMenu);
		}
	}

	private static void rejoining(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		assertRealSocket(context);
		view(context, connection, server, "40.5 %d 2.4".formatted(Y));
		assertBlock(context, server, connection, LEFT_BEHIND, "patinachime:waxed_weathered_copper_hopper");
		open(context, connection, LEFT_BEHIND, HopperScreen.class);
		ItemStack slot = context.computeOnClient(client -> client.player.containerMenu.getSlot(1).getItem().copy());
		if (!slot.is(Items.EMERALD) || slot.getCount() != 9) {
			throw new AssertionError("after rejoining the menu shows " + slot);
		}

		context.takeScreenshot("server-hopper-after-rejoining");
		close(context);
	}

	private static void savingAndReopening(ClientGameTestContext context) {
		BlockPos waxed = new BlockPos(0, Y, 0);
		BlockPos idle = new BlockPos(3, Y, 0);
		BlockPos chest = new BlockPos(6, Y, 0);
		BlockPos running = chest.above();

		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			server.runCommand("gamerule random_tick_speed 0");
			server.runCommand("setblock %d %d %d patinachime:waxed_oxidized_copper_hopper[facing=north]".formatted(waxed.getX(), waxed.getY(), waxed.getZ()));
			server.runCommand("item replace block %d %d %d container.2 with minecraft:diamond 5".formatted(waxed.getX(), waxed.getY(), waxed.getZ()));
			server.runCommand("setblock %d %d %d patinachime:weathered_copper_hopper[facing=east]".formatted(idle.getX(), idle.getY(), idle.getZ()));
			server.runCommand("item replace block %d %d %d container.0 with minecraft:gold_ingot 7".formatted(idle.getX(), idle.getY(), idle.getZ()));
			server.runCommand("setblock %d %d %d chest".formatted(chest.getX(), chest.getY(), chest.getZ()));
			server.runCommand("setblock %d %d %d patinachime:copper_hopper".formatted(running.getX(), running.getY(), running.getZ()));
			server.runCommand("item replace block %d %d %d container.0 with minecraft:cobblestone 32".formatted(running.getX(), running.getY(), running.getZ()));
			context.waitTicks(30);
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext world = save.open()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			assertSaved(context, server, connection, waxed, "patinachime:waxed_oxidized_copper_hopper", Direction.NORTH, Items.DIAMOND, 5);
			assertSaved(context, server, connection, idle, "patinachime:weathered_copper_hopper", Direction.EAST, Items.GOLD_INGOT, 7);

			int moved = server.computeOnServer(ignored -> containerCount(connection.getServerLevel(), chest, Items.COBBLESTONE));
			int waiting = server.computeOnServer(ignored -> containerCount(connection.getServerLevel(), running, Items.COBBLESTONE));
			if (moved == 0 || moved + waiting != 32) {
				throw new AssertionError("after reopening " + moved + " items are in the chest and " + waiting + " in the hopper, out of 32");
			}

			server.waitFor(ignored -> containerCount(connection.getServerLevel(), chest, Items.COBBLESTONE) >= moved + 2, 100);
		}
	}

	private static void assertSaved(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, BlockPos pos, String block, Direction facing, Item item, int count) {
		assertBlock(context, server, connection, pos, block);
		Direction facingNow = server.computeOnServer(ignored -> connection.getServerLevel().getBlockState(pos).getValue(HopperBlock.FACING));
		int countNow = server.computeOnServer(ignored -> containerCount(connection.getServerLevel(), pos, item));
		if (facingNow != facing || countNow != count) {
			throw new AssertionError(block + " came back facing " + facingNow + " with " + countNow + " " + item);
		}
	}

	private static void give(TestServerContext server, int hotbar, String item) {
		server.runCommand("item replace entity @a hotbar.%d with minecraft:%s".formatted(hotbar, item));
	}

	private static void select(ClientGameTestContext context, int hotbar) {
		context.getInput().pressKey(options -> options.keyHotbarSlots[hotbar]);
		context.waitTick();
	}

	private static void use(ClientGameTestContext context, TestServerConnection connection) {
		context.getInput().pressKey(options -> options.keyUse);
		settle(context, connection);
	}

	// Says so when something stands in the way, which would otherwise look like the block ignoring the click.
	private static void assertAimingAt(ClientGameTestContext context, BlockPos pos) {
		context.runOnClient(client -> {
			if (!(client.hitResult instanceof BlockHitResult block && block.getBlockPos().equals(pos))) {
				throw new AssertionError("aiming at " + (client.hitResult instanceof EntityHitResult entity ? entity.getEntity() : client.hitResult) + " instead of " + pos);
			}
		});
	}

	private static void aimAt(ClientGameTestContext context, Vec3 point) {
		context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, point));
		context.waitTick();
	}

	private static Vec3 topOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
	}

	private static void sneaking(ClientGameTestContext context, Runnable action) {
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		action.run();
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(3);
	}

	private static int menuCount(Minecraft client, int slot) {
		return client.player.containerMenu.getSlot(slot).getItem().getCount();
	}

	private static long gameTime(TestServerContext server) {
		return server.computeOnServer(minecraft -> minecraft.overworld().getGameTime());
	}

	private static int containerCount(ServerLevel level, BlockPos pos, Item item) {
		return ((Container) level.getBlockEntity(pos)).countItem(item);
	}

	private static String blockId(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	private static void assertBlock(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, BlockPos pos, String expected) {
		String onServer = server.computeOnServer(ignored -> blockId(connection.getServerLevel().getBlockState(pos)));
		String onClient = context.computeOnClient(client -> blockId(client.level.getBlockState(pos)));
		if (!onServer.equals(expected) || !onClient.equals(expected)) {
			throw new AssertionError("expected " + expected + " at " + pos + ": the server has " + onServer + " and the client " + onClient);
		}
	}

	private static void assertFacingDown(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		Direction onServer = server.computeOnServer(ignored -> connection.getServerLevel().getBlockState(HOPPER).getValue(HopperBlock.FACING));
		Direction onClient = context.computeOnClient(client -> client.level.getBlockState(HOPPER).getValue(HopperBlock.FACING));
		if (onServer != Direction.DOWN || onClient != Direction.DOWN) {
			throw new AssertionError("the hopper should face down: the server says " + onServer + " and the client " + onClient);
		}
	}

	private static void assertNoScreen(ClientGameTestContext context) {
		boolean open = context.computeOnClient(client -> client.gui.screen() != null);
		if (open) {
			throw new AssertionError("a menu opened instead of the block changing");
		}
	}

	private static void assertHolding(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, Item item, int count) {
		int onServer = server.computeOnServer(ignored -> connection.getServerPlayer().getInventory().countItem(item));
		int onClient = context.computeOnClient(client -> client.player.getInventory().countItem(item));
		if (onServer != count || onClient != count) {
			throw new AssertionError("expected " + count + " " + item + ": the server has " + onServer + " and the client " + onClient);
		}
	}

	// Counts it wherever it went: picked up, lying around, or pulled into a container next to the spot.
	private static void assertAround(TestServerContext server, TestServerConnection connection, BlockPos pos, Item item, int count) {
		int found = server.computeOnServer(ignored -> {
			ServerLevel level = connection.getServerLevel();
			int total = connection.getServerPlayer().getInventory().countItem(item);
			for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8))) {
				if (drop.getItem().is(item)) {
					total += drop.getItem().getCount();
				}
			}

			for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
				if (level.getBlockEntity(near) instanceof Container container) {
					total += container.countItem(item);
				}
			}

			return total;
		});
		if (found != count) {
			throw new AssertionError("expected " + count + " " + item + " around " + pos + " but found " + found);
		}
	}

	private record Heard(String id, float volume, float pitch, String subtitle) {
	}

	private static final class SoundLog implements SoundEventListener {
		private final List<Heard> heard = new CopyOnWriteArrayList<>();

		@Override
		public void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range) {
			Component subtitle = soundEvent.getSubtitle();
			this.heard.add(new Heard(sound.getIdentifier().toString(), sound.getVolume(), sound.getPitch(), subtitle == null ? "" : subtitle.getString()));
		}

		private void clear() {
			this.heard.clear();
		}

		private Optional<Heard> find(String id) {
			return this.heard.stream().filter(entry -> entry.id().equals(id)).findFirst();
		}

		private void assertHeard(String id, float volume, float pitch) {
			Heard entry = this.find(id).orElseThrow(() -> new AssertionError("never heard " + id + " among " + this.heard));
			if (Math.abs(entry.volume() - volume) > 0.001F || Math.abs(entry.pitch() - pitch) > 0.001F) {
				throw new AssertionError("expected volume " + volume + " and pitch " + pitch + " but heard " + entry);
			}

			if (entry.subtitle().isEmpty() || entry.subtitle().startsWith("subtitles.")) {
				throw new AssertionError("no readable subtitle for " + entry);
			}
		}
	}
}
