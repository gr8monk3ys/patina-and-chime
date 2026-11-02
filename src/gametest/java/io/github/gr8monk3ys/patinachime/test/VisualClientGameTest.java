package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Renders what the server-side tests can't see and saves screenshots to look at by hand. It also checks over a
 * real connection that what a shard reveals is what the player gets, and that every sound and string resolves.
 */
public class VisualClientGameTest implements FabricClientGameTest {
	private static final int Y = -60;
	private static final int MENU_ITEM = 0;
	private static final int MENU_CURRENCY = 1;
	// Enchanting table menu: 2-28 are the main inventory, 29-37 the hotbar.
	private static final int MENU_SWORD = 29;
	private static final int MENU_SHARDS = 30;
	private static final int MENU_LAPIS = 31;
	private static final int MENU_MORE_SWORDS = 32;
	private static final int MENU_MAIN = 2;
	private static final int LEFT_CLICK = 0;
	private static final int RIGHT_CLICK = 1;
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 166;
	private static final List<String> STAGES = List.of("", "exposed_", "weathered_", "oxidized_");

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		// A disc plays later on, and this window opens on a machine people are using.
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			assertSoundsAndStringsResolve(context);
			hoppers(context, server, connection);
			inventory(context, server, connection);
			enchanting(context, server, connection);
			music(context, server, connection);
		}
	}

	private static void assertSoundsAndStringsResolve(ClientGameTestContext context) {
		context.runOnClient(client -> {
			for (String id : List.of("tuning_fork.none", "tuning_fork.faint", "tuning_fork.steady", "tuning_fork.strong", "note_block.amethyst")) {
				WeighedSoundEvents events = client.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath("patinachime", id));
				if (events == null) {
					throw new AssertionError("no sound event " + id);
				}

				Sound sound = events.getSound(client.level.getRandom());
				if (client.getResourceManager().getResource(sound.getPath()).isEmpty()) {
					throw new AssertionError(id + " points at a missing file " + sound.getPath());
				}

				requireString("subtitles.patinachime." + id);
			}

			requireString(ShardOffers.SHARD_COST_KEY);
			requireString("container.patinachime.copper_hopper");
			for (String stage : STAGES) {
				requireString("block.patinachime." + stage + "copper_hopper");
				requireString("block.patinachime.waxed_" + stage + "copper_hopper");
			}

			requireString("item.patinachime.verdigris");
			requireString("item.patinachime.tuning_fork");
			requireString("tooltip.patinachime.verdigris");
			requireString("tooltip.patinachime.tuning_fork");
			for (String job : List.of("fresh", "exposed", "weathered", "oxidized", "ages", "waxed")) {
				requireString("tooltip.patinachime.hopper." + job);
			}

			for (String tier : List.of("none", "faint", "steady", "strong")) {
				requireString("message.patinachime.tuning_fork." + tier);
			}
		});
	}

	private static void requireString(String key) {
		if (!Language.getInstance().has(key)) {
			throw new AssertionError("no translation for " + key);
		}
	}

	private static void hoppers(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("gamemode creative @a");
		for (int i = 0; i < STAGES.size(); i++) {
			server.runCommand("setblock %d %d 0 patinachime:%scopper_hopper".formatted(i, Y, STAGES.get(i)));
			server.runCommand("setblock %d %d 2 patinachime:waxed_%scopper_hopper".formatted(i, Y, STAGES.get(i)));
			server.runCommand("setblock %d %d 4 patinachime:%scopper_hopper[facing=east]".formatted(i, Y, STAGES.get(i)));
		}

		server.runCommand("setblock 4 %d 0 hopper".formatted(Y));
		server.runCommand("setblock 4 %d 2 hopper".formatted(Y));
		server.runCommand("setblock 4 %d 4 hopper[facing=east]".formatted(Y));
		server.runCommand("setblock 0 %d 2 redstone_wire".formatted(Y + 1));
		server.runCommand("setblock 1 %d 2 redstone_wire".formatted(Y + 1));
		server.runCommand("item replace block 0 %d 0 container.0 with minecraft:diamond 7".formatted(Y));
		server.runCommand("item replace block 0 %d 0 container.3 with minecraft:copper_ingot 64".formatted(Y));

		fly(context, server, connection);
		hideHud(context, true);
		view(context, connection, server, "2 %d 8 facing 2 %d 2".formatted(Y + 1, Y));
		context.takeScreenshot("hoppers-from-the-side");
		view(context, connection, server, "2 %d -2 facing 2 %d 2".formatted(Y + 3, Y));
		context.takeScreenshot("hoppers-from-above");
		view(context, connection, server, "-3 %d 2 facing 2 %d 2".formatted(Y + 1, Y));
		context.takeScreenshot("hoppers-from-the-end");

		hideHud(context, false);
		view(context, connection, server, "0.5 %d -2 facing 0.5 %d 0.5".formatted(Y + 1, Y));
		open(context, connection, new BlockPos(0, Y, 0), HopperScreen.class);
		context.takeScreenshot("hopper-menu");
		close(context);
	}

	private static void inventory(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("gamemode survival @a");
		for (String item : List.of("verdigris 16", "tuning_fork", "copper_hopper", "exposed_copper_hopper", "weathered_copper_hopper", "oxidized_copper_hopper", "waxed_copper_hopper", "waxed_oxidized_copper_hopper")) {
			server.runCommand("give @a patinachime:" + item);
		}

		connection.waitForClientboundPackets();
		context.waitTicks(2);
		context.takeScreenshot("hotbar");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		hoverHotbar(context, 0);
		context.takeScreenshot("verdigris-tooltip");
		hoverHotbar(context, 1);
		context.takeScreenshot("tuning-fork-tooltip");
		hoverHotbar(context, 2);
		context.takeScreenshot("hopper-tooltip");
		context.setScreen(() -> null);
	}

	private static void hoverHotbar(ClientGameTestContext context, int slot) {
		hover(context, 8 + 18 * slot + 8, 142 + 8);
	}

	private static void enchanting(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("clear @a");
		server.runCommand("give @a diamond_sword");
		server.runCommand("give @a amethyst_shard 64");
		server.runCommand("give @a lapis_lazuli 64");
		for (int i = 0; i < 3; i++) {
			server.runCommand("give @a diamond_sword");
		}

		server.runCommand("experience add @a 200 levels");
		server.runCommand("fill 18 %1$d -2 22 %1$d 2 bookshelf".formatted(Y));
		server.runCommand("fill 19 %1$d -1 21 %1$d 1 air".formatted(Y));
		server.runCommand("setblock 20 %d 0 enchanting_table".formatted(Y));
		view(context, connection, server, "20 %d 1.5 facing 20 %d 0".formatted(Y, Y));
		open(context, connection, new BlockPos(20, Y, 0), EnchantmentScreen.class);
		context.takeScreenshot("enchanting-empty");

		move(context, connection, MENU_SWORD, MENU_ITEM);
		context.takeScreenshot("enchanting-sword-no-currency");

		for (int shards = 1; shards <= 6; shards++) {
			addOneShard(context, connection);
			if (shards == 1 || shards == 3 || shards == 6) {
				for (int offer = 0; offer < 3; offer++) {
					hoverOffer(context, offer);
					context.takeScreenshot("enchanting-%d-shards-offer-%d".formatted(shards, offer + 1));
				}
			}
		}

		move(context, connection, MENU_CURRENCY, MENU_SHARDS);
		move(context, connection, MENU_LAPIS, MENU_CURRENCY);
		hoverOffer(context, 2);
		context.takeScreenshot("enchanting-lapis-offer-3");
		move(context, connection, MENU_CURRENCY, MENU_LAPIS);

		for (int offer = 0; offer < 3; offer++) {
			assertWhatShardsRevealIsWhatYouGet(context, connection, offer);
		}

		close(context);
	}

	private static void assertWhatShardsRevealIsWhatYouGet(ClientGameTestContext context, TestServerConnection connection, int offer) {
		move(context, connection, MENU_SHARDS, MENU_CURRENCY);
		List<EnchantmentInstance> shown = context.computeOnClient(client -> ShardOffers.reveal((EnchantmentMenu) client.player.containerMenu, client.level.registryAccess(), offer));
		if (shown.isEmpty()) {
			throw new AssertionError("offer " + offer + " revealed nothing");
		}

		context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, offer));
		settle(context, connection);
		ItemStack result = context.computeOnClient(client -> client.player.containerMenu.getSlot(MENU_ITEM).getItem().copy());
		ItemEnchantments got = result.getEnchantments();
		boolean same = got.size() == shown.size() && shown.stream().allMatch(instance -> got.getLevel(instance.enchantment()) == instance.level());
		if (!same) {
			throw new AssertionError("offer " + offer + " showed " + shown + " but the item got " + got);
		}

		move(context, connection, MENU_ITEM, MENU_MAIN + offer);
		move(context, connection, MENU_CURRENCY, MENU_SHARDS);
		if (offer < 2) {
			move(context, connection, MENU_MORE_SWORDS + offer, MENU_ITEM);
		}
	}

	private static void music(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("gamemode creative @a");
		for (int x = 28; x <= 32; x += 2) {
			server.runCommand("setblock %d %d 0 budding_amethyst".formatted(x, Y));
			server.runCommand("setblock %d %d 2 budding_amethyst".formatted(x, Y));
		}

		server.runCommand("setblock 30 %d 1 jukebox".formatted(Y));
		server.runCommand("item replace block 30 %d 1 container.0 with minecraft:music_disc_cat".formatted(Y));
		fly(context, server, connection);
		hideHud(context, true);
		view(context, connection, server, "30 %d 6 facing 30 %d 1".formatted(Y + 2, Y));
		server.runCommand("gamerule random_tick_speed 1000");
		context.waitTicks(20);
		context.takeScreenshot("music-growth-particles");
		server.runCommand("gamerule random_tick_speed 3");
		server.runCommand("setblock 30 %d 1 air".formatted(Y));
		hideHud(context, false);
	}

	private static void fly(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runOnServer(ignored -> {
			connection.getServerPlayer().getAbilities().flying = true;
			connection.getServerPlayer().onUpdateAbilities();
		});
		settle(context, connection);
	}

	private static void hideHud(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void view(ClientGameTestContext context, TestServerConnection connection, TestServerContext server, String destination) {
		server.runCommand("tp @a " + destination);
		settle(context, connection);
		connection.waitForChunksRender();
		context.waitTicks(5);
	}

	private static void open(ClientGameTestContext context, TestServerConnection connection, BlockPos pos, Class<? extends Screen> screen) {
		context.getInput().lookAt(pos);
		context.waitTick();
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(screen);
		settle(context, connection);
	}

	private static void close(ClientGameTestContext context) {
		context.runOnClient(client -> client.player.closeContainer());
		context.waitTicks(2);
	}

	// Moves a whole stack, then puts back whatever didn't fit.
	private static void move(ClientGameTestContext context, TestServerConnection connection, int from, int to) {
		click(context, from, LEFT_CLICK);
		click(context, to, LEFT_CLICK);
		boolean holding = context.computeOnClient(client -> !client.player.containerMenu.getCarried().isEmpty());
		if (holding) {
			click(context, from, LEFT_CLICK);
		}

		settle(context, connection);
	}

	private static void addOneShard(ClientGameTestContext context, TestServerConnection connection) {
		click(context, MENU_SHARDS, LEFT_CLICK);
		click(context, MENU_CURRENCY, RIGHT_CLICK);
		click(context, MENU_SHARDS, LEFT_CLICK);
		settle(context, connection);
	}

	private static void click(ClientGameTestContext context, int slot, int button) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, button, ContainerInput.PICKUP, client.player));
	}

	private static void settle(ClientGameTestContext context, TestServerConnection connection) {
		connection.waitForServerboundPackets();
		connection.waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static void hoverOffer(ClientGameTestContext context, int offer) {
		hover(context, 60 + 54, 14 + 19 * offer + 9);
	}

	private static void hover(ClientGameTestContext context, int imageX, int imageY) {
		double[] at = context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			Screen screen = client.gui.screen();
			return new double[] {
				((screen.width - IMAGE_WIDTH) / 2 + imageX) * scale,
				((screen.height - IMAGE_HEIGHT) / 2 + imageY) * scale
			};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
	}
}
