package io.github.lucaargolo.seasonsextras.utils;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class StyledTextRenderer extends TextRenderer {

    private final Style style;

    public StyledTextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, boolean validateAdvance, Style style) {
        super(fontStorageAccessor, validateAdvance);
        this.style = style;
    }


    @Override
    public int drawWithShadow(MatrixStack matrices, String text, float x, float y, int color) {
        return super.draw(matrices, Text.literal(text).styled(s -> style), x, y, color);
    }

    @Override
    public int drawWithShadow(MatrixStack matrices, OrderedText text, float x, float y, int color) {
        return super.draw(matrices, text, x, y, color);
    }
}
