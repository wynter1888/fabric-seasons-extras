package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.client.FabricSeasonsExtrasClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FabricSeasons.class, remap = false)
public class FabricSeasonsMixin {

    @Inject(at = @At("HEAD"), method = "getCurrentSeason()Lio/github/lucaargolo/seasons/utils/Season;", cancellable = true)
    private static void injectMultiblockSeason(CallbackInfoReturnable<Season> cir) {
        if(FabricSeasonsExtrasClient.multiblockSeasonOverride != null) {
            cir.setReturnValue(FabricSeasonsExtrasClient.multiblockSeasonOverride);
        }
    }

}
