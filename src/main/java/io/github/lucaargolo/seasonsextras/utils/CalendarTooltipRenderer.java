package io.github.lucaargolo.seasonsextras.utils;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.item.SeasonCalendarItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class CalendarTooltipRenderer {

    private static final Screen DUMMY_SCREEN = new Screen(Text.translatable("blocks.seasonsextras.season_calendar")) {};

    public static void render(MinecraftClient client, MatrixStack matrixStack, float tickDelta) {
        if(client.player != null && client.world != null && client.crosshairTarget != null) {
            PlayerEntity player = client.player;
            World world = client.world;
            if (player.isSneaking() && client.crosshairTarget instanceof BlockHitResult hitResult) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = world.getBlockState(pos);
                if(state.isOf(FabricSeasonsExtras.SEASON_CALENDAR_BLOCK)) {
                    float scaleFactor = client.getWindow().calculateScaleFactor(client.options.getGuiScale().getValue(), client.forcesUnicodeFont());
                    int x = (int) (client.getWindow().getWidth()/(scaleFactor*2) - 24);
                    int y = (int) (client.getWindow().getHeight()/(scaleFactor*2) - 24);
                    DUMMY_SCREEN.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
                    List<Text> lines = new ArrayList<>();
                    SeasonCalendarItem.appendCalendarTooltip(world, lines);
                    DUMMY_SCREEN.renderTooltip(matrixStack, lines, x, y);
                }
            }
        }

    }


}
