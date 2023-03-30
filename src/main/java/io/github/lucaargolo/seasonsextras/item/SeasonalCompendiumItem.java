package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import vazkii.patchouli.api.PatchouliAPI;

public class SeasonalCompendiumItem extends Item {

    public SeasonalCompendiumItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(!world.isClient()) {
            PatchouliAPI.get().openBookGUI((ServerPlayerEntity) user, FabricSeasonsExtras.SEASONAL_COMPENDIUM_ITEM_ID);
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return TypedActionResult.fail(user.getStackInHand(hand));
    }


}
