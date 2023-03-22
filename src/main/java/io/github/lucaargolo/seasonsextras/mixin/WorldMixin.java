package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(World.class)
public class WorldMixin {

    @Inject(at = @At("HEAD"), method = "getBlockState", cancellable = true)
    public void getTestingTreePos(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if(FabricSeasonsExtras.testingTree) {
            if(FabricSeasonsExtras.testingMap.containsKey(pos)) {
                cir.setReturnValue(FabricSeasonsExtras.testingMap.get(pos));
            }else if(pos.getY() == 99) {
                cir.setReturnValue(Blocks.GRASS_BLOCK.getDefaultState());
            }else{
                cir.setReturnValue(Blocks.AIR.getDefaultState());
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", cancellable = true)
    public void setTestingTreePos(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if(FabricSeasonsExtras.testingTree) {
            FabricSeasonsExtras.testingMap.put(pos.toImmutable(), state);
            cir.setReturnValue(false);
        }
    }


}
