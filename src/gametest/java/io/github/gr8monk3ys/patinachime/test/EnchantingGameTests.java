package io.github.gr8monk3ys.patinachime.test;

import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

public class EnchantingGameTests {
	private static final BlockPos TABLE = new BlockPos(3, 1, 3);
	private static final int ITEM_SLOT = 0;
	private static final int CURRENCY_SLOT = 1;
	private static final int FIRST_HOTBAR_SLOT = 29;
	private static final int SEED_DATA_SLOT = 3;
	private static final int OFFERS = 3;
	private static final int START_LEVEL = 100;
	private static final int[] SEEDS = {0x12345678, -0x6D2B79F5, 0x7FFF0001, 0x00010000, -2, 42, 0x5BD1E995, -0x3C6EF372};
	private static final List<Item> ITEMS = List.of(Items.DIAMOND_SWORD, Items.BOOK, Items.DIAMOND_PICKAXE, Items.BOW, Items.GOLDEN_CHESTPLATE);

	private static ContainerLevelAccess placeTable(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			helper.setBlock(TABLE.offset(offset), Blocks.BOOKSHELF);
		}
		return ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE));
	}

	private static Player playerWithLevels(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.experienceLevel = START_LEVEL;
		return player;
	}

	private static EnchantmentMenu offers(ContainerLevelAccess table, Player player, int seed, Item item, ItemStack currency) {
		EnchantmentMenu menu = new EnchantmentMenu(1, player.getInventory(), table);
		menu.setData(SEED_DATA_SLOT, seed);
		menu.getSlot(CURRENCY_SLOT).set(currency);
		menu.getSlot(ITEM_SLOT).set(new ItemStack(item));
		return menu;
	}

	private static Map<Holder<Enchantment>, Integer> asMap(List<EnchantmentInstance> enchantments) {
		Map<Holder<Enchantment>, Integer> map = new HashMap<>();
		for (EnchantmentInstance enchantment : enchantments) {
			map.put(enchantment.enchantment(), enchantment.level());
		}
		return map;
	}

	private static Map<Holder<Enchantment>, Integer> applied(ItemStack stack) {
		Map<Holder<Enchantment>, Integer> map = new HashMap<>();
		EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().forEach(entry -> map.put(entry.getKey(), entry.getIntValue()));
		return map;
	}

	private static void assertPays(GameTestHelper helper, ContainerLevelAccess table, Item currency, int held, int offer, int left) {
		String label = "offer " + offer + " with " + held + " " + currency;
		Player player = playerWithLevels(helper);
		EnchantmentMenu menu = offers(table, player, SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(currency, held));
		helper.assertTrue(menu.costs[offer] > 0, "no " + label);
		helper.assertTrue(menu.clickMenuButton(player, offer), "refused " + label);
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), left, "items left after " + label);
		helper.assertTrue(menu.getSlot(ITEM_SLOT).getItem().isEnchanted(), "nothing enchanted by " + label);
		helper.assertValueEqual(player.experienceLevel, START_LEVEL - offer - 1, "levels after " + label);
	}

	private static void assertRefused(GameTestHelper helper, ContainerLevelAccess table, Item currency, int held, int offer) {
		String label = "offer " + offer + " with " + held + " " + currency;
		Player player = playerWithLevels(helper);
		EnchantmentMenu menu = offers(table, player, SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(currency, held));
		helper.assertTrue(menu.costs[offer] > 0, "no " + label);
		helper.assertFalse(menu.clickMenuButton(player, offer), "accepted " + label);
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), held, "items left after " + label);
		helper.assertFalse(menu.getSlot(ITEM_SLOT).getItem().isEnchanted(), "enchanted by " + label);
		helper.assertValueEqual(player.experienceLevel, START_LEVEL, "levels after " + label);
	}

	@GameTest
	public void slotAcceptsShardsAndLapisOnly(GameTestHelper helper) {
		Player player = playerWithLevels(helper);
		EnchantmentMenu menu = new EnchantmentMenu(1, player.getInventory(), placeTable(helper));
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).mayPlace(new ItemStack(Items.AMETHYST_SHARD)), "shard rejected");
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).mayPlace(new ItemStack(Items.LAPIS_LAZULI)), "lapis rejected");
		for (Item other : List.of(Items.AMETHYST_BLOCK, Items.DIAMOND, Items.IRON_INGOT, Items.BOOK, Items.DIAMOND_SWORD)) {
			helper.assertFalse(menu.getSlot(CURRENCY_SLOT).mayPlace(new ItemStack(other)), other + " accepted");
		}

		menu.setCarried(new ItemStack(Items.AMETHYST_SHARD, 5));
		menu.clicked(CURRENCY_SLOT, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 5, "shards placed by clicking");

		menu.setCarried(new ItemStack(Items.DIAMOND, 5));
		menu.clicked(CURRENCY_SLOT, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).getItem().is(Items.AMETHYST_SHARD), "diamonds swapped into the slot");
		helper.succeed();
	}

	@GameTest
	public void shiftClickMovesShardsIntoTheSlot(GameTestHelper helper) {
		Player player = playerWithLevels(helper);
		Inventory inventory = player.getInventory();
		EnchantmentMenu menu = new EnchantmentMenu(1, inventory, placeTable(helper));

		inventory.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 9));
		menu.clicked(FIRST_HOTBAR_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).getItem().is(Items.AMETHYST_SHARD), "shards not in the currency slot");
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 9, "shards in the slot");
		helper.assertTrue(menu.getSlot(ITEM_SLOT).getItem().isEmpty(), "a shard went into the item slot");
		helper.assertValueEqual(inventory.countItem(Items.AMETHYST_SHARD), 0, "shards still in the inventory");

		menu.clicked(CURRENCY_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).getItem().isEmpty(), "shards stayed in the slot");
		helper.assertValueEqual(inventory.countItem(Items.AMETHYST_SHARD), 9, "shards back in the inventory");

		inventory.clearContent();
		inventory.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 9));
		menu.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.AMETHYST_SHARD, 60));
		menu.clicked(FIRST_HOTBAR_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 64, "topped-up shards");
		helper.assertValueEqual(inventory.countItem(Items.AMETHYST_SHARD), 5, "shards that did not fit");
		helper.succeed();
	}

	@GameTest
	public void shiftClickKeepsShardsOutWhileLapisIsInTheSlot(GameTestHelper helper) {
		Player player = playerWithLevels(helper);
		Inventory inventory = player.getInventory();
		EnchantmentMenu menu = new EnchantmentMenu(1, inventory, placeTable(helper));

		menu.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 3));
		inventory.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 9));
		menu.clicked(FIRST_HOTBAR_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).getItem().is(Items.LAPIS_LAZULI), "lapis was displaced");
		helper.assertTrue(menu.getSlot(ITEM_SLOT).getItem().isEmpty(), "a shard went into the item slot");
		helper.assertValueEqual(inventory.countItem(Items.AMETHYST_SHARD), 9, "shards in the inventory");

		inventory.setItem(0, new ItemStack(Items.LAPIS_LAZULI, 4));
		menu.clicked(FIRST_HOTBAR_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 7, "lapis still shift-clicks in");
		helper.succeed();
	}

	@GameTest
	public void shardsCostTwoFourSix(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		assertPays(helper, table, Items.AMETHYST_SHARD, 2, 0, 0);
		assertPays(helper, table, Items.AMETHYST_SHARD, 3, 0, 1);
		assertPays(helper, table, Items.AMETHYST_SHARD, 64, 0, 62);
		assertPays(helper, table, Items.AMETHYST_SHARD, 4, 1, 0);
		assertPays(helper, table, Items.AMETHYST_SHARD, 5, 1, 1);
		assertPays(helper, table, Items.AMETHYST_SHARD, 6, 2, 0);
		assertPays(helper, table, Items.AMETHYST_SHARD, 7, 2, 1);
		helper.succeed();
	}

	@GameTest
	public void shardsAreRefusedWhenShort(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 1, 0);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 1, 1);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 2, 1);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 3, 1);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 3, 2);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 4, 2);
		assertRefused(helper, table, Items.AMETHYST_SHARD, 5, 2);
		helper.succeed();
	}

	@GameTest
	public void lapisStillCostsOneTwoThree(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		assertPays(helper, table, Items.LAPIS_LAZULI, 1, 0, 0);
		assertPays(helper, table, Items.LAPIS_LAZULI, 2, 0, 1);
		assertPays(helper, table, Items.LAPIS_LAZULI, 2, 1, 0);
		assertPays(helper, table, Items.LAPIS_LAZULI, 3, 1, 1);
		assertPays(helper, table, Items.LAPIS_LAZULI, 3, 2, 0);
		assertPays(helper, table, Items.LAPIS_LAZULI, 4, 2, 1);
		assertRefused(helper, table, Items.LAPIS_LAZULI, 1, 1);
		assertRefused(helper, table, Items.LAPIS_LAZULI, 1, 2);
		assertRefused(helper, table, Items.LAPIS_LAZULI, 2, 2);
		helper.succeed();
	}

	@GameTest
	public void goldCountDrivesTheEnableCheckInLapisUnits(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		int[][] shardsToUnits = {{1, 0}, {2, 1}, {3, 1}, {4, 2}, {5, 2}, {6, 3}, {7, 3}, {64, 32}};
		for (int[] pair : shardsToUnits) {
			EnchantmentMenu menu = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.AMETHYST_SHARD, pair[0]));
			helper.assertValueEqual(menu.getGoldCount(), pair[1], "gold count for " + pair[0] + " shards");
		}

		EnchantmentMenu lapis = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.LAPIS_LAZULI, 3));
		helper.assertValueEqual(lapis.getGoldCount(), 3, "gold count for 3 lapis");
		EnchantmentMenu empty = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, ItemStack.EMPTY);
		helper.assertValueEqual(empty.getGoldCount(), 0, "gold count for an empty slot");
		helper.succeed();
	}

	@GameTest
	public void creativePlayersPayNothing(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
		EnchantmentMenu menu = offers(table, player, SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.AMETHYST_SHARD, 1));
		helper.assertTrue(menu.clickMenuButton(player, 2), "creative refused with 1 shard");
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 1, "shards after a creative enchant");
		helper.succeed();
	}

	@GameTest
	public void shardsAndLapisGiveIdenticalOffersResultsAndXp(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		for (int seed : SEEDS) {
			for (Item item : ITEMS) {
				for (int offer = 0; offer < OFFERS; offer++) {
					String label = item + " seed " + seed + " offer " + offer;
					Player lapisPlayer = playerWithLevels(helper);
					Player shardPlayer = playerWithLevels(helper);
					EnchantmentMenu lapis = offers(table, lapisPlayer, seed, item, new ItemStack(Items.LAPIS_LAZULI, 3));
					EnchantmentMenu shards = offers(table, shardPlayer, seed, item, new ItemStack(Items.AMETHYST_SHARD, 6));
					helper.assertTrue(Arrays.equals(lapis.costs, shards.costs), "costs differ for " + label);
					helper.assertTrue(Arrays.equals(lapis.enchantClue, shards.enchantClue), "clues differ for " + label);
					helper.assertTrue(Arrays.equals(lapis.levelClue, shards.levelClue), "clue levels differ for " + label);

					helper.assertTrue(lapis.clickMenuButton(lapisPlayer, offer), "lapis refused for " + label);
					helper.assertTrue(shards.clickMenuButton(shardPlayer, offer), "shards refused for " + label);
					helper.assertValueEqual(shardPlayer.experienceLevel, lapisPlayer.experienceLevel, "levels for " + label);
					helper.assertValueEqual(shardPlayer.experienceLevel, START_LEVEL - offer - 1, "levels spent for " + label);
					helper.assertValueEqual(applied(shards.getSlot(ITEM_SLOT).getItem()), applied(lapis.getSlot(ITEM_SLOT).getItem()), "enchantments for " + label);
					helper.assertTrue(shards.getSlot(ITEM_SLOT).getItem().is(lapis.getSlot(ITEM_SLOT).getItem().getItem()), "result item for " + label);
				}
			}
		}
		helper.succeed();
	}

	@GameTest
	public void revealMatchesWhatTheClickApplies(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		var tableEnchantments = registries.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE).orElseThrow();
		boolean sawBookLoseAnEntry = false;
		boolean sawSeveralOnOneOffer = false;

		for (int seed : SEEDS) {
			for (Item item : ITEMS) {
				for (int offer = 0; offer < OFFERS; offer++) {
					String label = item + " seed " + seed + " offer " + offer;
					Player player = playerWithLevels(helper);
					EnchantmentMenu menu = offers(table, player, seed, item, new ItemStack(Items.AMETHYST_SHARD, 6));
					List<EnchantmentInstance> revealed = ShardOffers.reveal(menu, registries, offer);
					helper.assertFalse(revealed.isEmpty(), "nothing revealed for " + label);
					sawSeveralOnOneOffer |= revealed.size() > 1;

					if (item == Items.BOOK) {
						int unfiltered = EnchantmentHelper.selectEnchantment(RandomSource.create(seed + offer), new ItemStack(item), menu.costs[offer], tableEnchantments.stream()).size();
						sawBookLoseAnEntry |= unfiltered == revealed.size() + 1;
					}

					int clue = menu.enchantClue[offer];
					int clueLevel = menu.levelClue[offer];
					var holders = registries.lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
					helper.assertTrue(revealed.stream().anyMatch(e -> holders.getId(e.enchantment()) == clue && e.level() == clueLevel), "clue missing from the reveal for " + label);

					helper.assertTrue(menu.clickMenuButton(player, offer), "refused " + label);
					helper.assertValueEqual(applied(menu.getSlot(ITEM_SLOT).getItem()), asMap(revealed), "enchantments for " + label);
				}
			}
		}

		helper.assertTrue(sawBookLoseAnEntry, "no book case exercised vanilla's random removal");
		helper.assertTrue(sawSeveralOnOneOffer, "no case revealed more than one enchantment");
		helper.succeed();
	}

	/** Feeds a client menu the way the network does: slots then data, with data through the real packet codec, which writes shorts. */
	private record PacketSync(AbstractContainerMenu client) implements ContainerSynchronizer {
		@Override
		public void sendInitialData(AbstractContainerMenu container, List<ItemStack> items, ItemStack carried, int[] data) {
			for (int slot = 0; slot < items.size(); slot++) {
				this.sendSlotChange(container, slot, items.get(slot));
			}
			for (int id = 0; id < data.length; id++) {
				this.sendDataChange(container, id, data[id]);
			}
		}

		@Override
		public void sendSlotChange(AbstractContainerMenu container, int slot, ItemStack stack) {
			this.client.getSlot(slot).set(stack);
		}

		@Override
		public void sendCarriedChange(AbstractContainerMenu container, ItemStack stack) {
		}

		@Override
		public void sendDataChange(AbstractContainerMenu container, int id, int value) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			ClientboundContainerSetDataPacket.STREAM_CODEC.encode(buffer, new ClientboundContainerSetDataPacket(container.containerId, id, value));
			ClientboundContainerSetDataPacket received = ClientboundContainerSetDataPacket.STREAM_CODEC.decode(buffer);
			this.client.setData(received.getId(), received.getValue());
		}

		@Override
		public RemoteSlot createSlot() {
			return new RemoteSlot.Synchronized(component -> 0);
		}
	}

	@GameTest
	public void clientRevealMatchesTheClickAfterPacketSync(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		for (int seed : SEEDS) {
			for (Item item : ITEMS) {
				for (int offer = 0; offer < OFFERS; offer++) {
					String label = item + " seed " + seed + " offer " + offer;
					Player player = playerWithLevels(helper);
					EnchantmentMenu server = new EnchantmentMenu(1, player.getInventory(), table);
					EnchantmentMenu client = new EnchantmentMenu(1, player.getInventory());
					server.setSynchronizer(new PacketSync(client));

					server.setData(SEED_DATA_SLOT, seed);
					server.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.AMETHYST_SHARD, 6));
					server.getSlot(ITEM_SLOT).set(new ItemStack(item));

					helper.assertValueEqual(client.getEnchantmentSeed(), (int) (short) seed, "vanilla's truncated seed for " + label);
					helper.assertTrue(Arrays.equals(server.costs, client.costs), "costs did not sync for " + label);

					List<EnchantmentInstance> revealed = ShardOffers.reveal(client, registries, offer);
					helper.assertTrue(server.clickMenuButton(player, offer), "refused " + label);
					helper.assertValueEqual(applied(server.getSlot(ITEM_SLOT).getItem()), asMap(revealed), "client reveal for " + label);
				}
			}
		}
		helper.succeed();
	}

	@GameTest
	public void clientRevealFollowsEitherHalfOfTheSeedChanging(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		Player player = playerWithLevels(helper);
		EnchantmentMenu server = new EnchantmentMenu(1, player.getInventory(), table);
		EnchantmentMenu client = new EnchantmentMenu(1, player.getInventory());
		server.setSynchronizer(new PacketSync(client));
		server.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.AMETHYST_SHARD, 6));

		int[] seeds = {0x12345678, 0x12345679, 0x12355679, 0x00005679, -1, -0x10000, 0x7FFF8000};
		for (int seed : seeds) {
			server.setData(SEED_DATA_SLOT, seed);
			server.getSlot(ITEM_SLOT).set(new ItemStack(Items.DIAMOND_SWORD));
			for (int offer = 0; offer < OFFERS; offer++) {
				helper.assertValueEqual(asMap(ShardOffers.reveal(client, registries, offer)), asMap(ShardOffers.reveal(server, registries, offer)), "reveal for seed " + seed + " offer " + offer);
			}
		}
		helper.succeed();
	}

	private static List<Component> vanillaAffordableLines(int offer) {
		String lapisKey = offer == 0 ? "container.enchant.lapis.one" : "container.enchant.lapis.many";
		return List.of(
			Component.translatable("container.enchant.clue", "x").withStyle(ChatFormatting.WHITE),
			CommonComponents.EMPTY,
			Component.translatable(lapisKey, offer + 1).withStyle(ChatFormatting.RED),
			Component.translatable("container.enchant.level.many", offer + 1).withStyle(ChatFormatting.GRAY)
		);
	}

	private static String translationKey(Component line) {
		return line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	@GameTest
	public void tooltipListsEveryEnchantmentAndTheShardCost(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		for (int offer = 0; offer < OFFERS; offer++) {
			EnchantmentMenu menu = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.AMETHYST_SHARD, 6));
			List<EnchantmentInstance> revealed = ShardOffers.reveal(menu, registries, offer);
			List<Component> vanillaLines = vanillaAffordableLines(offer);
			List<Component> lines = ShardOffers.tooltip(menu, registries, offer, vanillaLines);

			helper.assertValueEqual(lines.size(), revealed.size() + 3, "tooltip lines for offer " + offer);
			for (int i = 0; i < revealed.size(); i++) {
				helper.assertValueEqual(lines.get(i), Enchantment.getFullname(revealed.get(i).enchantment(), revealed.get(i).level()), "enchantment line " + i + " for offer " + offer);
			}

			Component cost = lines.get(revealed.size() + 1);
			helper.assertValueEqual(translationKey(cost), "container.patinachime.enchant.shards", "cost line key for offer " + offer);
			Object[] args = ((TranslatableContents) cost.getContents()).getArgs();
			helper.assertValueEqual(args[0], (offer + 1) * 2, "shard cost for offer " + offer);
			helper.assertValueEqual(cost.getStyle(), vanillaLines.get(2).getStyle(), "cost line style for offer " + offer);
			helper.assertValueEqual(lines.get(revealed.size()), vanillaLines.get(1), "spacer for offer " + offer);
			helper.assertValueEqual(lines.get(revealed.size() + 2), vanillaLines.get(3), "level line for offer " + offer);
		}
		helper.succeed();
	}

	@GameTest
	public void tooltipKeepsVanillasOtherLayouts(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		EnchantmentMenu menu = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.AMETHYST_SHARD, 6));
		int revealed = ShardOffers.reveal(menu, registries, 0).size();

		List<Component> creative = List.of(Component.literal("clue"));
		helper.assertValueEqual(ShardOffers.tooltip(menu, registries, 0, creative).size(), revealed, "creative tooltip lines");

		Component requirement = Component.translatable("container.enchant.level.requirement", 30);
		List<Component> tooFewLevels = List.of(Component.literal("clue"), CommonComponents.EMPTY, requirement);
		List<Component> lines = ShardOffers.tooltip(menu, registries, 0, tooFewLevels);
		helper.assertValueEqual(lines.size(), revealed + 2, "too-few-levels tooltip lines");
		helper.assertValueEqual(lines.getLast(), requirement, "level requirement line");
		helper.succeed();
	}

	@GameTest
	public void lapisTooltipIsUntouched(GameTestHelper helper) {
		ContainerLevelAccess table = placeTable(helper);
		RegistryAccess registries = helper.getLevel().registryAccess();
		EnchantmentMenu menu = offers(table, playerWithLevels(helper), SEEDS[0], Items.DIAMOND_SWORD, new ItemStack(Items.LAPIS_LAZULI, 3));
		List<Component> vanillaLines = vanillaAffordableLines(1);
		helper.assertTrue(ShardOffers.tooltip(menu, registries, 1, vanillaLines) == vanillaLines, "lapis tooltip was rebuilt");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void closingTheMenuReturnsShards(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		EnchantmentMenu menu = new EnchantmentMenu(1, player.getInventory(), placeTable(helper));
		menu.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.AMETHYST_SHARD, 7));

		menu.removed(player);

		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).getItem().isEmpty(), "shards stayed in the slot");
		helper.assertValueEqual(player.getInventory().countItem(Items.AMETHYST_SHARD), 7, "shards returned");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	/**
	 * Easy Magic swaps in its own subclass with its own slots and quick-move rules, so the shard feature is off there.
	 * This pins that lapis still works and that shards never reach the currency slot. Reflection, since Easy Magic is
	 * only on the runtime classpath of -Pcompat=easymagic runs.
	 */
	@GameTest
	@SuppressWarnings("removal")
	public void easyMagicMenuKeepsLapisAndStaysShardFree(GameTestHelper helper) throws ReflectiveOperationException {
		if (!FabricLoader.getInstance().isModLoaded("easymagic")) {
			helper.succeed();
			return;
		}

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.experienceLevel = START_LEVEL;
		EnchantmentMenu menu = (EnchantmentMenu) Class.forName("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu")
			.getConstructor(int.class, Inventory.class, Container.class, ContainerLevelAccess.class)
			.newInstance(1, player.getInventory(), new SimpleContainer(3), placeTable(helper));

		helper.assertFalse(menu.getSlot(CURRENCY_SLOT).mayPlace(new ItemStack(Items.AMETHYST_SHARD)), "Easy Magic's currency slot took a shard");
		helper.assertTrue(menu.getSlot(CURRENCY_SLOT).mayPlace(new ItemStack(Items.LAPIS_LAZULI)), "Easy Magic's currency slot refused lapis");

		player.getInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD, 9));
		menu.clicked(FIRST_HOTBAR_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertFalse(menu.getSlot(CURRENCY_SLOT).getItem().is(Items.AMETHYST_SHARD), "a shard reached Easy Magic's currency slot");

		menu.getSlot(ITEM_SLOT).set(new ItemStack(Items.DIAMOND_SWORD));
		menu.getSlot(CURRENCY_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 3));
		menu.broadcastChanges();
		helper.assertTrue(menu.costs[1] > 0, "Easy Magic made no offer");
		helper.assertValueEqual(menu.getGoldCount(), 3, "gold count for 3 lapis on Easy Magic");
		helper.assertTrue(menu.clickMenuButton(player, 1), "Easy Magic refused lapis");
		helper.assertValueEqual(menu.getSlot(CURRENCY_SLOT).getItem().getCount(), 1, "lapis left on Easy Magic");
		helper.assertValueEqual(player.experienceLevel, START_LEVEL - 2, "levels spent on Easy Magic");
		helper.assertTrue(menu.getSlot(ITEM_SLOT).getItem().isEnchanted(), "Easy Magic enchanted nothing");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}
}
