package io.github.lucaargolo.seasonsextras.patchouli.page;

import io.github.lucaargolo.seasonsextras.patchouli.FabricSeasonsExtrasPatchouliCompatClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.World;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

import java.util.HashSet;

public class PageBiomeSearch extends PageSearch {

    protected String getSearchable() {
        return "biomes";
    };

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        searchable.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if(world != null) {
            RegistryKey<World> worldKey = world.getRegistryKey();
            FabricSeasonsExtrasPatchouliCompatClient.worldValidBiomes.getOrDefault(worldKey, new HashSet<>()).forEach(entry -> {
                entry.getKey().ifPresent(key -> {
                    Identifier id = key.getValue();
                    String name = Text.translatable(id.toTranslationKey("biome")).getString();
                    searchable.add(new Pair<>(id, name));
                });
            });
        }
        super.onDisplayed(parent, left, top);
    }

}