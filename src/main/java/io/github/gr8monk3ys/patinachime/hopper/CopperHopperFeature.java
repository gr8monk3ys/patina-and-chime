package io.github.gr8monk3ys.patinachime.hopper;

import io.github.gr8monk3ys.patinachime.PatinaChime;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class CopperHopperFeature {
	private static final WeatheringCopperCollection<String> IDS = WeatheringCopperCollection.prefixWithState(WeatheringCopperCollection.create("copper_hopper"));

	public static final WeatheringCopperCollection<Block> BLOCKS = WeatheringCopperCollection.registerBlocks(
		IDS, CopperHopperFeature::registerBlock, CopperHopperBlock::new, WeatheringCopperHopperBlock::new, CopperHopperFeature::blockProperties
	);
	public static final WeatheringCopperCollection<Item> ITEMS = WeatheringCopperCollection.registerItems(IDS, BLOCKS, CopperHopperFeature::registerItem);
	public static final BlockEntityType<CopperHopperBlockEntity> BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, PatinaChime.id("copper_hopper"), new BlockEntityType<>(CopperHopperBlockEntity::new, Set.copyOf(BLOCKS.asList()))
	);

	private CopperHopperFeature() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK_TYPE, PatinaChime.id("copper_hopper"), CopperHopperBlock.CODEC);
		Registry.register(BuiltInRegistries.BLOCK_TYPE, PatinaChime.id("weathering_copper_hopper"), WeatheringCopperHopperBlock.CODEC);
		OxidizableBlocksRegistry.registerWeatheringCopperBlocks(BLOCKS);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
			.register(output -> output.insertAfter(Items.HOPPER, ITEMS.asList().toArray(ItemLike[]::new)));
	}

	private static BlockBehaviour.Properties blockProperties(WeatherState weatherState) {
		return BlockBehaviour.Properties.of()
			.mapColor(state -> Blocks.COPPER_BLOCK.weathering().pick(weatherState).defaultMapColor())
			.requiresCorrectToolForDrops()
			.strength(3.0F, 4.8F)
			.sound(SoundType.COPPER)
			.noOcclusion();
	}

	private static Block registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PatinaChime.id(id));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item registerItem(String id, Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PatinaChime.id(id));
		CopperHopperItem item = new CopperHopperItem(
			(CopperHopperBlock) block,
			new Item.Properties().setId(key).useBlockDescriptionPrefix().component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
		);
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}
}
