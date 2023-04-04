package io.github.lucaargolo.seasonsextras.patchouli.page;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtrasClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageText;

public class PageBiomeDescription extends PageText {

    private transient BookTextRenderer textRender;

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pticks) {
        super.render(ms, mouseX, mouseY, pticks);
        textRender.render(ms, mouseX, mouseY);
        RenderSystem.setShaderTexture(0, parent.getBookTexture());
        int mx = mouseX-parent.bookLeft;
        int my = mouseY-parent.bookTop;
        DrawableHelper.drawTexture(ms, -28, 0, 352f+27f, FabricSeasonsExtrasClient.prefersCelsius ? 56f : 66f, 13, 10, 512, 256);
        if(mx > -28 && mx < -28+13 && my > 0 && my < 10) {
            DrawableHelper.drawTexture(ms, -28, 0, 352f+14f, FabricSeasonsExtrasClient.prefersCelsius ? 56f : 66f, 13, 10, 512, 256);
            parent.renderTooltip(ms, FabricSeasonsExtrasClient.prefersCelsius ? Text.translatable("patchouli.seasonsextras.changetofahrenheit") : Text.translatable("patchouli.seasonsextras.changetocelsius"), mx, my);
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, 12);
        updateText();
    }

    public void updateText() {
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, getTextHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int mx = (int) mouseX-parent.bookLeft;
        int my = (int) mouseY-parent.bookTop;
        if(mx > -28 && mx < -28+13 && my > 0 && my < 10) {
            FabricSeasonsExtrasClient.prefersCelsius = !FabricSeasonsExtrasClient.prefersCelsius;
            updateText();
            return true;
        }
        return textRender.click(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

}