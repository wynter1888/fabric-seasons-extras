package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.client.FabricSeasonsExtrasClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.common.multiblock.AbstractMultiblock;

import java.util.Optional;

@Mixin(value = AbstractMultiblock.class, remap = false)
public class AbstractMultiblockMixin {

    @Shadow
    World world;

    @Inject(at = @At("HEAD"), method = "getColor", cancellable = true)
    private void overrideMultiblockBiome(BlockPos pos, ColorResolver color, CallbackInfoReturnable<Integer> cir) {
        if(FabricSeasonsExtrasClient.multiblockBiomeOverride != null) {
            Optional<Biome> optional = world.getRegistryManager().get(Registry.BIOME_KEY).getOrEmpty(FabricSeasonsExtrasClient.multiblockBiomeOverride);
            optional.ifPresent(b -> cir.setReturnValue(color.getColor(b, pos.getX(), pos.getZ())));
        }
    }

}
