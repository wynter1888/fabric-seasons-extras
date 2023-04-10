package io.github.lucaargolo.seasonsextras.block;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity.Conditioning;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class AirConditioningBlock extends BlockWithEntity {

    public static final Property<Integer> LEVEL = IntProperty.of("level", 0, 3);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private final Conditioning conditioning;

    public AirConditioningBlock(Conditioning conditioning, Settings settings) {
        super(settings);
        this.conditioning = conditioning;
        this.setDefaultState(this.getDefaultState().with(LEVEL, 0).with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LEVEL);
        builder.add(FACING);
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getSide().getAxis() != Direction.Axis.Y ? ctx.getSide() : ctx.getPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.isOf(newState.getBlock())) {
            return;
        }
        Optional<AirConditioningBlockEntity> optional = world.getBlockEntity(pos, FabricSeasonsExtras.AIR_CONDITIONING_TYPE);
        if (optional.isPresent()) {
            ItemScatterer.spawn(world, pos, optional.get().getInputInventory());
            ItemScatterer.spawn(world, pos, optional.get().getModuleInventory());
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient) {
            return ActionResult.SUCCESS;
        }else{
            Optional<AirConditioningBlockEntity> optional = world.getBlockEntity(pos, FabricSeasonsExtras.AIR_CONDITIONING_TYPE);
            if (optional.isPresent()) {
                AirConditioningBlockEntity blockEntity = optional.get();
                player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                    @Override
                    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                        buf.writeBlockPos(pos);
                        buf.writeRegistryValue(Registry.BLOCK, AirConditioningBlock.this);
                    }

                    @Override
                    public Text getDisplayName() {
                        return AirConditioningBlock.this.getName();
                    }

                    @Override
                    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                        return new AirConditioningScreenHandler(syncId, inv, ScreenHandlerContext.create(player.world, pos), AirConditioningBlock.this, blockEntity.getInputInventory(), blockEntity.getModuleInventory(), blockEntity.getPropertyDelegate());
                    }
                });
                return ActionResult.CONSUME;
            }else {
                return ActionResult.FAIL;
            }
        }
    }

    @Override
    public AirConditioningBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AirConditioningBlockEntity(pos, state, conditioning);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, FabricSeasonsExtras.AIR_CONDITIONING_TYPE, AirConditioningBlockEntity::serverTick);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        Optional<AirConditioningBlockEntity> optional = world.getBlockEntity(pos, FabricSeasonsExtras.AIR_CONDITIONING_TYPE);
        return optional.map(blockEntity -> ScreenHandler.calculateComparatorOutput(blockEntity.getInputInventory())).orElse(0);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

}
