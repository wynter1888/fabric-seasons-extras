package io.github.lucaargolo.seasonsextras.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.lucaargolo.seasonsextras.utils.PatchouliModifications;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContentLoader;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;

import java.util.function.Function;

@Mixin(value = BookContentsBuilder.class, remap = false)
public class BookContentsBuilderMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/BookEntry;<init>(Lcom/google/gson/JsonObject;Lnet/minecraft/util/Identifier;Lvazkii/patchouli/common/book/Book;)V"), method = "loadEntry", locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void injectPagesAtLoad(Book book, BookContentLoader loader, Identifier id, Identifier file, Function<Identifier, BookCategory> categories, CallbackInfoReturnable<@Nullable BookEntry> cir, JsonElement json) {
        if(json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            JsonElement element = object.get("pages");
            if(element.isJsonArray()) {
                JsonArray pages = element.getAsJsonArray();
                PatchouliModifications.applyEntries(id, pages);
            }
        }
    }

}
