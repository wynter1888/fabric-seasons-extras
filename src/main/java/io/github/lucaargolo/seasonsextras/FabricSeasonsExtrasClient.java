package io.github.lucaargolo.seasonsextras;

import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.mixin.GuiBookAccessor;
import io.github.lucaargolo.seasonsextras.mixin.GuiBookEntryAccessor;
import io.github.lucaargolo.seasonsextras.patchouli.PageBiomeSearch;
import io.github.lucaargolo.seasonsextras.patchouli.PageSeasonalBiomeMultiblock;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import vazkii.patchouli.client.book.ClientBookRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FabricSeasonsExtrasClient implements ClientModInitializer {

    public static RegistryKey<Biome> multiblockBiomeOverride = null;
    public static Season multiblockSeasonOverride = null;

    public static List<RegistryEntry<Biome>> validBiomes = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("biome_search"), PageBiomeSearch.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(new ModIdentifier("biome_multiblock"), PageSeasonalBiomeMultiblock.class);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.currentScreen == null) {
                FabricSeasonsExtrasClient.multiblockBiomeOverride = null;
                FabricSeasonsExtrasClient.multiblockSeasonOverride = null;
            }else{
                if(client.currentScreen instanceof GuiBookEntryAccessor bookEntry) {
                    if(bookEntry.getLeftPage() instanceof PageBiomeSearch page) {
                        page.tick();
                    }
                    if(bookEntry.getRightPage() instanceof PageBiomeSearch page) {
                        page.tick();
                    }
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricSeasonsExtras.SEND_VALID_BIOMES_S2C, (client, handler, buf, responseSender) -> {
            List<Identifier> biomes = new ArrayList<>();
            int size = buf.readInt();
            for(int i = 0; i < size; i++) {
               biomes.add(buf.readIdentifier());
            }
            client.execute(() -> {
                validBiomes.clear();
                biomes.stream().sorted(Comparator.comparing(Identifier::getPath)).forEach(biome -> {
                    handler.getRegistryManager().get(Registry.BIOME_KEY).getEntry(RegistryKey.of(Registry.BIOME_KEY, biome)).ifPresent(entry -> validBiomes.add(entry));
                });
            });
        });
        BlockRenderLayerMap.INSTANCE.putBlock(Registry.BLOCK.get(new ModIdentifier("greenhouse_glass")), RenderLayer.getTranslucent());
    }

}
