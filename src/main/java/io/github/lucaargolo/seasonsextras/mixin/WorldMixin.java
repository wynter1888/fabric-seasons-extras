package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.patchouli.PatchouliMultiblockCreator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(World.class)
public class WorldMixin {

    @Inject(at = @At("HEAD"), method = "getBlockState", cancellable = true)
    public void getTestingTreePos(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        PatchouliMultiblockCreator testing = PatchouliMultiblockCreator.getTesting();
        if(testing != null && Thread.currentThread() == testing.getThread()) {
            Optional<BlockState> blockState = testing.getBlockState(pos);
            if(blockState.isPresent()) {
                cir.setReturnValue(blockState.get());
            }else if(pos.getY() == 99) {
                cir.setReturnValue(testing.getGround());
            }else{
                cir.setReturnValue(Blocks.AIR.getDefaultState());
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", cancellable = true)
    public void setTestingTreePos(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        PatchouliMultiblockCreator testing = PatchouliMultiblockCreator.getTesting();
        if(testing != null && Thread.currentThread() == testing.getThread()) {
            testing.setBlockState(pos, state);
            cir.setReturnValue(false);
        }
    }


}
