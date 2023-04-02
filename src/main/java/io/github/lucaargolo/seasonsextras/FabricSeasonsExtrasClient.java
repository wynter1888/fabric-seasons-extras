package io.github.lucaargolo.seasonsextras;

import com.google.gson.*;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.mixin.GuiBookEntryAccessor;
import io.github.lucaargolo.seasonsextras.patchouli.PageBiomeDescription;
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
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.client.book.text.BookTextParser;

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

    public static boolean prefersCelsius = true;


    @Override
    public void onInitializeClient() {
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

        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("biome_search"), PageBiomeSearch.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("seasonal_biome"), PageSeasonalBiome.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("biome_description"), PageBiomeDescription.class);

        BookTextParser.register((parameter, state) -> prefersCelsius ? minecraftToCelsius(parameter) : minecraftToFahrenheit(parameter), "seasonsextrastemperature");
        BookTextParser.register((parameter, state) -> I18n.translate(parameter), "seasonsextrastranslate");

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

        String biomeName = biomeId.toTranslationKey("biome");
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

        String springTemperature = "$(seasonsextrastemperature:"+springPair.getRight()+")";
        String summerTemperature = "$(seasonsextrastemperature:"+summerPair.getRight()+")";
        String fallTemperature = "$(seasonsextrastemperature:"+fallPair.getRight()+")";
        String winterTemperature = "$(seasonsextrastemperature:"+winterPair.getRight()+")";

        StringBuilder rainInfo = new StringBuilder(rainSeasons.isEmpty() ? "$(seasonsextrastranslate:patchouli.seasonsextras.doesnotrain)" : "$(seasonsextrastranslate:patchouli.seasonsextras.rainsduring)"+" ");
        for(int i = 0; i < rainSeasons.size(); i++) {
            Season season = rainSeasons.get(i);
            rainInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")");
            if(i == rainSeasons.size()-1) {
                rainInfo.append("$(0).");
            }else if(i == rainSeasons.size()-2) {
                rainInfo.append("$(0) ").append("$(seasonsextrastranslate:patchouli.seasonsextras.and)").append(" ");
            }else{
                rainInfo.append("$(0), ");
            }
        }

        StringBuilder snowInfo = new StringBuilder(snowSeasons.isEmpty() ? "$(seasonsextrastranslate:patchouli.seasonsextras.doesnotsnow)" : "$(seasonsextrastranslate:patchouli.seasonsextras.snowsduring)"+" ");
        for(int i = 0; i < snowSeasons.size(); i++) {
            Season season = snowSeasons.get(i);
            snowInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")");
            if(i == snowSeasons.size()-1) {
                snowInfo.append("$(0).");
            }else if(i == snowSeasons.size()-2) {
                snowInfo.append("$(0) ").append("$(seasonsextrastranslate:patchouli.seasonsextras.and)").append(" ");
            }else{
                snowInfo.append("$(0), ");
            }
        }

        String biomeInfo;
        if(isJungle) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisa)"+" $(2)"+"$(seasonsextrastranslate:patchouli.seasonsextras.tropicalbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.tropicaldesc)";
        }else if(springPair.getRight() <= 0.1) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisa)"+" $(3)"+"$(seasonsextrastranslate:patchouli.seasonsextras.frozenbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.frozendesc)";
        }else if(springPair.getRight() <= 0.3) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisa)"+" $(3)"+"$(seasonsextrastranslate:patchouli.seasonsextras.coldbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.colddesc)";
        }else if(springPair.getRight() <= 0.95) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisa)"+" $(2)"+"$(seasonsextrastranslate:patchouli.seasonsextras.temperatebiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.temperatedesc)";
        }else{
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisa)"+" $(4)"+"$(seasonsextrastranslate:patchouli.seasonsextras.hotbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.hotdesc)";
        }

        JsonObject biomeFirstPage = new JsonObject();
        biomeFirstPage.add("type", new JsonPrimitive("seasonsextras:biome_description"));
        biomeFirstPage.add("title", new JsonPrimitive(biomeName));
        String biomeText = biomeInfo +
                "$(br2)" +
                "$(2)" + "$(seasonsextrastranslate:patchouli.seasonsextras.springtemp)" + ": $(0)" + springTemperature +
                "$(br)" +
                "$(6)" + "$(seasonsextrastranslate:patchouli.seasonsextras.summertemp)" + ": $(0)" + summerTemperature +
                "$(br)" +
                "$(c)" + "$(seasonsextrastranslate:patchouli.seasonsextras.falltemp)" + ": $(0)" + fallTemperature +
                "$(br)" +
                "$(3)" + "$(seasonsextrastranslate:patchouli.seasonsextras.wintertemp)" + ": $(0)" + winterTemperature +
                "$(br2)" +
                rainInfo +
                "$(br2)" +
                snowInfo;
        biomeFirstPage.add("text", new JsonPrimitive(biomeText));
        biomeFirstPage.add("anchor", new JsonPrimitive(biomeId.toString()));

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
    private static double minecraftToCelsius(float x) {
        return 1.02557113*x*x*x*x - 2.5249755*x*x*x + 0.61120004*x*x + 28.51377*x - 4.2984804;
    }

    public static String minecraftToCelsius(String string) {
        float x = Float.parseFloat(string);
        return ""+BigDecimal.valueOf(minecraftToCelsius(x)).setScale(1, RoundingMode.HALF_UP).doubleValue()+ "°C";
    }

    public static String minecraftToFahrenheit(String string) {
        float x = Float.parseFloat(string);
        return ""+BigDecimal.valueOf((minecraftToCelsius(x) * 1.8) + 32).setScale(1, RoundingMode.HALF_UP).doubleValue()+ "°F";
    }

}
