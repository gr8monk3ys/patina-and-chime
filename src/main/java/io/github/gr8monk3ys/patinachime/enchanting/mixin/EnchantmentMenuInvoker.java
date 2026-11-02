package io.github.gr8monk3ys.patinachime.enchanting.mixin;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuInvoker {
	@Invoker("getEnchantmentList")
	List<EnchantmentInstance> patinachime$getEnchantmentList(RegistryAccess registries, ItemStack item, int offer, int cost);
}
