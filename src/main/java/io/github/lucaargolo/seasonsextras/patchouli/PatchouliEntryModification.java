package io.github.lucaargolo.seasonsextras.patchouli;

import com.google.gson.JsonArray;

public interface PatchouliEntryModification {

    void call(JsonArray pages, int index);

}
