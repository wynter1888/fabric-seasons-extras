package io.github.lucaargolo.seasonsextras.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

@Mixin(value = GuiBook.class, remap = false)
public interface GuiBookAccessor {

    @Accessor
    float getScaleFactor();

}
