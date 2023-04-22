package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class AirConditioningItem extends BlockItem {

    private final AirConditioningBlock airConditioningBlock;

    public AirConditioningItem(AirConditioningBlock block, Settings settings) {
        super(block, settings);
        this.airConditioningBlock = block;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        String name = airConditioningBlock.getConditioning().name().toLowerCase(Locale.ROOT);
        tooltip.add(Text.translatable("tooltip.seasonsextras."+name+"_1").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        tooltip.add(Text.translatable("tooltip.seasonsextras."+name+"_2").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        if(!FabricSeasons.CONFIG.isSeasonMessingCrops()) {
            tooltip.add(Text.translatable("tooltip.seasonsextras.not_enabled").formatted(Formatting.RED, Formatting.ITALIC));
        }
    }

}
