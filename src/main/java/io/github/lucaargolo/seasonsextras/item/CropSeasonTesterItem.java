package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CropSeasonTesterItem extends Item {

    public CropSeasonTesterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(!context.getWorld().isClient) {
            LinkedHashSet<Season> greenHouseSeasons = new LinkedHashSet<>();
            greenHouseSeasons.add(FabricSeasons.getCurrentSeason(context.getWorld()));
            greenHouseSeasons.addAll(GreenhouseCache.test(context.getWorld(), context.getBlockPos().up()));
            //TODO: Translate these
            MutableText posInfo = Text.literal("Crops planted on this block will grow at their max speed during these seasons: ");
            int i = 0;
            for(Season season : greenHouseSeasons) {
                posInfo.append(Text.translatable(season.getTranslationKey()).formatted(season.getDarkFormatting()));
                if(i == greenHouseSeasons.size()-1) {
                    posInfo.append(Text.literal(".").formatted(Formatting.RESET));
                }else if(i == greenHouseSeasons.size()-2) {
                    posInfo.append(Text.literal(" ").append(Text.translatable("patchouli.seasonsextras.and")).append(" ").formatted(Formatting.RESET));
                }else{
                    posInfo.append(Text.literal(", ").formatted(Formatting.RESET));
                }
                i++;
            }
            PlayerEntity player = context.getPlayer();
            if(player != null) {
                player.sendMessage(posInfo, false);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        //TODO: Add these
    }
}
