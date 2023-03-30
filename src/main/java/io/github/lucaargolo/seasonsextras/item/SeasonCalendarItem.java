package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class SeasonCalendarItem extends BlockItem {


    public SeasonCalendarItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if(world != null) {
            appendCalendarTooltip(world, tooltip);
        }
    }

    public static void appendCalendarTooltip(World world, List<Text> tooltip) {
        Season season = FabricSeasons.getCurrentSeason(world);
        int seasonLength = FabricSeasons.CONFIG.getSeasonLength();
        tooltip.add(Text.translatable("tooltip.seasonsextras.calendar_info_1").formatted(season.getFormatting()).append(Text.translatable(season.getTranslationKey()).formatted(season.getFormatting())).formatted(Formatting.UNDERLINE));
        if(!FabricSeasons.CONFIG.isSeasonLocked() && !FabricSeasons.CONFIG.isSeasonTiedWithSystemTime())
            tooltip.add(Text.literal(Long.toString(((seasonLength - (world.getTimeOfDay() - ((world.getTimeOfDay()/seasonLength)*seasonLength) )) % seasonLength)/24000L)).append(Text.translatable("tooltip.seasonsextras.calendar_info_2").formatted(Formatting.GRAY).append(Text.translatable("tooltip.seasons."+season.getNext().name().toLowerCase(Locale.ROOT)).formatted(season.getNext().getFormatting()))));
    }

}
