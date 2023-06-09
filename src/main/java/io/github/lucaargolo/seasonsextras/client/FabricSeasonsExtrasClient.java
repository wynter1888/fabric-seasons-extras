package io.github.lucaargolo.seasonsextras.client;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.client.screen.AirConditioningScreen;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.utils.TooltipRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class FabricSeasonsExtrasClient implements ClientModInitializer {

    public static boolean prefersCelsius = true;
    public static final List<Text> testedTooltip = new ArrayList<>();
    public static BlockPos testedPos = null;


    @Override
    public void onInitializeClient() {
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
        ModelPredicateProviderRegistry.register(FabricSeasonsExtras.SEASON_CALENDAR_ITEM, new Identifier("season"), (itemStack, clientWorld, livingEntity, seed) -> {
            Entity entity = livingEntity != null ? livingEntity : itemStack.getHolder();
            if (entity == null) {
                return 0.0F;
            } else {
                if (clientWorld == null && entity.getWorld() instanceof ClientWorld) {
                    clientWorld = (ClientWorld) entity.getWorld();
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
                if (clientWorld == null && entity.getWorld() instanceof ClientWorld) {
                    clientWorld = (ClientWorld) entity.getWorld();
                }
                if (clientWorld == null) {
                    return 0f;
                } else {
                    long timeToNextSeason = FabricSeasons.getTimeToNextSeason(clientWorld);
                    double progressLeft = (double) timeToNextSeason / FabricSeasons.getCurrentSeason(clientWorld).getSeasonLength();
                    return (((int) (16.0 - (progressLeft*16.0))) % 16)/16f;
                }
            }
        });
        HandledScreens.register(FabricSeasonsExtras.AIR_CONDITIONING_SCREEN_HANDLER, AirConditioningScreen::new);
        for (GreenhouseGlassBlock block : FabricSeasonsExtras.GREENHOUSE_GLASS_BLOCKS) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent());
        }
        BlockRenderLayerMap.INSTANCE.putBlock(Registries.BLOCK.get(new ModIdentifier("season_calendar")), RenderLayer.getCutout());
        HudRenderCallback.EVENT.register(((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            TooltipRenderer.render(client, drawContext, tickDelta);
        }));
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
