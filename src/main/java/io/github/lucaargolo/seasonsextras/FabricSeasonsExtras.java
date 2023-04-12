package io.github.lucaargolo.seasonsextras;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonCalendarBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity.Conditioning;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonCalendarBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.*;
import io.github.lucaargolo.seasonsextras.patchouli.PatchouliMultiblockCreator;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.TreeFeature;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FabricSeasonsExtras implements ModInitializer {

    private static final HashMap<Identifier, JsonObject> multiblockCache = new HashMap<>();
    public static final String MOD_ID = "seasonsextras";

    //TODO: Add patchouli entries for blocks/items
    //TODO: Add missing advancements for recipes

    //Block Entities
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<SeasonCalendarBlockEntity> SEASON_CALENDAR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;
    public static BlockEntityType<AirConditioningBlockEntity> AIR_CONDITIONING_TYPE = null;

    //Blocks
    public static SeasonCalendarBlock SEASON_CALENDAR_BLOCK;
    public static GreenhouseGlassBlock[] GREENHOUSE_GLASS_BLOCKS = new GreenhouseGlassBlock[17];

    //Items
    public static ModIdentifier SEASONAL_COMPENDIUM_ITEM_ID = new ModIdentifier("seasonal_compendium");
    public static Item SEASON_CALENDAR_ITEM;

    //Screen Handlers
    public static ScreenHandlerType<AirConditioningScreenHandler> AIR_CONDITIONING_SCREEN_HANDLER;

    //Packets
    public static ModIdentifier SEND_VALID_BIOMES_S2C = new ModIdentifier("send_valid_biomes_s2c");
    public static ModIdentifier SEND_BIOME_MULTIBLOCKS_S2C = new ModIdentifier("send_biome_multiblocks_s2c");
    public static ModIdentifier SEND_MULTIBLOCKS_S2C = new ModIdentifier("send_multiblocks_s2c");
    public static ModIdentifier SEND_TESTED_SEASON_S2C = new ModIdentifier("send_tested_season_s2c");
    public static ModIdentifier SEND_MODULE_PRESS_C2S = new ModIdentifier("send_module_press_c2s");

    public static final ItemGroup CREATIVE_TAB = FabricItemGroupBuilder
            .create(new ModIdentifier("creative_tab"))
            .icon(() -> SEASON_CALENDAR_ITEM.getDefaultStack())
            .build();


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onInitialize() {

        for (DyeColor value : DyeColor.values()) {
            GreenhouseGlassBlock greenhouseGlass = Registry.register(Registry.BLOCK, new ModIdentifier(value.getName()+"_greenhouse_glass"), new GreenhouseGlassBlock(false, FabricBlockSettings.copyOf(Blocks.GREEN_STAINED_GLASS)));
            Registry.register(Registry.ITEM, new ModIdentifier(value.getName()+"_greenhouse_glass"), new GreenHouseGlassItem(greenhouseGlass, new Item.Settings().group(CREATIVE_TAB)));
            GREENHOUSE_GLASS_BLOCKS[value.ordinal()] = greenhouseGlass;
        }
        GreenhouseGlassBlock tintedGreenhouseGlass = Registry.register(Registry.BLOCK, new ModIdentifier("tinted_greenhouse_glass"), new GreenhouseGlassBlock(true, FabricBlockSettings.copyOf(Blocks.TINTED_GLASS)));
        Registry.register(Registry.ITEM, new ModIdentifier("tinted_greenhouse_glass"), new GreenHouseGlassItem(tintedGreenhouseGlass, new Item.Settings().group(CREATIVE_TAB)));
        GREENHOUSE_GLASS_BLOCKS[16] = tintedGreenhouseGlass;
        GREENHOUSE_GLASS_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("greenhouse_glass"), FabricBlockEntityTypeBuilder.create(GreenhouseGlassBlockEntity::new, GREENHOUSE_GLASS_BLOCKS).build(null));

        AirConditioningBlock heaterBlock = Registry.register(Registry.BLOCK, new ModIdentifier("heater"), new AirConditioningBlock(Conditioning.HEATER, FabricBlockSettings.copyOf(Blocks.COBBLESTONE).luminance(state -> state.get(AirConditioningBlock.LEVEL) * 5)));
        Registry.register(Registry.ITEM, new ModIdentifier("heater"), new AirConditioningItem(heaterBlock, new Item.Settings().group(CREATIVE_TAB)));
        AirConditioningBlock chillerBlock = Registry.register(Registry.BLOCK, new ModIdentifier("chiller"), new AirConditioningBlock(Conditioning.CHILLER, FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).luminance(state -> state.get(AirConditioningBlock.LEVEL) * 5)));
        Registry.register(Registry.ITEM, new ModIdentifier("chiller"), new AirConditioningItem(chillerBlock, new Item.Settings().group(CREATIVE_TAB)));
        AIR_CONDITIONING_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("air_conditioning"), FabricBlockEntityTypeBuilder.create(AirConditioningBlockEntity::new, heaterBlock, chillerBlock).build(null));
        AIR_CONDITIONING_SCREEN_HANDLER = Registry.register(Registry.SCREEN_HANDLER, new ModIdentifier("air_conditioning_screen"), new ExtendedScreenHandlerType<>((syncId, playerInventory, buf) -> {
            return new AirConditioningScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerInventory.player.world, buf.readBlockPos()), buf.readRegistryValue(Registry.BLOCK));
        }));

        SeasonDetectorBlock seasonDetector = Registry.register(Registry.BLOCK, new ModIdentifier("season_detector"), new SeasonDetectorBlock(FabricBlockSettings.copyOf(Blocks.DAYLIGHT_DETECTOR)));
        SEASON_DETECTOR_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("season_detector"), FabricBlockEntityTypeBuilder.create(seasonDetector::createBlockEntity, seasonDetector).build(null));
        Registry.register(Registry.ITEM, new ModIdentifier("season_detector"), new SeasonDetectorItem(seasonDetector, new Item.Settings().group(CREATIVE_TAB)));

        SEASON_CALENDAR_BLOCK = Registry.register(Registry.BLOCK, new ModIdentifier("season_calendar"), new SeasonCalendarBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));
        SEASON_CALENDAR_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("season_calendar"), FabricBlockEntityTypeBuilder.create(SEASON_CALENDAR_BLOCK::createBlockEntity, SEASON_CALENDAR_BLOCK).build(null));
        SEASON_CALENDAR_ITEM = Registry.register(Registry.ITEM, new ModIdentifier("season_calendar"), new SeasonCalendarItem(SEASON_CALENDAR_BLOCK, (new Item.Settings()).group(CREATIVE_TAB)));

        Registry.register(Registry.ITEM, SEASONAL_COMPENDIUM_ITEM_ID, new SeasonalCompendiumItem(new Item.Settings().group(CREATIVE_TAB)));
        Registry.register(Registry.ITEM, new ModIdentifier("crop_season_tester"), new CropSeasonTesterItem(new Item.Settings().group(CREATIVE_TAB)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendValidBiomes(server, handler.player);
        });

        ServerPlayNetworking.registerGlobalReceiver(SEND_MODULE_PRESS_C2S, (server, player, handler, buf, sender) -> {
            int button = buf.readInt();
            server.execute(() -> {
                if(player.currentScreenHandler instanceof AirConditioningScreenHandler screenHandler) {
                    screenHandler.cycleButton(button);
                }
            });
        });

        ItemStorage.SIDED.registerForBlockEntity((entity, direction) -> InventoryStorage.of(entity.getInputInventory(), direction), AIR_CONDITIONING_TYPE);
        ItemStorage.SIDED.registerForBlockEntity((entity, direction) -> FilteringStorage.extractOnlyOf(InventoryStorage.of(entity.getModuleInventory(), direction)), AIR_CONDITIONING_TYPE);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void sendValidBiomes(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        boolean generateMultiblocks = player == null;
        if(generateMultiblocks) {
            multiblockCache.clear();
        }
        server.getWorlds().forEach(serverWorld -> {
            if(FabricSeasons.CONFIG.isValidInDimension(serverWorld.getRegistryKey())) {
                Set<RegistryEntry<Biome>> validBiomes = new HashSet<>();
                serverWorld.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes().forEach(entry -> {
                    entry.getKey().ifPresent(key -> validBiomes.add(entry));
                });
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeIdentifier(serverWorld.getRegistryKey().getValue());
                buf.writeInt(validBiomes.size());
                validBiomes.stream().map(r -> r.getKey().get().getValue()).forEach(buf::writeIdentifier);
                if(player != null) {
                    ServerPlayNetworking.send(player, SEND_VALID_BIOMES_S2C, buf);
                }else{
                    server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, SEND_VALID_BIOMES_S2C, buf));
                }
                sendBiomeMultiblocks(server, player, serverWorld, validBiomes);
            }
        });
        sendMultiblocks(server, player);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static void sendBiomeMultiblocks(MinecraftServer server, @Nullable ServerPlayerEntity player, ServerWorld serverWorld, Set<RegistryEntry<Biome>> validBiomes) {
        Identifier worldId = serverWorld.getRegistryKey().getValue();
        HashMap<Identifier, HashSet<Identifier>> biomeToMultiblocks = new HashMap<>();
        validBiomes.forEach(entry -> {
            Identifier biomeId = entry.getKey().get().getValue();
            List<ConfiguredFeature<?, ?>> validFeatures = entry.value().getGenerationSettings().getFeatures().stream()
                    .flatMap(RegistryEntryList::stream)
                    .map(RegistryEntry::value)
                    .flatMap(PlacedFeature::getDecoratedFeatures)
                    .filter((c) -> c.feature() instanceof TreeFeature)
                    .collect(ImmutableList.toImmutableList());

            for(ConfiguredFeature<?, ?> cf : validFeatures) {
                Identifier cfId = server.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).getId(cf);
                if(cfId != null) {
                    if (!multiblockCache.containsKey(cfId)) {
                        PatchouliMultiblockCreator creator = new PatchouliMultiblockCreator(Blocks.GRASS_BLOCK.getDefaultState(), Blocks.GRASS.getDefaultState(), new BlockPos(-100, -100, -100), () -> {
                            cf.generate(serverWorld, serverWorld.getChunkManager().getChunkGenerator(), Random.create(0L), new BlockPos(100, 100, 100));
                        });
                        Optional<JsonObject> optional = creator.getMultiblock((set) -> {
                            boolean foundLeave = false;
                            boolean foundLog = false;
                            Iterator<BlockState> iterator = set.iterator();
                            while(iterator.hasNext() && (!foundLeave || !foundLog)) {
                                BlockState state = iterator.next();
                                if(state.isIn(BlockTags.LEAVES)) {
                                    foundLeave = true;
                                }
                                if(state.isIn(BlockTags.LOGS)) {
                                    foundLog = true;
                                }
                            }
                            return foundLeave && foundLog;
                        });
                        optional.ifPresent((o) -> {
                            multiblockCache.put(cfId, o);
                            biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>()).add(cfId);
                        });
                    }else{
                        biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>()).add(cfId);
                    }
                }
            };
            Identifier empty = new ModIdentifier("empty");
            if(multiblockCache.containsKey(empty)) {
                biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>(Collections.singleton(empty)));
            }else{
                PatchouliMultiblockCreator creator = new PatchouliMultiblockCreator(Blocks.SAND.getDefaultState(), Blocks.DEAD_BUSH.getDefaultState(), new BlockPos(0, 0, 0), () -> {});
                JsonObject emptyMultiblock = creator.getMultiblock((set) -> true).get();
                multiblockCache.put(empty, emptyMultiblock);
                biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>(Collections.singleton(empty)));
            }
        });

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(worldId);
        buf.writeInt(biomeToMultiblocks.size());
        biomeToMultiblocks.forEach((identifier, set) -> {
            buf.writeIdentifier(identifier);
            buf.writeInt(set.size());
            set.forEach(buf::writeIdentifier);
        });
        if(player != null) {
            ServerPlayNetworking.send(player, SEND_BIOME_MULTIBLOCKS_S2C, buf);
        }else{
            server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, SEND_BIOME_MULTIBLOCKS_S2C, buf));
        }

    }

    private static void sendMultiblocks(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(multiblockCache.size());
        multiblockCache.forEach((identifier, jsonObject) -> {
            buf.writeIdentifier(identifier);
            buf.writeString(jsonObject.toString());
        });
        if(player != null) {
            ServerPlayNetworking.send(player, SEND_MULTIBLOCKS_S2C, buf);
        }else{
            server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, SEND_MULTIBLOCKS_S2C, buf));
        }
    }


}
