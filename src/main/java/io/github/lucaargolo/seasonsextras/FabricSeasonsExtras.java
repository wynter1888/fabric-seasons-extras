package io.github.lucaargolo.seasonsextras;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import io.github.lucaargolo.seasonsextras.item.SeasonalCompendiumItem;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class FabricSeasonsExtras implements ModInitializer {

    public static final String MOD_ID = "seasonsextras";
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;

    public static ModIdentifier SEASONAL_COMPENDIUM_ITEM = new ModIdentifier("seasonal_compendium");
    public static ModIdentifier SEND_VALID_BIOMES_S2C = new ModIdentifier("send_valid_biomes_s2c");


    public static HashMap<Identifier, HashSet<Identifier>> biomeToTrees = new HashMap<>();
    public static HashMap<Identifier, JsonObject> treeToMultiblock = new HashMap<>();

    public static HashMap<BlockPos, BlockState> testingMap = new HashMap<>();
    public static Thread testingThread = null;
    public static boolean testingTree = false;

    private static final char[] VALID = {
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

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
        if(player == null) {
            treeToMultiblock.clear();
        }
        Set<Identifier> validBiomes = new HashSet<>();
        server.getWorlds().forEach(serverWorld -> {
            if(FabricSeasons.CONFIG.isValidInDimension(serverWorld.getRegistryKey())) {
                serverWorld.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes().forEach(entry -> {
                    entry.getKey().ifPresent(key -> {
                        Identifier biomeId = key.getValue();

                        List<ConfiguredFeature<?, ?>> list = entry.value().getGenerationSettings().getFeatures().stream()
                                .flatMap(RegistryEntryList::stream)
                                .map(RegistryEntry::value)
                                .flatMap(PlacedFeature::getDecoratedFeatures)
                                .filter((c) -> c.feature() == Feature.TREE)
                                .collect(ImmutableList.toImmutableList());

                        list.forEach(cf -> {
                            Identifier cfId = server.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).getId(cf);
                            if(cfId != null && cf.feature() instanceof TreeFeature feature && cf.config() instanceof TreeFeatureConfig config) {
                                if (!treeToMultiblock.containsKey(cfId)) {
                                    FabricSeasonsExtras.testingMap.clear();
                                    FabricSeasonsExtras.testingTree = true;
                                    FabricSeasonsExtras.testingThread = Thread.currentThread();
                                    try {
                                        feature.generate(new FeatureContext<>(Optional.of(cf), serverWorld, serverWorld.getChunkManager().getChunkGenerator(), Random.create(0L), new BlockPos(100, 100, 100), config));
                                    } catch (Exception ignored) {
                                        FabricSeasonsExtras.testingMap.clear();
                                    }
                                    FabricSeasonsExtras.testingTree = false;

                                    AtomicInteger index = new AtomicInteger();
                                    HashMap<BlockState, Character> mappings = new HashMap<>();
                                    HashMap<Integer, HashMap<Integer, HashMap<Integer, Character>>> pattern = new HashMap<>();

                                    Optional<BlockBox> optional = BlockBox.encompassPositions(FabricSeasonsExtras.testingMap.keySet());
                                    if (optional.isPresent()) {
                                        BlockBox box = optional.get();
                                        int xOffset = (box.getMinX() - 100 <= 0) ? -100 - (box.getMinX() - 100) : -100;
                                        int yOffset = (box.getMinY() - 100 <= 0) ? -100 - (box.getMinY() - 100) : -100;
                                        int zOffset = (box.getMinZ() - 100 <= 0) ? -100 - (box.getMinZ() - 100) : -100;

                                        AtomicReference<BlockState> centerState = new AtomicReference<>(Blocks.AIR.getDefaultState());
                                        BlockPos centerPos = box.getCenter().add(xOffset, yOffset, zOffset);

                                        FabricSeasonsExtras.testingMap.forEach((pos, state) -> {
                                            if (pos.add(xOffset, yOffset, zOffset).equals(centerPos)) {
                                                centerState.set(state);
                                            }
                                            if (!mappings.containsKey(state) && index.getAndIncrement() < VALID.length) {
                                                mappings.put(state, VALID[index.get() - 1]);
                                            }
                                            Character mapping = mappings.get(state);
                                            if (mapping != null) {
                                                int x = pos.getX() + xOffset;
                                                int y = pos.getY() + yOffset;
                                                int z = pos.getZ() + zOffset;
                                                pattern.computeIfAbsent(y, i -> new HashMap<>()).computeIfAbsent(x, i -> new HashMap<>()).put(z, mapping);
                                            }
                                        });

                                        String[][] realPattern = new String[box.getBlockCountY()][box.getBlockCountX()];
                                        Random random = Random.create(1337L);
                                        for (int i = 0; i < box.getBlockCountY(); i++) {
                                            int y = box.getBlockCountY() - 1 - i;
                                            for (int x = 0; x < box.getBlockCountX(); x++) {
                                                realPattern[y][x] = "";
                                                for (int z = 0; z < box.getBlockCountZ(); z++) {
                                                    char d = ' ';
                                                    if (x != 0 && x != box.getBlockCountX() - 1 && z != 0 && z != box.getBlockCountZ() - 1) {
                                                        if (y == (box.getBlockCountY() - 1)) {
                                                            d = '1';
                                                        } else if (y == (box.getBlockCountY() - 2) && random.nextInt(4) == 2) {
                                                            d = '2';
                                                        }
                                                    }
                                                    if (centerPos.getX() == x && centerPos.getY() == i && centerPos.getZ() == z) {
                                                        realPattern[y][x] += '0';
                                                    } else {
                                                        realPattern[y][x] += pattern.getOrDefault(i, new HashMap<>()).getOrDefault(x, new HashMap<>()).getOrDefault(z, d);
                                                    }
                                                }
                                            }
                                        }

                                        JsonObject multiblock = new JsonObject();
                                        JsonObject m = new JsonObject();
                                        mappings.forEach((s, c) -> m.addProperty(c.toString(), s.toString().replace("Block{", "").replace("}", "")));
                                        m.addProperty("0", centerState.get().toString().replace("Block{", "").replace("}", ""));
                                        m.addProperty("1", Blocks.GRASS_BLOCK.getDefaultState().toString().replace("Block{", "").replace("}", ""));
                                        m.addProperty("2", Blocks.GRASS.getDefaultState().toString().replace("Block{", "").replace("}", ""));
                                        multiblock.add("mapping", m);
                                        multiblock.add("pattern", new Gson().toJsonTree(realPattern));
                                        treeToMultiblock.put(cfId, multiblock);
                                    }
                                }
                                biomeToTrees.computeIfAbsent(biomeId, b -> new HashSet<>()).add(cfId);
                            }
                        });

                        validBiomes.add(biomeId);
                    });
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
