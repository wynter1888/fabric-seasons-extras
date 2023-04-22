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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import net.minecraft.util.math.random.Random;
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

    public Conditioning getConditioning() {
        return conditioning;
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
                        buf.writeRegistryValue(Registries.BLOCK, AirConditioningBlock.this);
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
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int level = state.get(LEVEL);
        if(level > 0) {
            if (random.nextInt(10) == 0) {
                world.playSound((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
            }
            Direction direction = state.get(AirConditioningBlock.FACING);
            Direction.Axis axis = direction.getAxis();
            double h = random.nextDouble() * 0.6 - 0.3;
            double i = axis == Direction.Axis.X ? (double)direction.getOffsetX() * 0.52 : h;
            double j = random.nextDouble() * 6.0 / 16.0;
            double k = axis == Direction.Axis.Z ? (double)direction.getOffsetZ() * 0.52 : h;
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + i, pos.getY() + j, pos.getZ() + 0.5 + k, 0.0, 0.0, 0.0);
            world.addParticle(conditioning.getParticle(), pos.getX() + 0.5 + i, pos.getY() + j, pos.getZ() + 0.5 + k, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public AirConditioningBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AirConditioningBlockEntity(pos, state, conditioning);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, FabricSeasonsExtras.AIR_CONDITIONING_TYPE, world.isClient ? AirConditioningBlockEntity::clientTick : AirConditioningBlockEntity::serverTick);
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
