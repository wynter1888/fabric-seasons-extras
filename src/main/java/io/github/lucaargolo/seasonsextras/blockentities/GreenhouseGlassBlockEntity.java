package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class GreenhouseGlassBlockEntity extends BlockEntity {
    private GreenhouseCache.GreenHouseTicket ticket = null;

    public GreenhouseGlassBlockEntity(BlockPos pos, BlockState state) {
        super(FabricSeasonsExtras.GREENHOUSE_GLASS_TYPE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, GreenhouseGlassBlockEntity entity) {
        Season worldSeason = FabricSeasons.getCurrentSeason(world);
        Season glassSeason = Season.SUMMER;
        switch (worldSeason) {
            case FALL -> glassSeason = Season.SPRING;
            case WINTER -> glassSeason = Season.FALL;
        }
        if(entity.ticket == null || entity.ticket.expired || !entity.ticket.seasons.contains(glassSeason)) {
            BlockBox box = BlockBox.create(pos.withY(Integer.MIN_VALUE), pos);
            entity.ticket = new GreenhouseCache.GreenHouseTicket(box, glassSeason);
            GreenhouseCache.add(world, new ChunkPos(pos), entity.ticket);
        }else{
            //If the greenhouse glass is removed / its season get changed, the ticket will stop updating and will be removed when tested.
            entity.ticket.age++;
        }
    }

}
