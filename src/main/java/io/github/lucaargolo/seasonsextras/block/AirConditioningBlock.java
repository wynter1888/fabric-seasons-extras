package io.github.lucaargolo.seasonsextras.block;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class AirConditioningBlock extends BlockWithEntity {

    private final AirConditioningBlockEntity.Conditioning conditioning;

    public AirConditioningBlock(AirConditioningBlockEntity.Conditioning conditioning, Settings settings) {
        super(settings);
        this.conditioning = conditioning;
    }

    @Override
    public AirConditioningBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AirConditioningBlockEntity(pos, state, conditioning);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, FabricSeasonsExtras.AIR_CONDITIONING_TYPE, AirConditioningBlockEntity::serverTick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

}
