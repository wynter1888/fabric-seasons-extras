package io.github.lucaargolo.seasonsextras.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtrasClient;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tag.BiomeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContentLoader;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;

import java.util.*;
import java.util.function.Function;

@Mixin(value = BookContentsBuilder.class, remap = false)
public class BookContentsBuilderMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/BookEntry;<init>(Lcom/google/gson/JsonObject;Lnet/minecraft/util/Identifier;Lvazkii/patchouli/common/book/Book;)V"), method = "loadEntry", locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void injectPagesAtLoad(Book book, BookContentLoader loader, Identifier id, Identifier file, Function<Identifier, BookCategory> categories, CallbackInfoReturnable<@Nullable BookEntry> cir, JsonElement json) {
        //TODO make a register/event for this

        if(Objects.equals(id, new ModIdentifier("biomes")) && json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            JsonArray pages = object.getAsJsonArray("pages");
            JsonObject firstPage = pages.get(2).getAsJsonObject();
            JsonObject secondPage = pages.get(3).getAsJsonObject();
            pages.remove(3);
            pages.remove(2);
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if(world != null) {
                RegistryKey<World> worldKey = world.getRegistryKey();
                FabricSeasonsExtrasClient.worldValidBiomes.getOrDefault(worldKey, new HashSet<>()).forEach(entry -> {
                    entry.getKey().ifPresent(key -> {
                        Biome biome = entry.value();
                        Identifier biomeId = key.getValue();

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

                        String springTemperature = FabricSeasonsExtras.minecraftToCelsius(springPair.getRight()) + "°C";
                        String summerTemperature = FabricSeasonsExtras.minecraftToCelsius(summerPair.getRight()) + "°C";
                        String fallTemperature = FabricSeasonsExtras.minecraftToCelsius(fallPair.getRight()) + "°C";
                        String winterTemperature = FabricSeasonsExtras.minecraftToCelsius(winterPair.getRight()) + "°C";

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

                        JsonObject biomeFirstPage = firstPage.deepCopy();
                        biomeFirstPage.add("title", new JsonPrimitive(biomeName));
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
                        pages.add(biomeFirstPage);
                        JsonObject biomeSecondPage = secondPage.deepCopy();
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

                        pages.add(biomeSecondPage);
                    });
                });
            }


        }
    }

}
