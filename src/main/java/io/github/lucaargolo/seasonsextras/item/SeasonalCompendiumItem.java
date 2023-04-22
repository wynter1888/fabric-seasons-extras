package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class SeasonalCompendiumItem extends Item {

    public SeasonalCompendiumItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(!world.isClient() && FabricLoader.getInstance().isModLoaded("patchouli")) {
            PatchouliAPI.get().openBookGUI((ServerPlayerEntity) user, FabricSeasonsExtras.SEASONAL_COMPENDIUM_ITEM_ID);
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return TypedActionResult.fail(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("patchouli.seasonsextras.description").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        if(!FabricLoader.getInstance().isModLoaded("patchouli")) {
            tooltip.add(Text.translatable("tooltip.seasonsextras.not_enabled").formatted(Formatting.RED, Formatting.ITALIC));
        }
    }

}
