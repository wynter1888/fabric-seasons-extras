package io.github.lucaargolo.seasonsextras.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreenHouseGlassItem extends BlockItem {


    public GreenHouseGlassItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        //TODO: Translate these
        tooltip.add(Text.literal("Slightly warms up crops planted under itself").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        tooltip.add(Text.literal("allowing some of them to grow out of season.").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
    }


}
