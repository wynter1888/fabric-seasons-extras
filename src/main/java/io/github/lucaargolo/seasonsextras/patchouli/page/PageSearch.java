package io.github.lucaargolo.seasonsextras.patchouli.page;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.seasonsextras.mixed.FontManagerMixed;
import io.github.lucaargolo.seasonsextras.mixin.MinecraftClientAccessor;
import io.github.lucaargolo.seasonsextras.utils.Tickable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class PageSearch extends PageText implements Tickable {

    protected final transient List<Pair<Identifier, String>> searchable = new ArrayList<>();

    protected abstract String getSearchable();

    private transient BookTextRenderer textRender;
    private transient TextFieldWidget searchBar;

    //Variables used for the scroll logic
    private double scrollableOffset = 0.0;
    private boolean scrollable = false;
    private double excessHeight = 0.0;
    private boolean draggingScroll = false;

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pticks) {

        super.render(ms, mouseX, mouseY, pticks);
        RenderSystem.setShaderTexture(0, parent.getBookTexture());
        DrawableHelper.drawTexture(ms, 6, 135, 140f, 183f, 99, 14, 512, 256);
        textRender.render(ms, mouseX, mouseY);
        RenderSystem.setShaderTexture(0, parent.getBookTexture());
        if(scrollable) {
            int mx = mouseX-parent.bookLeft;
            int my = mouseY-parent.bookTop;
            double offset = MathHelper.lerp(scrollableOffset/excessHeight, 12, 104);
            DrawableHelper.drawTexture(ms, 99, 11, 352f, 56f, 8, 115, 512, 256);
            DrawableHelper.drawTexture(ms, 100, (int) offset, 360f, 56f, 6, 21, 512, 256);
            if(mx > 100 && mx < 106 && my > offset && my < offset+21) {
                DrawableHelper.fill(ms, 100, (int) offset, 106, (int) offset + 21, -2130706433);
            }
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, 12);
        TextRenderer fontRenderer = ((FontManagerMixed) ((MinecraftClientAccessor) MinecraftClient.getInstance()).getFontManager()).createStyledTextRenderer(book.getFontStyle());
        searchBar = new TextFieldWidget(fontRenderer, parent.bookLeft+left+21, parent.bookTop+top+136, 115, 10, Text.literal(""));
        searchBar.setEditableColor(0);
        searchBar.setDrawsBackground(false);
        searchBar.setChangedListener(this::updateText);
        searchBar.setRenderTextProvider((string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, parent.book.getFontStyle()));
        parent.addDrawableChild(searchBar);
        updateText(searchBar.getText());
    }

    public void updateText(String search) {
        scrollableOffset = 0.0;
        excessHeight = 0.0;
        scrollable = false;

        AtomicReference<String> string = new AtomicReference<>("");
        AtomicInteger height = new AtomicInteger(0);
        AtomicInteger overflow = new AtomicInteger(0);
        searchable.stream().filter(p -> p.getRight().toLowerCase().contains(search.toLowerCase())).forEach(p -> {
            if(height.getAndIncrement() < 13) {
                string.getAndUpdate(t -> t + "$(l:"+getSearchable()+"#"+p.getLeft()+")"+p.getRight()+"$(br)$()");
            }else{
                scrollable = true;
                overflow.getAndIncrement();
            }
        });
        excessHeight = overflow.get()*GuiBook.TEXT_LINE_HEIGHT;
        while (height.getAndIncrement() < 13) {
            string.getAndUpdate(t -> t + "$(br)");
        }
        string.getAndUpdate(t -> t + "$(br)");
        setText(string.get());
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, getTextHeight());
    }

    public void updateTextHeight() {
        String search = searchBar.getText();
        int cycle = MathHelper.floor(scrollableOffset/GuiBook.TEXT_LINE_HEIGHT);
        if(cycle < searchable.size()) {
            AtomicReference<String> string = new AtomicReference<>("");
            AtomicInteger height = new AtomicInteger(0);
            searchable.subList(cycle, searchable.size()).stream().filter(p -> p.getRight().toLowerCase().contains(search.toLowerCase())).forEach(p -> {
                if(height.getAndIncrement() < 13) {
                    string.getAndUpdate(t -> t + "$(l:"+getSearchable()+"#"+p.getLeft()+")"+p.getRight()+"$(br)$()");
                }
            });
            while (height.getAndIncrement() < 13) {
                string.getAndUpdate(t -> t + "$(br)");
            }
            string.getAndUpdate(t -> t + "$(br)");
            setText(string.get());
            textRender = new BookTextRenderer(parent, text.as(Text.class), 0, getTextHeight());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int mx = (int) mouseX-parent.bookLeft;
        int my = (int) mouseY-parent.bookTop;
        if(scrollable && mx > 100 && mx < 106 && my > 12 && my < 125) {
            double offset = MathHelper.lerp(scrollableOffset/excessHeight, 12, 104);
            if(my > offset && my < offset+21) {
                draggingScroll = true;
            }else{
                scrollableOffset = MathHelper.lerp((my-12.0)/113.0, 0.0, excessHeight);
                updateTextHeight();
            }
        }
        return textRender.click(mouseX, mouseY, mouseButton);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if(draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if(draggingScroll) {
            if(scrollable) {
                scrollableOffset += deltaY * (excessHeight/104);
                scrollableOffset = MathHelper.clamp(scrollableOffset, 0.0, excessHeight);
                updateTextHeight();
                return true;
            }else{
                draggingScroll = false;
                return false;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(scrollable) {
            scrollableOffset -= amount*4;
            scrollableOffset = MathHelper.clamp(scrollableOffset, 0.0, excessHeight);
            updateTextHeight();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        searchBar.tick();
    }

    @Override
    public void onHidden(GuiBookEntry parent) {
        super.onHidden(parent);
        parent.removeDrawablesIf(d -> d.equals(searchBar));
        searchBar = null;
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

}