package io.github.lucaargolo.seasonsextras.utils;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.util.Identifier;

public class ModIdentifier extends Identifier {

    public ModIdentifier(String path) {
        super(FabricSeasonsExtras.MOD_ID, path);
    }

}
