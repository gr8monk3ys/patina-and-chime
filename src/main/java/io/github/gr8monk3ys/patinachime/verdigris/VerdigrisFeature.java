package io.github.gr8monk3ys.patinachime.verdigris;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class VerdigrisFeature {
	private static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, PatinaChime.id("verdigris"));
	public static final Item VERDIGRIS = Registry.register(BuiltInRegistries.ITEM, KEY, new VerdigrisItem(new Item.Properties().setId(KEY)));

	private VerdigrisFeature() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.insertAfter(Items.COPPER_INGOT, VERDIGRIS));
		UseBlockCallback.EVENT.register(VerdigrisFeature::onUseBlock);
	}

	// Vanilla runs the clicked block first, so a plain Item.useOn would lose to a door toggling, a chest or hopper
	// opening, or a statue changing pose. Anything without a next stage passes through to that normal behaviour.
	private static InteractionResult onUseBlock(final Player player, final Level level, final InteractionHand hand, final BlockHitResult hitResult) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || !stack.is(VERDIGRIS)) {
			return InteractionResult.PASS;
		}

		return stack.useOn(new UseOnContext(player, hand, hitResult));
	}

	public static void onAxeChangedBlock(final UseOnContext context, final BlockState oldState, final BlockState newState) {
		Player player = context.getPlayer();
		boolean removedOxidation = WeatheringCopper.getPrevious(oldState).filter(newState::equals).isPresent();
		if (removedOxidation && (player == null || !player.hasInfiniteMaterials())) {
			Block.popResourceFromFace(context.getLevel(), context.getClickedPos(), context.getClickedFace(), new ItemStack(VERDIGRIS));
		}
	}
}
