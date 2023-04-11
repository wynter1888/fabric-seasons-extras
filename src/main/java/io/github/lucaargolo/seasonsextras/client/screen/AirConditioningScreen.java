package io.github.lucaargolo.seasonsextras.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity.*;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class AirConditioningScreen extends HandledScreen<AirConditioningScreenHandler> {

    private final int[] pressedTime = new int[] { 0, 0, 0, 0 };
    private final int[] lastBurnProgress = new int[] { 0, 0, 0 };
    private int lastProgress = 0;

    public AirConditioningScreen(AirConditioningScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, handler.getConditioning().getTexture());
        this.drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        float delta = MinecraftClient.getInstance().getTickDelta();
        RenderSystem.setShaderTexture(0, handler.getConditioning().getTexture());
        BurnSlot[] burnSlots = handler.getBurnSlots();

        float lerpProgress = MathHelper.lerp(delta, lastProgress, handler.getProgress());
        lastProgress = handler.getProgress();
        int progress = MathHelper.floor(lerpProgress);

        int maxProgress = AirConditioningScreenHandler.getMaxProgress(burnSlots);
        this.drawTexture(matrices, 79, 42, 176, 28, Math.min(progress, maxProgress-13), 29);
        List<OrderedText> tooltip = new ArrayList<>();
        boolean sneak = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), MinecraftClient.getInstance().options.sneakKey.boundKey.getCode());
        for(int i = 0; i < burnSlots.length; i++) {
            BurnSlot burnSlot = burnSlots[i];
            int burnSlotX = x+102+(18*i);
            int burnSlotY = y+59;
            //Draw item transfer progress
            int moduleProgress = Math.min(Math.max(0, progress - (28 + (18*i))), burnSlot.enabled && !burnSlot.full ? 13 : 0);
            this.drawTexture(matrices, 102+(18*i), 54+(13-moduleProgress), 176, 57+(13-moduleProgress), 7, moduleProgress);
            //Draw burn slot state
            this.drawTexture(matrices, 102+(18*i), 59, burnSlot.enabled ? 176 : 183,  pressedTime[i] == 0 ? 14 : 21, 7, 7);
            if(mouseX >= burnSlotX && mouseX < burnSlotX+7 && mouseY >= burnSlotY && mouseY < burnSlotY+7) {
                this.fillGradient(matrices, burnSlotX-x, burnSlotY-y, burnSlotX-x+7, burnSlotY-y+7, -2130706433, -2130706433);
                tooltip.add(Text.translatable("screen.seasonsextras."+(burnSlot.enabled ? "close" : "open")+"_valve").formatted(burnSlot.enabled ? Formatting.RED : Formatting.GREEN).asOrderedText());
                if(!sneak) {
                    tooltip.add(Text.translatable("tooltip.seasons.show_more", Text.translatable(MinecraftClient.getInstance().options.sneakKey.getBoundKeyTranslationKey()).formatted(Formatting.BLUE)).formatted(Formatting.GRAY).asOrderedText());
                }else{
                    tooltip.addAll(textRenderer.wrapLines(Text.translatable("screen.seasonsextras.valve_description").formatted(Formatting.GRAY), 240));
                }
            }
            //Draw burn progress
            int slotProgress = MathHelper.floor(MathHelper.lerp(burnSlot.burnTime/(burnSlot.burnTimeTotal + 0f), 0, 13));
            float lerpSlotProgress = MathHelper.lerp(delta, lastBurnProgress[i], slotProgress);
            lastBurnProgress[i] = slotProgress;
            int burnProgress = MathHelper.floor(lerpSlotProgress);
            this.drawTexture(matrices, 98+(18*i), 18+(13-burnProgress), 176, 13-burnProgress, 13, burnProgress);
        }
        this.drawTexture(matrices, 163, 6, handler.hasParticles() ? 190 : 197,  pressedTime[burnSlots.length] == 0 ? 14 : 21, 7, 7);
        if(mouseX >= x+163 && mouseX < x+163+7 && mouseY >= y+6 && mouseY < y+6+7) {
            this.fillGradient(matrices, 163, 6, 163+7, 6+7, -2130706433, -2130706433);
            tooltip.add(Text.translatable("screen.seasonsextras."+(handler.hasParticles() ? "disable" : "enable")+"_particles").formatted(handler.hasParticles() ? Formatting.GRAY : Formatting.YELLOW).asOrderedText());
        }
        if(mouseX >= x+97 && mouseX < x+97+54 && mouseY >= y+16 && mouseY < y+16+18) {
            this.fillGradient(matrices, 97, 16, 97+54, 16+18, -2130706433, -2130706433);
            int level = handler.getLevel();
            Season worldSeason = FabricSeasons.getCurrentSeason();
            Season conditionedSeason = handler.getConditioning().getConditioned(worldSeason, level);
            tooltip.add(Text.translatable("screen.seasonsextras.level_description", Text.literal(title.getString()).formatted(Formatting.BLUE), Text.literal(String.valueOf(level)).formatted(Formatting.BLUE)).formatted(Formatting.GRAY).asOrderedText());
            for(int i = 0; i < burnSlots.length; i++) {
                BurnSlot burnSlot = burnSlots[i];
                if(burnSlot.burnTime > 0) {
                    tooltip.add(Text.translatable("screen.seasonsextras.burn_description", (i + 1), Text.translatable("screen.seasonsextras.time_left", StringHelper.formatTicks(burnSlot.burnTime)).formatted(Formatting.DARK_RED)).formatted(Formatting.GRAY).asOrderedText());
                }
            }
            tooltip.add(Text.translatable(worldSeason.getTranslationKey()).formatted(worldSeason.getFormatting()).append(Text.literal(" -> ").formatted(Formatting.GRAY)).append(Text.translatable(conditionedSeason.getTranslationKey()).formatted(conditionedSeason.getFormatting())).asOrderedText());
        }
        if(!tooltip.isEmpty()) {
            this.renderOrderedTooltip(matrices, tooltip, mouseX-x, mouseY-y);
        }
        super.drawForeground(matrices, mouseX, mouseY);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        for(int i = 0; i < pressedTime.length; i++) {
            if(pressedTime[i] > 0) {
                pressedTime[i]--;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        BurnSlot[] burnSlots = handler.getBurnSlots();
        for(int i = 0; i < burnSlots.length; i++) {
            int clickX = x+102+(18*i);
            int clickY = y+59;
            if(mouseX >= clickX && mouseX < clickX+7 && mouseY >= clickY && mouseY < clickY+7) {
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                pressedTime[i] = 3;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(i);
                ClientPlayNetworking.send(FabricSeasonsExtras.SEND_MODULE_PRESS_C2S, buf);
            }
        }
        if(mouseX >= x+163 && mouseX < x+163+7 && mouseY >= y+6 && mouseY < y+6+7) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            pressedTime[burnSlots.length] = 3;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(burnSlots.length);
            ClientPlayNetworking.send(FabricSeasonsExtras.SEND_MODULE_PRESS_C2S, buf);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}
