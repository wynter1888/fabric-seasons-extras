package io.github.lucaargolo.seasonsextras;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import io.github.lucaargolo.seasonsextras.item.SeasonalCompendiumItem;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FabricSeasonsExtras implements ModInitializer {

    public static final String MOD_ID = "seasonsextras";
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;

    public static ModIdentifier SEASONAL_COMPENDIUM_ITEM = new ModIdentifier("seasonal_compendium");
    public static ModIdentifier SEND_VALID_BIOMES_S2C = new ModIdentifier("send_valid_biomes_s2c");

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, SEASONAL_COMPENDIUM_ITEM, new SeasonalCompendiumItem(new Item.Settings().group(ItemGroup.TOOLS)));

        Registry.register(Registry.ITEM, new ModIdentifier("season_calendar"), new SeasonCalendarItem((new Item.Settings()).group(ItemGroup.TOOLS)));

        SeasonDetectorBlock seasonDetector = Registry.register(Registry.BLOCK, new ModIdentifier("season_detector"), new SeasonDetectorBlock(FabricBlockSettings.copyOf(Blocks.DAYLIGHT_DETECTOR)));
        SEASON_DETECTOR_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("season_detector"), FabricBlockEntityTypeBuilder.create(seasonDetector::createBlockEntity, seasonDetector).build(null));
        Registry.register(Registry.ITEM, new ModIdentifier("season_detector"), new BlockItem(seasonDetector, new Item.Settings().group(ItemGroup.REDSTONE)));

        GreenhouseGlassBlock greenhouseGlass = Registry.register(Registry.BLOCK, new ModIdentifier("greenhouse_glass"), new GreenhouseGlassBlock(FabricBlockSettings.copyOf(Blocks.GREEN_STAINED_GLASS)));
        GREENHOUSE_GLASS_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("greenhouse_glass"), FabricBlockEntityTypeBuilder.create(greenhouseGlass::createBlockEntity, greenhouseGlass).build(null));
        Registry.register(Registry.ITEM, new ModIdentifier("greenhouse_glass"), new BlockItem(greenhouseGlass, new Item.Settings().group(ItemGroup.DECORATIONS)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendValidBiomes(server, handler.player);
        });
    }

    public static void sendValidBiomes(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        Set<Identifier> validBiomes = new HashSet<>();
        server.getWorlds().forEach(serverWorld -> {
            if(FabricSeasons.CONFIG.isValidInDimension(serverWorld.getRegistryKey())) {
                serverWorld.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes().forEach(entry -> {
                    entry.getKey().ifPresent(key -> validBiomes.add(key.getValue()));
                });
            }
        });
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(validBiomes.size());
        validBiomes.forEach(buf::writeIdentifier);
        if(player != null) {
            ServerPlayNetworking.send(player, SEND_VALID_BIOMES_S2C, buf);
        }else{
            server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, SEND_VALID_BIOMES_S2C, buf));
        }
    }

    //TODO: I don't know what this is but it kind of works
    public static double minecraftToCelsius(float x) {
        double value = 1.02557113*x*x*x*x - 2.5249755*x*x*x + 0.61120004*x*x + 28.51377*x - 4.2984804;
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static double minecraftToFahrenheit(float x) {
        return (minecraftToCelsius(x) * 1.8) + 32;
    }


}
