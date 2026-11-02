package io.github.gr8monk3ys.patinachime.enchanting.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.gr8monk3ys.patinachime.enchanting.SeedHighBitsSlot;
import io.github.gr8monk3ys.patinachime.enchanting.ShardOffers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
	@Shadow
	@Final
	private DataSlot enchantmentSeed;

	@Unique
	private SeedHighBitsSlot patinachime$seedHighBits;

	protected EnchantmentMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void patinachime$syncFullSeed(CallbackInfo ci) {
		this.patinachime$seedHighBits = new SeedHighBitsSlot(this.enchantmentSeed);
		this.addDataSlot(this.patinachime$seedHighBits);
	}

	@ModifyExpressionValue(
		method = "getEnchantmentList",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I")
	)
	private int patinachime$useFullSeed(int syncedSeed) {
		return this.patinachime$seedHighBits.fullSeed(syncedSeed);
	}

	@WrapOperation(
		method = {"clickMenuButton", "getGoldCount"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
	)
	private int patinachime$countInLapisUnits(ItemStack currency, Operation<Integer> original) {
		return original.call(currency) / ShardOffers.itemsPerLapis(currency);
	}

	@WrapOperation(
		method = "lambda$clickMenuButton$0",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V")
	)
	private void patinachime$chargeInItems(ItemStack currency, int lapisCost, LivingEntity owner, Operation<Void> original) {
		original.call(currency, lapisCost * ShardOffers.itemsPerLapis(currency), owner);
	}

	@WrapOperation(
		method = "quickMoveStack",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z")
	)
	private boolean patinachime$routeShardsToCurrencySlot(ItemStack stack, Object lapis, Operation<Boolean> original) {
		return original.call(stack, lapis) || ShardOffers.isShards(stack);
	}
}
