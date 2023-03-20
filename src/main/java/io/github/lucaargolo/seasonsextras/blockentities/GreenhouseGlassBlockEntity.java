package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class GreenhouseGlassBlockEntity extends BlockEntity {

    private int age = 0;

    public GreenhouseGlassBlockEntity(BlockPos pos, BlockState state) {
        super(FabricSeasonsExtras.GREENHOUSE_GLASS_TYPE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, GreenhouseGlassBlockEntity entity) {
        //TODO: Improve this? and reference Expiration Time
        if(entity.age++ % 40 == 0) {
            GreenhouseCache.GreenHouseTicket ticket = new GreenhouseCache.GreenHouseTicket(pos.getX(), pos.getZ(), Integer.MIN_VALUE, pos.getY(), Season.SPRING);
            GreenhouseCache.add(world, new ChunkPos(pos), ticket);
        }
    }

}
