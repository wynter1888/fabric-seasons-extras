package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

@Mixin(value = GuiBookEntry.class, remap = false)
public interface GuiBookEntryAccessor  {

    @Nullable @Accessor
    BookPage getLeftPage();
    @Nullable @Accessor
    BookPage getRightPage();

}
