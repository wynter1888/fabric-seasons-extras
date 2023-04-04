package io.github.lucaargolo.seasonsextras.patchouli.page;

import io.github.lucaargolo.seasons.FabricSeasons;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

import java.util.Comparator;
import java.util.Map;

public class PageCropSearch extends PageSearch {

    protected String getSearchable() {
        return "crops";
    };

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        searchable.clear();
        FabricSeasons.SEEDS_MAP.values().stream().map(crop -> Map.entry(crop, Registry.BLOCK.getId(crop))).sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            String name = Text.translatable(entry.getKey().getTranslationKey()).getString();
            searchable.add(new Pair<>(entry.getValue(), name));
        });
        super.onDisplayed(parent, left, top);
    }

}