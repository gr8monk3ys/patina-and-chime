package io.github.gr8monk3ys.patinachime.test;

import static io.github.gr8monk3ys.patinachime.test.ClientActions.*;

import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.ChatVisiblity;

/**
 * Renders what the server-side tests can't see and saves screenshots to look at by hand. It also checks that what a
 * shard reveals is what the player gets, and that every sound and string resolves. Singleplayer hands packets over
 * without writing them, so {@link ServerPlayClientGameTest} checks the same things over a real connection.
 */
public class VisualClientGameTest implements FabricClientGameTest {
	private static final List<String> STAGES = List.of("", "exposed_", "weathered_", "oxidized_");

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		// A disc plays later on, and this window opens on a machine people are using.
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
		context.runOnClient(client -> client.options.chatVisibility().set(ChatVisiblity.HIDDEN));

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule spawn_mobs false");
			removeMobs(server);

			assertSoundsAndStringsResolve(context);
			hoppers(context, server, connection);
			inventory(context, server, connection);
			enchanting(context, server, connection);
			music(context, server, connection);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.takeScreenshot(name);
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
		shot(context, "hoppers-from-the-side");
		view(context, connection, server, "2 %d -2 facing 2 %d 2".formatted(Y + 3, Y));
		shot(context, "hoppers-from-above");
		view(context, connection, server, "-3 %d 2 facing 2 %d 2".formatted(Y + 1, Y));
		shot(context, "hoppers-from-the-end");

		hideHud(context, false);
		view(context, connection, server, "0.5 %d -2 facing 0.5 %d 0.5".formatted(Y + 1, Y));
		open(context, connection, new BlockPos(0, Y, 0), HopperScreen.class);
		shot(context, "hopper-menu");
		close(context);
	}

	private static void inventory(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("gamemode survival @a");
		for (String item : List.of("verdigris 16", "tuning_fork", "copper_hopper", "exposed_copper_hopper", "weathered_copper_hopper", "oxidized_copper_hopper", "waxed_copper_hopper", "waxed_oxidized_copper_hopper")) {
			server.runCommand("give @a patinachime:" + item);
		}

		connection.waitForClientboundPackets();
		context.waitTicks(2);
		shot(context, "hotbar");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		hoverHotbar(context, 0);
		shot(context, "verdigris-tooltip");
		hoverHotbar(context, 1);
		shot(context, "tuning-fork-tooltip");
		hoverHotbar(context, 2);
		shot(context, "hopper-tooltip");

		context.setScreen(() -> null);
	}

	private static void hoverHotbar(ClientGameTestContext context, int slot) {
		hover(context, 8 + 18 * slot + 8, 142 + 8);
	}

	private static void enchanting(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		if (enchantingTableIsReplaced()) {
			assertReplacedTableTakesLapis(context, server, connection);
			return;
		}

		openEnchantingTable(context, server, connection);
		shot(context, "enchanting-empty");

		move(context, connection, MENU_SWORD, MENU_ITEM);
		shot(context, "enchanting-sword-no-currency");

		for (int shards = 1; shards <= 6; shards++) {
			addOneShard(context, connection);
			if (shards == 1 || shards == 3 || shards == 6) {
				for (int offer = 0; offer < 3; offer++) {
					hoverOffer(context, offer);
					shot(context, "enchanting-%d-shards-offer-%d".formatted(shards, offer + 1));
				}
			}
		}

		move(context, connection, MENU_CURRENCY, MENU_SHARDS);
		move(context, connection, MENU_LAPIS, MENU_CURRENCY);
		hoverOffer(context, 2);
		shot(context, "enchanting-lapis-offer-3");
		move(context, connection, MENU_CURRENCY, MENU_LAPIS);

		for (int offer = 0; offer < 3; offer++) {
			assertWhatShardsRevealIsWhatYouGet(context, connection, offer);
		}

		close(context);
	}
}
