package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeasonDetectorItem extends BlockItem {


    public SeasonDetectorItem(SeasonDetectorBlock block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.seasonsextras.detector_1").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        tooltip.add(Text.translatable("tooltip.seasonsextras.detector_2").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
    }


}
