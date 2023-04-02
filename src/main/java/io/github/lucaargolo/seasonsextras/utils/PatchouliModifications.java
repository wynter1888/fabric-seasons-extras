package io.github.lucaargolo.seasonsextras.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PatchouliModifications {

    private static final Identifier MODIFIABLE_ID = new ModIdentifier("modifiable");
    private static final HashMap<Identifier, List<Pair<Identifier, PatchouliEntryModification>>> entryModifications = new HashMap<>();

    public static void registerEntry(Identifier id, Identifier modId, PatchouliEntryModification modification) {
        entryModifications.computeIfAbsent(id, i -> new ArrayList<>()).add(new Pair<>(modId, modification));
    }

    public static void applyEntries(Identifier id, JsonArray pages) {
        List<Pair<Identifier, PatchouliEntryModification>> modifications = entryModifications.get(id);
        if(modifications != null) {
            for(Pair<Identifier, PatchouliEntryModification> m : modifications) {
                Identifier modId = m.getLeft();
                PatchouliEntryModification modification = m.getRight();
                applyEntry(pages, modId, modification);
            }
        }
    }

    private static void applyEntry(JsonArray pages, Identifier modId, PatchouliEntryModification modification) {
        boolean found = false;
        int i;
        for(i = 0; i < pages.size(); i++) {
            JsonElement element = pages.get(i);
            if(element.isJsonObject()) {
                JsonObject page = element.getAsJsonObject();
                JsonElement type = page.get("type");
                JsonElement modify = page.get("modify");
                if(type != null && type.isJsonPrimitive() && type.getAsString().equals(MODIFIABLE_ID.toString()) && modify != null && modify.isJsonPrimitive() && modify.getAsString().equals(modId.toString())) {
                    found = true;
                    pages.remove(i);
                    break;
                }
            }
        }
        if(found) {
            modification.call(pages, i);
            applyEntry(pages, modId, modification);
        }
    }


}
