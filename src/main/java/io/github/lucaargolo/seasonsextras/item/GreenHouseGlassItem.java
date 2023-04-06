package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreenHouseGlassItem extends BlockItem {

    private final GreenhouseGlassBlock greenhouseGlassBlock;

    public GreenHouseGlassItem(GreenhouseGlassBlock block, Settings settings) {
        super(block, settings);
        this.greenhouseGlassBlock = block;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        //TODO: Translate these
        if(greenhouseGlassBlock.inverted) {
            tooltip.add(Text.literal("Slightly colds up crops planted under itself").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
            tooltip.add(Text.literal("allowing some of them to grow out of season.").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        }else{
            tooltip.add(Text.literal("Slightly warms up crops planted under itself").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
            tooltip.add(Text.literal("allowing some of them to grow out of season.").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        }

    }


}
