package io.github.lucaargolo.seasonsextras;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonCalendarBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonCalendarBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import io.github.lucaargolo.seasonsextras.item.SeasonalCompendiumItem;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.patchouli.PatchouliMultiblockCreator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
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

    //Block Entities
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<SeasonCalendarBlockEntity> SEASON_CALENDAR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;

    //Blocks
    public static SeasonCalendarBlock SEASON_CALENDAR_BLOCK;

    //Items
    public static ModIdentifier SEASONAL_COMPENDIUM_ITEM_ID = new ModIdentifier("seasonal_compendium");
    public static Item SEASON_CALENDAR_ITEM;

    //Packets
    public static ModIdentifier SEND_VALID_BIOMES_S2C = new ModIdentifier("send_valid_biomes_s2c");
    public static ModIdentifier SEND_BIOME_MULTIBLOCKS_S2C = new ModIdentifier("send_biome_multiblocks_s2c");
    public static ModIdentifier SEND_MULTIBLOCKS_S2C = new ModIdentifier("send_multiblocks_s2c");



    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, SEASONAL_COMPENDIUM_ITEM_ID, new SeasonalCompendiumItem(new Item.Settings().group(ItemGroup.TOOLS)));

        SEASON_CALENDAR_BLOCK = Registry.register(Registry.BLOCK, new ModIdentifier("season_calendar"), new SeasonCalendarBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));
        SEASON_CALENDAR_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ModIdentifier("season_calendar"), FabricBlockEntityTypeBuilder.create(SEASON_CALENDAR_BLOCK::createBlockEntity, SEASON_CALENDAR_BLOCK).build(null));
        SEASON_CALENDAR_ITEM = Registry.register(Registry.ITEM, new ModIdentifier("season_calendar"), new SeasonCalendarItem(SEASON_CALENDAR_BLOCK, (new Item.Settings()).group(ItemGroup.TOOLS)));

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
