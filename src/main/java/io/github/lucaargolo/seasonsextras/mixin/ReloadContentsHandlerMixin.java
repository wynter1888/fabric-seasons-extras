package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.common.handler.ReloadContentsHandler;

@Mixin(value = ReloadContentsHandler.class, remap = false)
public class ReloadContentsHandlerMixin {

    @Inject(at = @At("HEAD"), method = "dataReloaded")
    private static void syncBookBiomes(MinecraftServer server, CallbackInfo ci) {
        FabricSeasonsExtras.sendValidBiomes(server, null);
    }

}
