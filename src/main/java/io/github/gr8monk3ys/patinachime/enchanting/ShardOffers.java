package io.github.gr8monk3ys.patinachime.enchanting;

import io.github.gr8monk3ys.patinachime.enchanting.mixin.EnchantmentMenuInvoker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public final class ShardOffers {
	public static final String SHARD_COST_KEY = "container.patinachime.enchant.shards";
	private static final String LAPIS_COST_KEY_PREFIX = "container.enchant.lapis.";
	private static final int ITEM_SLOT = 0;
	private static final int CURRENCY_SLOT = 1;
	private static final int SHARDS_PER_LAPIS = 2;

	private ShardOffers() {
	}

	public static boolean isShards(ItemStack currency) {
		return currency.is(Items.AMETHYST_SHARD);
	}

	public static int itemsPerLapis(ItemStack currency) {
		return isShards(currency) ? SHARDS_PER_LAPIS : 1;
	}

	public static List<EnchantmentInstance> reveal(EnchantmentMenu menu, RegistryAccess registries, int offer) {
		ItemStack item = menu.getSlot(ITEM_SLOT).getItem();
		return ((EnchantmentMenuInvoker) menu).patinachime$getEnchantmentList(registries, item, offer, menu.costs[offer]);
	}

	public static List<Component> tooltip(EnchantmentMenu menu, RegistryAccess registries, int offer, List<Component> vanillaLines) {
		if (!isShards(menu.getSlot(CURRENCY_SLOT).getItem())) {
			return vanillaLines;
		}

		List<Component> lines = new ArrayList<>();
		for (EnchantmentInstance enchantment : reveal(menu, registries, offer)) {
			lines.add(Enchantment.getFullname(enchantment.enchantment(), enchantment.level()));
		}

		// Line 0 is vanilla's one-enchantment clue, which the full list replaces.
		for (Component line : vanillaLines.subList(1, vanillaLines.size())) {
			lines.add(isLapisCost(line) ? shardCost(offer, line.getStyle()) : line);
		}
		return lines;
	}

	private static boolean isLapisCost(Component line) {
		return line.getContents() instanceof TranslatableContents contents && contents.getKey().startsWith(LAPIS_COST_KEY_PREFIX);
	}

	private static Component shardCost(int offer, Style style) {
		return Component.translatable(SHARD_COST_KEY, (offer + 1) * SHARDS_PER_LAPIS).withStyle(style);
	}
}
