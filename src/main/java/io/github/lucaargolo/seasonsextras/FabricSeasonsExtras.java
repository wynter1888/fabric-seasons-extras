package io.github.lucaargolo.seasonsextras;

import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.registry.Registry;

public class FabricSeasonsExtras implements ModInitializer {

    public static final String MOD_ID = "seasonsextras";
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new ModIdentifier("season_calendar"), new SeasonCalendarItem((new Item.Settings()).group(ItemGroup.TOOLS)));

        SeasonDetectorBlock seasonDetector = Registry.register(Registry.BLOCK, new ModIdentifier("season_detector"), new SeasonDetectorBlock(FabricBlockSettings.copyOf(Blocks.DAYLIGHT_DETECTOR)));
        SEASON_DETECTOR_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("season_detector"), FabricBlockEntityTypeBuilder.create(seasonDetector::createBlockEntity, seasonDetector).build(null));
        Registry.register(Registry.ITEM, new ModIdentifier("season_detector"), new BlockItem(seasonDetector, new Item.Settings().group(ItemGroup.REDSTONE)));

        GreenhouseGlassBlock greenhouseGlass = Registry.register(Registry.BLOCK, new ModIdentifier("greenhouse_glass"), new GreenhouseGlassBlock(FabricBlockSettings.copyOf(Blocks.GREEN_STAINED_GLASS)));
        GREENHOUSE_GLASS_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("greenhouse_glass"), FabricBlockEntityTypeBuilder.create(greenhouseGlass::createBlockEntity, greenhouseGlass).build(null));
        Registry.register(Registry.ITEM, new ModIdentifier("greenhouse_glass"), new BlockItem(greenhouseGlass, new Item.Settings().group(ItemGroup.DECORATIONS)));
    }


}
