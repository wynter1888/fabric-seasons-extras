package io.github.lucaargolo.seasonsextras.mixin;


import io.github.lucaargolo.seasonsextras.patchouli.page.PageSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Mouse.class)
public class MouseMixin {


    @Shadow private int activeButton;

    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "method_1605", cancellable = true)
    private static void mouseReleased(boolean[] bls, Screen screen, double d, double e, int i, CallbackInfo ci) {
        if(screen instanceof GuiBookEntryAccessor bookEntry && screen instanceof GuiBookAccessor book) {
            if(bookEntry.getLeftPage() instanceof PageSearch page) {
                if(page.mouseReleased(d/book.getScaleFactor(), e/book.getScaleFactor(), i)) {
                    ci.cancel();
                }
            }
            if(bookEntry.getRightPage() instanceof PageSearch page) {
                if(page.mouseReleased(d/book.getScaleFactor(), e/book.getScaleFactor(), i)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "method_1602", cancellable = true)
    public void mouseDragged(Screen screen, double d, double e, double f, double g, CallbackInfo ci) {
        if(screen instanceof GuiBookEntryAccessor bookEntry && screen instanceof GuiBookAccessor book) {
            if(bookEntry.getLeftPage() instanceof PageSearch page) {
                if(page.mouseDragged(d/book.getScaleFactor(), e/book.getScaleFactor(), this.activeButton, f/book.getScaleFactor(), g/book.getScaleFactor())) {
                    ci.cancel();
                }
            }
            if(bookEntry.getRightPage() instanceof PageSearch page) {
                if(page.mouseDragged(d/book.getScaleFactor(), e/book.getScaleFactor(), this.activeButton, f/book.getScaleFactor(), g/book.getScaleFactor())) {
                    ci.cancel();
                }
            }
        }
    }


    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseScrolled(DDD)Z"), method = "onMouseScroll", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void mouseScrolled(long window, double horizontal, double vertical, CallbackInfo ci, double d, double e, double f) {
        Screen screen = this.client.currentScreen;
        if(screen instanceof GuiBookEntryAccessor bookEntry && screen instanceof GuiBookAccessor book) {
            if(bookEntry.getLeftPage() instanceof PageSearch page) {
                if(page.mouseScrolled(e/book.getScaleFactor(), f/book.getScaleFactor(), d)) {
                   ci.cancel();
                }
            }
            if(bookEntry.getRightPage() instanceof PageSearch page) {
                if(page.mouseScrolled(e/book.getScaleFactor(), f/book.getScaleFactor(), d)) {
                    ci.cancel();
                }
            }
        }
    }


}
