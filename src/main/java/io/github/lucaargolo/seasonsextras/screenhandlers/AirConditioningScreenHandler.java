package io.github.lucaargolo.seasonsextras.screenhandlers;


import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

import java.util.Optional;

public class AirConditioningScreenHandler extends ScreenHandler {

    private final Inventory inputInventory;
    private final Inventory moduleInventory;
    private final PropertyDelegate propertyDelegate;
    private final ScreenHandlerContext context;

    private final Block block;

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block) {
        this(syncId, playerInventory, context, block, new SimpleInventory(9), new SimpleInventory(3), new ArrayPropertyDelegate(11));
    }

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block, Inventory inputInventory, Inventory moduleInventory, PropertyDelegate propertyDelegate) {
        super(FabricSeasonsExtras.AIR_CONDITIONING_SCREEN_HANDLER, syncId);
        int i;
        this.context = context;
        this.block = block;
        AbstractFurnaceScreenHandler.checkSize(inputInventory, 9);
        AbstractFurnaceScreenHandler.checkSize(moduleInventory, 3);
        AbstractFurnaceScreenHandler.checkDataCount(propertyDelegate, 11);
        this.inputInventory = inputInventory;
        this.moduleInventory = moduleInventory;
        this.propertyDelegate = propertyDelegate;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(inputInventory, j + i * 3, 26 + j * 18, 17 + i * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            this.addSlot(new Slot(moduleInventory, i, 98 + i * 18, 35) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return false;
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.addProperties(propertyDelegate);
    }

    public AirConditioningBlockEntity.Conditioning getConditioning() {
        return AirConditioningBlockEntity.Conditioning.values()[Math.max(0, Math.min(AirConditioningBlockEntity.Conditioning.values().length, propertyDelegate.get(1)))];
    }

    public int getProgress() {
        return propertyDelegate.get(0);
    }

    public AirConditioningBlockEntity.Module[] getModules() {
        AirConditioningBlockEntity.Module[] modules = new AirConditioningBlockEntity.Module[3];
        for(int index = 0; index < 3; index++) {
            int i = (index * 3) + 2;
            modules[index] = new AirConditioningBlockEntity.Module(propertyDelegate.get(i) != 0, moduleInventory.getStack(index), propertyDelegate.get(i+1), propertyDelegate.get(i+2));
        }
        return modules;
    }

    public void cycleModule(int module) {
        context.run((world, blockPos) -> {
            Optional<AirConditioningBlockEntity> optional = world.getBlockEntity(blockPos, FabricSeasonsExtras.AIR_CONDITIONING_TYPE);
            if(optional.isPresent()) {
                AirConditioningBlockEntity blockEntity = optional.get();
                blockEntity.cycleModule(module);
            }
        });
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return AirConditioningScreenHandler.canUse(context, player, block);
    }

}
