package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.common.book.Book;

@Mixin(value = GuiBookEntry.class, remap = false)
public abstract class GuiBookEntryMixin extends GuiBook {

    public GuiBookEntryMixin(Book book, Text title) {
        super(book, title);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true, remap = true)
    public void fixSearchBar(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.close();
            cir.setReturnValue(true);
        }else if (getFocused() instanceof TextFieldWidget textFieldWidget && textFieldWidget.isFocused()) {
            cir.setReturnValue(textFieldWidget.keyPressed(keyCode, scanCode, modifiers));
        }
    }

}
