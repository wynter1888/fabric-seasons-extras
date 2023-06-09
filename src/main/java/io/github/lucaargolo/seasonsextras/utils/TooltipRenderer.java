package io.github.lucaargolo.seasonsextras.utils;

import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.client.FabricSeasonsExtrasClient;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class TooltipRenderer {

    public static void render(MinecraftClient client, DrawContext drawContext, float tickDelta) {
        if(client.player != null && client.world != null && client.crosshairTarget != null) {
            PlayerEntity player = client.player;
            World world = client.world;
            if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = world.getBlockState(pos);
                renderCalendar(player, client, world, drawContext, state);
                renderTestedBlock(client, drawContext, pos);
            }
        }

    }

    public static void renderCalendar(PlayerEntity player, MinecraftClient client, World world, DrawContext drawContext, BlockState state) {
        if(player.isSneaking() && state.isOf(FabricSeasonsExtras.SEASON_CALENDAR_BLOCK)) {
            float scaleFactor = client.getWindow().calculateScaleFactor(client.options.getGuiScale().getValue(), client.forcesUnicodeFont());
            int x = (int) (client.getWindow().getWidth()/(scaleFactor*2) - 24);
            int y = (int) (client.getWindow().getHeight()/(scaleFactor*2) - 24);
            List<Text> lines = new ArrayList<>();
            SeasonCalendarItem.appendCalendarTooltip(world, lines);
            drawContext.drawTooltip(client.textRenderer, lines, x, y);
        }
    }

    public static void renderTestedBlock(MinecraftClient client, DrawContext drawContext, BlockPos pos) {
        if(FabricSeasonsExtrasClient.testedPos != null) {
            if(FabricSeasonsExtrasClient.testedPos.equals(pos)) {
                float scaleFactor = client.getWindow().calculateScaleFactor(client.options.getGuiScale().getValue(), client.forcesUnicodeFont());
                int x = (int) (client.getWindow().getWidth() / (scaleFactor * 2) - 24);
                int y = (int) (client.getWindow().getHeight() / (scaleFactor * 2) - 24);
                drawContext.drawTooltip(client.textRenderer, FabricSeasonsExtrasClient.testedTooltip, x, y);
            }else{
                FabricSeasonsExtrasClient.testedPos = null;
                FabricSeasonsExtrasClient.testedTooltip.clear();
            }
        }
    }


}
