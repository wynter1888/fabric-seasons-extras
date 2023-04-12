package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;
import vazkii.patchouli.common.book.Book;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GuiBookLanding.class, remap = false)
public class GuiBookLandingMixin extends GuiBook {

    @Shadow List<BookEntry> entriesInPamphlet;
    @Shadow @Nullable BookTextRenderer text;

    @Unique
    List<BookEntry> capturedEntriesInPamphlet;
    @Unique
    BookTextRenderer capuredText;
    @Unique
    Book capturedBook;

    @Unique
    private BookEntry dummyEntry;
    @Unique
    private int entryX = 0;

    public GuiBookLandingMixin(Book book, Text title) {
        super(book, title);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void captureBook(Book book, CallbackInfo ci) {
        this.capturedBook = book;
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            JsonObject dummyJson = new JsonObject();
            dummyJson.add("name", new JsonPrimitive(""));
            dummyJson.add("category", new JsonPrimitive("seasonsextras:compendium"));
            dummyJson.add("icon", new JsonPrimitive("minecraft:air"));
            dummyJson.add("pages", new JsonArray());
            dummyJson.add("read_by_default", new JsonPrimitive(true));
            dummyEntry = new BookEntry(dummyJson, new Identifier(""), capturedBook);
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void captureText(CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            capuredText = text;
            if (spread != 0) {
                text = null;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "buildEntryButtons")
    public void addDummySpaces(CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            if(capturedEntriesInPamphlet == null) {
                capturedEntriesInPamphlet = entriesInPamphlet;
            }
            entriesInPamphlet = new ArrayList<>();
            entriesInPamphlet.add(dummyEntry);
            entriesInPamphlet.add(dummyEntry);
            entriesInPamphlet.add(capturedEntriesInPamphlet.get(0));
            if(FabricSeasons.CONFIG.isSeasonMessingCrops()) {
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(1));
            }
            entriesInPamphlet.add(dummyEntry);
            entriesInPamphlet.add(dummyEntry);
            entriesInPamphlet.add(dummyEntry);
            if(FabricSeasons.CONFIG.isSeasonMessingCrops()) {
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(2));
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(3));
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(4));
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(5));
            }
            entriesInPamphlet.add(capturedEntriesInPamphlet.get(6));
            entriesInPamphlet.add(capturedEntriesInPamphlet.get(7));
            if(FabricSeasons.CONFIG.isSeasonMessingCrops()) {
                entriesInPamphlet.add(capturedEntriesInPamphlet.get(8));
            }
        }
    }

    @ModifyArgs(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/GuiBookLanding;addEntryButtons(IIII)V"), method = "buildEntryButtons")
    public void fixTopPadding(Args args) {
        int y = args.get(1);
        int start = args.get(2);
        int count = args.get(3);
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            if(spread == 0) {
                args.set(1, y+3);
            }
            args.set(2, start + (13 * spread));
            args.set(3, count+2);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/button/GuiButtonEntry;<init>(Lvazkii/patchouli/client/book/gui/GuiBook;IILvazkii/patchouli/client/book/BookEntry;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V", shift = At.Shift.BEFORE), method = "addEntryButtons", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void collectEntry(int x, int y, int start, int count, CallbackInfo ci, int i) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            if (spread != 0 && i-start < 13) {
                entryX = bookLeft + LEFT_PAGE_X;
            }else{
                entryX = bookLeft + RIGHT_PAGE_X;
            }
        }
    }

    @ModifyArg(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/button/GuiButtonEntry;<init>(Lvazkii/patchouli/client/book/gui/GuiBook;IILvazkii/patchouli/client/book/BookEntry;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V"), method = "addEntryButtons", index = 1)
    public int fixEntryX(int x) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            return entryX;
        }else{
            return x;
        }
    }

    @Inject(at = @At("TAIL"), method = "addEntryButtons")
    public void fixDummySpaces(int x, int y, int start, int count, CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            removeDrawablesIf(drawable -> drawable instanceof GuiButtonEntry entry && entry.getEntry() == dummyEntry);
        }
    }


    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/GuiBookLanding;addEntryButtons(IIII)V"), method = "buildEntryButtons")
    public void addPageButton(CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            maxSpreads = (int) Math.ceil((float) (13+entriesInPamphlet.size()) / (13 * 2));
        }
    }

    @Inject(at = @At("HEAD"), method = "onPageChanged")
    public void fixText(CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            if(spread == 0) {
                text = capuredText;
            }else{
                text = null;
            }
        }
    }
    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/GuiBookLanding;drawFromTexture(Lnet/minecraft/client/util/math/MatrixStack;Lvazkii/patchouli/common/book/Book;IIIIII)V"), method = "drawHeader", cancellable = true)
    public void drawExtraHeaders(MatrixStack ms, CallbackInfo ci) {
        if(capturedBook.id.equals(new ModIdentifier("seasonal_compendium"))) {
            if (spread == 0) {
                int color = book.nameplateColor;
                drawFromTexture(ms, book, 272 - 132, 12, 0, 211, 140, 31);
                textRenderer.draw(ms, Text.translatable("patchouli.seasonsextras.modifications"), 272 - 132 + 21, 16, color);
                textRenderer.draw(ms, Text.translatable("patchouli.seasonsextras.modifications_info").fillStyle(book.getFontStyle()), 272 - 132 + 21, 24, color);
                int cropOffset = FabricSeasons.CONFIG.isSeasonMessingCrops() ? 0 : -10;
                drawFromTexture(ms, book, 272 - 132, 69+cropOffset, 0, 211, 140, 31);
                textRenderer.draw(ms, Text.translatable("patchouli.seasonsextras.resources"), 272 - 132 + 21, 73+cropOffset, color);
                textRenderer.draw(ms, Text.translatable("patchouli.seasonsextras.resources_info").fillStyle(book.getFontStyle()), 272 - 132 + 21, 81+cropOffset, color);
            } else {
                ci.cancel();
            }
        }
    }

}
