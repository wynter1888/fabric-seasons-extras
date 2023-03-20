package io.github.lucaargolo.seasonsextras;

import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.registry.Registry;

public class FabricSeasonsExtrasClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Registry.BLOCK.get(new ModIdentifier("greenhouse_glass")), RenderLayer.getTranslucent());
    }

}
