package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.JukeboxBlock;

/** What the client tests do with their hands: getting around, opening menus and moving items. */
final class ClientActions {
	static final int Y = -60;
	static final int LEFT_CLICK = 0;
	static final int RIGHT_CLICK = 1;
	static final int MENU_ITEM = 0;
	static final int MENU_CURRENCY = 1;
	// Enchanting table menu: 2-28 are the main inventory, 29-37 the hotbar.
	static final int MENU_MAIN = 2;
	static final int MENU_SWORD = 29;
	static final int MENU_SHARDS = 30;
	static final int MENU_LAPIS = 31;
	static final int MENU_MORE_SWORDS = 32;
	private static final int COST_WITH_ALL_SHELVES = 30;
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 166;

	private ClientActions() {
	}

	// Easy Magic swaps in its own menu, where shards don't work.
	static boolean enchantingTableIsReplaced() {
		return FabricLoader.getInstance().isModLoaded("easymagic");
	}

	static void openEnchantingTable(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		buildEnchantingTable(context, server, connection);
		open(context, connection, new BlockPos(20, Y, 0), EnchantmentScreen.class);
	}

	static void assertReplacedTableTakesLapis(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		buildEnchantingTable(context, server, connection);
		BlockPos table = new BlockPos(20, Y, 0);
		// A table set by command is still the vanilla block, and Easy Magic's first click only converts it.
		context.getInput().lookAt(table);
		context.waitTick();
		context.getInput().pressKey(options -> options.keyUse);
		settle(context, connection);
		open(context, connection, table, EnchantmentScreen.class);
		context.takeScreenshot("replaced-enchanting-table");

		move(context, connection, MENU_LAPIS, MENU_CURRENCY);
		boolean tookLapis = context.computeOnClient(client -> client.player.containerMenu.getSlot(MENU_CURRENCY).getItem().is(Items.LAPIS_LAZULI))
			&& server.computeOnServer(ignored -> connection.getServerPlayer().containerMenu.getSlot(MENU_CURRENCY).getItem().is(Items.LAPIS_LAZULI));
		if (!tookLapis) {
			throw new AssertionError("the replaced table's menu didn't take lapis");
		}

		close(context);
	}

	private static void buildEnchantingTable(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("clear @a");
		server.runCommand("give @a diamond_sword");
		server.runCommand("give @a amethyst_shard 64");
		server.runCommand("give @a lapis_lazuli 64");
		for (int i = 0; i < 3; i++) {
			server.runCommand("give @a diamond_sword");
		}

		server.runCommand("experience add @a 200 levels");
		// A command fails without a word in a chunk that isn't loaded, so go there first.
		view(context, connection, server, "16 %d 0".formatted(Y));
		server.runCommand("fill 18 %1$d -2 22 %1$d 2 bookshelf".formatted(Y));
		server.runCommand("fill 19 %1$d -1 21 %1$d 1 air".formatted(Y));
		server.runCommand("setblock 20 %d 0 enchanting_table".formatted(Y));
		view(context, connection, server, "20 %d 1.5 facing 20 %d 0".formatted(Y, Y));
	}

	static void assertWhatShardsRevealIsWhatYouGet(ClientGameTestContext context, TestServerConnection connection, int offer) {
		move(context, connection, MENU_SHARDS, MENU_CURRENCY);
		int topCost = context.computeOnClient(client -> ((EnchantmentMenu) client.player.containerMenu).costs[2]);
		if (topCost != COST_WITH_ALL_SHELVES) {
			throw new AssertionError("the top offer costs " + topCost + ", so the table doesn't have its shelves");
		}

		List<EnchantmentInstance> shown = context.computeOnClient(client -> ShardOffers.reveal((EnchantmentMenu) client.player.containerMenu, client.level.registryAccess(), offer));
		if (shown.isEmpty()) {
			throw new AssertionError("offer " + offer + " revealed nothing");
		}

		hoverOffer(context, offer);
		context.getInput().pressMouse(LEFT_CLICK);
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

	static void music(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("gamemode creative @a");
		fly(context, server, connection);
		hideHud(context, true);
		// A command fails without a word in a chunk that isn't loaded, so go there first.
		view(context, connection, server, "30 %d 6 facing 30 %d 1".formatted(Y + 2, Y));
		for (int x = 28; x <= 32; x += 2) {
			server.runCommand("setblock %d %d 0 budding_amethyst".formatted(x, Y));
			server.runCommand("setblock %d %d 2 budding_amethyst".formatted(x, Y));
		}

		server.runCommand("setblock 30 %d 1 jukebox".formatted(Y));
		server.runCommand("item replace block 30 %d 1 container.0 with minecraft:music_disc_cat".formatted(Y));
		boolean playing = server.computeOnServer(ignored -> connection.getServerLevel().getBlockState(new BlockPos(30, Y, 1)).getOptionalValue(JukeboxBlock.HAS_RECORD).orElse(false));
		if (!playing) {
			throw new AssertionError("the jukebox didn't get built, or has no disc");
		}

		server.runCommand("gamerule random_tick_speed 1000");
		context.waitTicks(20);
		context.takeScreenshot("music-growth-particles");
		server.runCommand("gamerule random_tick_speed 3");
		server.runCommand("setblock 30 %d 1 air".formatted(Y));
		hideHud(context, false);
	}

	static void fly(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runOnServer(ignored -> {
			connection.getServerPlayer().getAbilities().flying = true;
			connection.getServerPlayer().onUpdateAbilities();
		});
		settle(context, connection);
	}

	static void hideHud(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	static void view(ClientGameTestContext context, TestServerConnection connection, TestServerContext server, String destination) {
		server.runCommand("tp @a " + destination);
		settle(context, connection);
		connection.waitForChunksRender();
		removeMobs(server);
		context.waitTicks(5);
	}

	// A sheep wandering between the player and a block takes the click meant for that block.
	static void removeMobs(TestServerContext server) {
		server.runCommand("kill @e[type=!player,type=!item]");
	}

	static void open(ClientGameTestContext context, TestServerConnection connection, BlockPos pos, Class<? extends Screen> screen) {
		context.getInput().lookAt(pos);
		context.waitTick();
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(screen);
		settle(context, connection);
	}

	static void close(ClientGameTestContext context) {
		context.runOnClient(client -> client.player.closeContainer());
		context.waitTicks(2);
	}

	// Moves a whole stack, then puts back whatever didn't fit.
	static void move(ClientGameTestContext context, TestServerConnection connection, int from, int to) {
		click(context, from, LEFT_CLICK);
		click(context, to, LEFT_CLICK);
		boolean holding = context.computeOnClient(client -> !client.player.containerMenu.getCarried().isEmpty());
		if (holding) {
			click(context, from, LEFT_CLICK);
		}

		settle(context, connection);
	}

	static void addOneShard(ClientGameTestContext context, TestServerConnection connection) {
		click(context, MENU_SHARDS, LEFT_CLICK);
		click(context, MENU_CURRENCY, RIGHT_CLICK);
		click(context, MENU_SHARDS, LEFT_CLICK);
		settle(context, connection);
	}

	static void click(ClientGameTestContext context, int slot, int button) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, button, ContainerInput.PICKUP, client.player));
	}

	static void settle(ClientGameTestContext context, TestServerConnection connection) {
		connection.waitForServerboundPackets();
		connection.waitForClientboundPackets();
		context.waitTicks(2);
	}

	static void hoverOffer(ClientGameTestContext context, int offer) {
		hover(context, 60 + 54, 14 + 19 * offer + 9);
	}

	static void hover(ClientGameTestContext context, int imageX, int imageY) {
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
