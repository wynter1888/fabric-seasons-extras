package io.github.lucaargolo.seasonsextras.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.client.screen.AirConditioningScreen;
import io.github.lucaargolo.seasonsextras.mixin.GuiBookEntryAccessor;
import io.github.lucaargolo.seasonsextras.patchouli.FabricSeasonsExtrasPatchouliCompat;
import io.github.lucaargolo.seasonsextras.utils.TooltipRenderer;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.utils.Tickable;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class FabricSeasonsExtrasClient implements ClientModInitializer {

    public static RegistryKey<Biome> multiblockBiomeOverride = null;
    public static Season multiblockSeasonOverride = null;

    public static final HashMap<RegistryKey<World>, Set<RegistryEntry<Biome>>> worldValidBiomes = new HashMap<>();
    public static final HashMap<RegistryKey<World>, HashMap<Identifier, Set<Identifier>>> worldBiomeMultiblocks = new HashMap<>();
    public static HashMap<Identifier, JsonObject> multiblocks = new HashMap<>();

    public static boolean prefersCelsius = true;
    public static final List<Text> testedTooltip = new ArrayList<>();
    public static BlockPos testedPos = null;


    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.currentScreen == null) {
                FabricSeasonsExtrasClient.multiblockBiomeOverride = null;
                FabricSeasonsExtrasClient.multiblockSeasonOverride = null;
            }else{
                if(client.currentScreen instanceof GuiBookEntryAccessor bookEntry) {
                    if(bookEntry.getLeftPage() instanceof Tickable page) {
                        page.tick();
                    }
                    if(bookEntry.getRightPage() instanceof Tickable page) {
                        page.tick();
                    }
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricSeasonsExtras.SEND_TESTED_SEASON_S2C, (client, handler, buf, responseSender) -> {
            BlockPos receivedTestedPos = buf.readBlockPos();
            List<Text> receivedTooltip = new ArrayList<>();
            int size = buf.readInt();
            for(int i = 0; i < size; i++) {
                receivedTooltip.add(buf.readText());
            }
            client.execute(() -> {
                testedPos = receivedTestedPos;
                testedTooltip.clear();
                testedTooltip.addAll(receivedTooltip);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricSeasonsExtras.SEND_VALID_BIOMES_S2C, (client, handler, buf, responseSender) -> {
            HashSet<Identifier> validBiomes = new HashSet<>();
            Identifier worldId = buf.readIdentifier();
            RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, worldId);
            int size = buf.readInt();
            for(int i = 0; i < size; i++) {
               validBiomes.add(buf.readIdentifier());
            }
            client.execute(() -> {
                worldValidBiomes.computeIfPresent(worldKey, (key, list) -> new LinkedHashSet<>());
                validBiomes.stream().sorted(Comparator.comparing(Identifier::getPath)).forEach(biome -> {
                    handler.getRegistryManager().get(Registry.BIOME_KEY).getEntry(RegistryKey.of(Registry.BIOME_KEY, biome)).ifPresent(entry -> {
                        worldValidBiomes.computeIfAbsent(worldKey, (key) -> new LinkedHashSet<>()).add(entry);
                    });
                });
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricSeasonsExtras.SEND_BIOME_MULTIBLOCKS_S2C, (client, handler, buf, responseSender) -> {
            HashMap<Identifier, Set<Identifier>> biomeMultiblocks = new HashMap<>();
            Identifier worldId = buf.readIdentifier();
            RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, worldId);
            int mapSize = buf.readInt();
            for(int m = 0; m < mapSize; m++) {
                HashSet<Identifier> set = new HashSet<>();
                Identifier biomeId = buf.readIdentifier();
                int setSize = buf.readInt();
                for(int s = 0; s < setSize; s++) {
                    set.add(buf.readIdentifier());
                }
                biomeMultiblocks.put(biomeId, set);
            }
            client.execute(() -> {
                worldBiomeMultiblocks.put(worldKey, biomeMultiblocks);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricSeasonsExtras.SEND_MULTIBLOCKS_S2C, (client, handler, buf, responseSender) -> {
            HashMap<Identifier, JsonObject> serverMultiblocks = new HashMap<>();
            int size = buf.readInt();
            for(int m = 0; m < size; m++) {
                serverMultiblocks.put(buf.readIdentifier(), JsonParser.parseString(buf.readString()).getAsJsonObject());
            }
            client.execute(() -> {
                multiblocks = serverMultiblocks;
            });
        });
        ModelPredicateProviderRegistry.register(FabricSeasonsExtras.SEASON_CALENDAR_ITEM, new Identifier("season"), (itemStack, clientWorld, livingEntity, seed) -> {
            Entity entity = livingEntity != null ? livingEntity : itemStack.getHolder();
            if (entity == null) {
                return 0.0F;
            } else {
                if (clientWorld == null && entity.world instanceof ClientWorld) {
                    clientWorld = (ClientWorld) entity.world;
                }
                if(clientWorld == null) {
                    return 0f;
                }else{
                    return FabricSeasons.getCurrentSeason(clientWorld).ordinal()/4f;
                }
            }

        });
        ModelPredicateProviderRegistry.register(FabricSeasonsExtras.SEASON_CALENDAR_ITEM, new Identifier("progress"), (itemStack, clientWorld, livingEntity, seed) -> {
            Entity entity = livingEntity != null ? livingEntity : itemStack.getHolder();
            if (entity == null) {
                return 0.0F;
            } else {
                if (clientWorld == null && entity.world instanceof ClientWorld) {
                    clientWorld = (ClientWorld) entity.world;
                }
                if (clientWorld == null) {
                    return 0f;
                } else {
                    long timeToNextSeason = (FabricSeasons.CONFIG.getSeasonLength() - (clientWorld.getTimeOfDay() - ((clientWorld.getTimeOfDay()/FabricSeasons.CONFIG.getSeasonLength())*FabricSeasons.CONFIG.getSeasonLength()) )) % FabricSeasons.CONFIG.getSeasonLength();
                    double progressLeft = timeToNextSeason / (double) FabricSeasons.CONFIG.getSeasonLength();
                    return (float) (1.0 - progressLeft);
                }
            }
        });
        HandledScreens.register(FabricSeasonsExtras.AIR_CONDITIONING_SCREEN_HANDLER, AirConditioningScreen::new);
        for (GreenhouseGlassBlock block : FabricSeasonsExtras.GREENHOUSE_GLASS_BLOCKS) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent());
        }
        BlockRenderLayerMap.INSTANCE.putBlock(Registry.BLOCK.get(new ModIdentifier("season_calendar")), RenderLayer.getCutout());
        HudRenderCallback.EVENT.register(((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            TooltipRenderer.render(client, matrixStack, tickDelta);
        }));
        FabricSeasonsExtrasPatchouliCompat.onInitializeClient();
    }

    //I don't know what this is but it kind of works
    private static double minecraftToCelsius(float x) {
        return 1.02557113*x*x*x*x - 2.5249755*x*x*x + 0.61120004*x*x + 28.51377*x - 4.2984804;
    }

    public static String minecraftToCelsius(String string) {
        float x = Float.parseFloat(string);
        return BigDecimal.valueOf(minecraftToCelsius(x)).setScale(1, RoundingMode.HALF_UP).doubleValue()+ "°C";
    }

    public static String minecraftToFahrenheit(String string) {
        float x = Float.parseFloat(string);
        return BigDecimal.valueOf((minecraftToCelsius(x) * 1.8) + 32).setScale(1, RoundingMode.HALF_UP).doubleValue()+ "°F";
    }

}
