package io.github.lucaargolo.seasonsextras.block;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonCalendarBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class SeasonCalendarBlock extends BlockWithEntity {

    public static final EnumProperty<Season> SEASON = EnumProperty.of("season", Season.class);

    public static final IntProperty PROGRESS = IntProperty.of("progress", 0, 15);

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(15, 3, 3, 16, 12, 13);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(3, 3, 0, 13, 12, 1);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(3, 3, 15, 13, 12, 16);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0, 3, 3, 1, 12, 13);

    public SeasonCalendarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(SEASON, Season.SPRING).with(PROGRESS, 0).with(FACING, Direction.SOUTH));
    }

    @Override
    public SeasonCalendarBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SeasonCalendarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, FabricSeasonsExtras.SEASON_CALENDAR_TYPE, SeasonCalendarBlockEntity::serverTick);
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        long timeToNextSeason = (FabricSeasons.CONFIG.getSeasonLength() - (world.getTimeOfDay() - ((world.getTimeOfDay()/FabricSeasons.CONFIG.getSeasonLength())*FabricSeasons.CONFIG.getSeasonLength()) )) % FabricSeasons.CONFIG.getSeasonLength();
        double progressLeft = timeToNextSeason / (double) FabricSeasons.CONFIG.getSeasonLength();
        int currentProgress = (int) (16.0 - (progressLeft*16.0));
        Season currentSeason = FabricSeasons.getCurrentSeason(world);
        Direction facing = ctx.getSide().getAxis() != Direction.Axis.Y ? ctx.getSide().getOpposite() : ctx.getHorizontalPlayerFacing();
        return this.getDefaultState().with(SEASON, currentSeason).with(PROGRESS, currentProgress).with(FACING, facing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos facingPos = pos.offset(facing);
        BlockState facingState = world.getBlockState(facingPos);
        return facingState.isFullCube(world, facingPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public static void updateState(BlockState state, World world, BlockPos pos) {
        long timeToNextSeason = (FabricSeasons.CONFIG.getSeasonLength() - (world.getTimeOfDay() - ((world.getTimeOfDay()/FabricSeasons.CONFIG.getSeasonLength())*FabricSeasons.CONFIG.getSeasonLength()) )) % FabricSeasons.CONFIG.getSeasonLength();
        double progressLeft = timeToNextSeason / (double) FabricSeasons.CONFIG.getSeasonLength();

        int stateProgress = state.get(PROGRESS);
        int currentProgress = (int) (16.0 - (progressLeft*16.0));

        Season stateSeason = state.get(SEASON);
        Season currentSeason = FabricSeasons.getCurrentSeason(world);

        if(currentSeason != stateSeason) {
            world.setBlockState(pos, state.with(SEASON, currentSeason).with(PROGRESS, currentProgress), 3);
        }else if(stateProgress != currentProgress) {
            world.setBlockState(pos, state.with(PROGRESS, currentProgress), 3);
        }

    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SEASON);
        builder.add(PROGRESS);
        builder.add(FACING);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACING)) {
            case EAST -> {
                return EAST_SHAPE;
            }
            case NORTH -> {
                return NORTH_SHAPE;
            }
            case WEST -> {
                return WEST_SHAPE;
            }
            default -> {
                return SOUTH_SHAPE;
            }
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
