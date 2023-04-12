package io.github.lucaargolo.seasonsextras.patchouli.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.page.PageMultiblock;
import vazkii.patchouli.common.multiblock.AbstractMultiblock;
import vazkii.patchouli.common.multiblock.SerializedMultiblock;

@Mixin(value = PageMultiblock.class, remap = false)
public interface PageMultiblockAccessor {

    @Accessor
    SerializedMultiblock getSerializedMultiblock();

    @Accessor
    AbstractMultiblock getMultiblockObj();

    @Accessor
    void setSerializedMultiblock(SerializedMultiblock s);

    @Accessor
    void setMultiblockObj(AbstractMultiblock a);

}
