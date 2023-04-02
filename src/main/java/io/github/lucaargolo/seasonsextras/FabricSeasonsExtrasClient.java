package io.github.lucaargolo.seasonsextras;

import com.google.gson.*;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.mixin.GuiBookEntryAccessor;
import io.github.lucaargolo.seasonsextras.patchouli.PageBiomeSearch;
import io.github.lucaargolo.seasonsextras.patchouli.PageSeasonalBiome;
import io.github.lucaargolo.seasonsextras.utils.CalendarTooltipRenderer;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.utils.PatchouliModifications;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.tag.BiomeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import vazkii.patchouli.client.book.ClientBookRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricSeasonsExtrasClient implements ClientModInitializer {

    public static RegistryKey<Biome> multiblockBiomeOverride = null;
    public static Season multiblockSeasonOverride = null;

    public static final HashMap<RegistryKey<World>, Set<RegistryEntry<Biome>>> worldValidBiomes = new HashMap<>();
    public static final HashMap<RegistryKey<World>, HashMap<Identifier, Set<Identifier>>> worldBiomeMultiblocks = new HashMap<>();
    public static HashMap<Identifier, JsonObject> multiblocks = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("biome_search"), PageBiomeSearch.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("seasonal_biome"), PageSeasonalBiome.class);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.currentScreen == null) {
                FabricSeasonsExtrasClient.multiblockBiomeOverride = null;
                FabricSeasonsExtrasClient.multiblockSeasonOverride = null;
            }else{
                if(client.currentScreen instanceof GuiBookEntryAccessor bookEntry) {
                    //TODO: Make TickablePage
                    if(bookEntry.getLeftPage() instanceof PageBiomeSearch page) {
                        page.tick();
                    }
                    if(bookEntry.getRightPage() instanceof PageBiomeSearch page) {
                        page.tick();
                    }
                    if(bookEntry.getLeftPage() instanceof PageSeasonalBiome page) {
                        page.tick();
                    }
                    if(bookEntry.getRightPage() instanceof PageSeasonalBiome page) {
                        page.tick();
                    }
                }
            }
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
        BlockRenderLayerMap.INSTANCE.putBlock(Registry.BLOCK.get(new ModIdentifier("greenhouse_glass")), RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(Registry.BLOCK.get(new ModIdentifier("season_calendar")), RenderLayer.getCutout());
        HudRenderCallback.EVENT.register(((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            CalendarTooltipRenderer.render(client, matrixStack, tickDelta);
        }));
        PatchouliModifications.registerEntry(new ModIdentifier("biomes"), new ModIdentifier("seasonal_biomes"), (pages, index) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if(world != null) {
                RegistryKey<World> worldKey = world.getRegistryKey();
                AtomicInteger offset = new AtomicInteger(0);
                FabricSeasonsExtrasClient.worldValidBiomes.getOrDefault(worldKey, new HashSet<>()).forEach(entry -> {
                    addSeasonalBiomePage(pages, index+offset.getAndAdd(2), worldKey, entry);
                });
            }
        });
    }

    private static void addSeasonalBiomePage(JsonArray pages, int index, RegistryKey<World> worldKey, RegistryEntry<Biome> entry) {

        Biome biome = entry.value();
        Identifier biomeId = entry.getKey().orElse(RegistryKey.of(Registry.BIOME_KEY, new Identifier("plains"))).getValue();

        String biomeName = Text.translatable(biomeId.toTranslationKey("biome")).getString();
        boolean isJungle = entry.isIn(BiomeTags.IS_JUNGLE) || entry.isIn(BiomeTags.HAS_CLOSER_WATER_FOG);
        Pair<Biome.Precipitation, Float> springPair = FabricSeasons.getSeasonWeather(Season.SPRING, isJungle, biome.getPrecipitation(),biome.getTemperature());
        Pair<Biome.Precipitation, Float> summerPair = FabricSeasons.getSeasonWeather(Season.SUMMER, isJungle, biome.getPrecipitation(),biome.getTemperature());
        Pair<Biome.Precipitation, Float> fallPair = FabricSeasons.getSeasonWeather(Season.FALL, isJungle, biome.getPrecipitation(),biome.getTemperature());
        Pair<Biome.Precipitation, Float> winterPair = FabricSeasons.getSeasonWeather(Season.WINTER, isJungle, biome.getPrecipitation(),biome.getTemperature());

        List<Season> rainSeasons = new ArrayList<>();
        if(springPair.getLeft().equals(Biome.Precipitation.RAIN)) {
            rainSeasons.add(Season.SPRING);
        }
        if(summerPair.getLeft().equals(Biome.Precipitation.RAIN)) {
            rainSeasons.add(Season.SUMMER);
        }
        if(fallPair.getLeft().equals(Biome.Precipitation.RAIN)) {
            rainSeasons.add(Season.FALL);
        }
        if(winterPair.getLeft().equals(Biome.Precipitation.RAIN)) {
            rainSeasons.add(Season.WINTER);
        }

        List<Season> snowSeasons = new ArrayList<>();
        if(springPair.getLeft().equals(Biome.Precipitation.SNOW)) {
            snowSeasons.add(Season.SPRING);
        }
        if(summerPair.getLeft().equals(Biome.Precipitation.SNOW)) {
            snowSeasons.add(Season.SUMMER);
        }
        if(fallPair.getLeft().equals(Biome.Precipitation.SNOW)) {
            snowSeasons.add(Season.FALL);
        }
        if(winterPair.getLeft().equals(Biome.Precipitation.SNOW)) {
            snowSeasons.add(Season.WINTER);
        }

        String springTemperature = minecraftToCelsius(springPair.getRight()) + "°C";
        String summerTemperature = minecraftToCelsius(summerPair.getRight()) + "°C";
        String fallTemperature = minecraftToCelsius(fallPair.getRight()) + "°C";
        String winterTemperature = minecraftToCelsius(winterPair.getRight()) + "°C";

        StringBuilder rainInfo = new StringBuilder(rainSeasons.isEmpty() ? "It doesn't rain in this biome." : "It rains in this biome during ");
        for(int i = 0; i < rainSeasons.size(); i++) {
            Season season = rainSeasons.get(i);
            rainInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")").append(Text.translatable(season.getTranslationKey()).getString());
            if(i == rainSeasons.size()-1) {
                rainInfo.append("$(0).");
            }else if(i == rainSeasons.size()-2) {
                rainInfo.append("$(0) and ");
            }else{
                rainInfo.append("$(0), ");
            }
        }

        StringBuilder snowInfo = new StringBuilder(snowSeasons.isEmpty() ? "It doesn't snow in this biome." : "It snows in this biome during ");
        for(int i = 0; i < snowSeasons.size(); i++) {
            Season season = snowSeasons.get(i);
            snowInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")").append(Text.translatable(season.getTranslationKey()).getString());
            if(i == snowSeasons.size()-1) {
                snowInfo.append("$(0).");
            }else if(i == snowSeasons.size()-2) {
                snowInfo.append("$(0) and ");
            }else{
                snowInfo.append("$(0), ");
            }
        }

        String biomeInfo;
        if(isJungle) {
            biomeInfo = "This is a $(2)tropical biome$(0). It's temperature doesn't change much during the year.";
        }else if(springPair.getRight() <= 0.1) {
            biomeInfo = "This is a $(3)frozen biome$(0). It is freezing cold for most of the year.";
        }else if(springPair.getRight() <= 0.3) {
            biomeInfo = "This is a $(3)cold biome$(0). It is cold for most of the seasons but it gets warmer during the summer.";
        }else if(springPair.getRight() <= 0.95) {
            biomeInfo = "This is a $(2)temperate biome$(0). It's temperatures change a lot during the year.";
        }else{
            biomeInfo = "This is a $(4)hot biome$(0). It's scalding hot for most of the year.";
        }

        JsonObject biomeFirstPage = new JsonObject();
        biomeFirstPage.add("type", new JsonPrimitive("patchouli:text"));
        biomeFirstPage.add("title", new JsonPrimitive(biomeName));
        biomeFirstPage.add("text", new JsonPrimitive("BIOME_INFO$(br2)$(2)Spring Temperature:$(0) SPRING_TEMPERATURE$(br)$(6)Summer Temperature: $(0)SUMMER_TEMPERATURE$(br)$(c)Fall Temperature: $(0)FALL_TEMPERATURE$(br)$(3)Winter Temperature: $(0)WINTER_TEMPERATURE$(br2)RAIN_INFO$(br2)SNOW_INFO"));
        String biomeText = biomeFirstPage.get("text").getAsString()
                .replace("SPRING_TEMPERATURE", springTemperature)
                .replace("SUMMER_TEMPERATURE", summerTemperature)
                .replace("FALL_TEMPERATURE", fallTemperature)
                .replace("WINTER_TEMPERATURE", winterTemperature)
                .replace("RAIN_INFO", rainInfo.toString())
                .replace("SNOW_INFO", snowInfo.toString())
                .replace("BIOME_INFO", biomeInfo)
                .replace("BIOME_NAME", biomeName);
        biomeFirstPage.add("anchor", new JsonPrimitive(biomeId.toString()));
        biomeFirstPage.add("text", new JsonPrimitive(biomeText));

        JsonObject biomeSecondPage = new JsonObject();
        biomeSecondPage.add("type", new JsonPrimitive("seasonsextras:seasonal_biome"));
        biomeSecondPage.add("name", new JsonPrimitive("Colors"));
        biomeSecondPage.add("enable_visualize", new JsonPrimitive(false));
        biomeSecondPage.add("biome_id", new JsonPrimitive(biomeId.toString()));

        if(FabricSeasonsExtrasClient.worldBiomeMultiblocks.getOrDefault(worldKey, new HashMap<>()).containsKey(biomeId)) {
            JsonArray multiblocks = new JsonArray();
            FabricSeasonsExtrasClient.worldBiomeMultiblocks.getOrDefault(worldKey, new HashMap<>()).get(biomeId).forEach(treeId -> {
                JsonObject multiblock = FabricSeasonsExtrasClient.multiblocks.get(treeId);
                if (multiblock != null) {
                    multiblocks.add(multiblock);
                }
            });
            biomeSecondPage.add("multiblocks", multiblocks);
        }

        JsonArray oldPages = pages.deepCopy();

        setOrAdd(pages, index, biomeFirstPage);
        setOrAdd(pages, index+1, biomeSecondPage);

        for(int i = index; i < oldPages.size(); i++) {
            setOrAdd(pages, index+2+(i-index), oldPages.get(i));
        }
    }

    private static void setOrAdd(JsonArray array, int index, JsonElement element) {
        if(index == array.size()) {
            array.add(element);
        }else{
            array.set(index, element);
        }
    }

    //I don't know what this is but it kind of works
    public static double minecraftToCelsius(float x) {
        double value = 1.02557113*x*x*x*x - 2.5249755*x*x*x + 0.61120004*x*x + 28.51377*x - 4.2984804;
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    //TODO: Let player select between celsius and fahrenheit
    public static double minecraftToFahrenheit(float x) {
        return (minecraftToCelsius(x) * 1.8) + 32;
    }

}
