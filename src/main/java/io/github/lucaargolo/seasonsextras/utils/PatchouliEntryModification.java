package io.github.lucaargolo.seasonsextras.utils;

import com.google.gson.JsonArray;

public interface PatchouliEntryModification {

    void call(JsonArray pages, int index);

}
