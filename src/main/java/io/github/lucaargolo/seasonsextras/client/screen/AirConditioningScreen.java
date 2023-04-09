package io.github.lucaargolo.seasonsextras.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class AirConditioningScreen extends HandledScreen<AirConditioningScreenHandler> {

    private final int[] pressedTime = new int[] { 0, 0, 0 };
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
        AirConditioningBlockEntity.Module[] modules = handler.getModules();
        float lerpProgress = MathHelper.lerp(delta, lastProgress, handler.getProgress());
        lastProgress = handler.getProgress();
        int progress = MathHelper.floor(lerpProgress);
        int maxProgress = AirConditioningBlockEntity.getMaxProgress(modules);
        this.drawTexture(matrices, x+79, y+42, 176, 28, Math.min(progress, maxProgress-13), 29);
        for(int i = 0; i < modules.length; i++) {
            int moduleProgress = Math.min(Math.max(0, progress - (28 + (18*i))), modules[i].enabled ? 13 : 0);
            this.drawTexture(matrices, x+102+(18*i), y+54+(13-moduleProgress), 176, 57+(13-moduleProgress), 7, moduleProgress);
        }
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, handler.getConditioning().getTexture());
        AirConditioningBlockEntity.Module[] modules = handler.getModules();
        for(int i = 0; i < modules.length; i++) {
            this.drawTexture(matrices, 102+(18*i), 59, modules[i].enabled ? 176 : 183,  pressedTime[i] == 0 ? 14 : 21, 7, 7);
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
        AirConditioningBlockEntity.Module[] modules = handler.getModules();
        for(int i = 0; i < modules.length; i++) {
            int clickX = x+102+(18*i);
            int clickY = y+59;
            if(mouseX >= clickX && mouseX < clickX+7 && mouseY >= clickY && mouseY < clickY+7) {
                pressedTime[i] = 3;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(i);
                ClientPlayNetworking.send(FabricSeasonsExtras.SEND_MODULE_PRESS_C2S, buf);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}
