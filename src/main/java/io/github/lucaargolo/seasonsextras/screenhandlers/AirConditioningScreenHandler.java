package io.github.lucaargolo.seasonsextras.screenhandlers;


import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

import java.util.Optional;

public class AirConditioningScreenHandler extends ScreenHandler {

    private final PropertyDelegate propertyDelegate;
    private final ScreenHandlerContext context;

    private final Block block;

    private final Inventory moduleInventory;

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block) {
        this(syncId, playerInventory, context, block, new SimpleInventory(9), new SimpleInventory(3), new ArrayPropertyDelegate(12));
    }

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block, Inventory inputInventory, Inventory moduleInventory, PropertyDelegate propertyDelegate) {
        super(FabricSeasonsExtras.AIR_CONDITIONING_SCREEN_HANDLER, syncId);
        int i;
        this.context = context;
        this.block = block;
        AbstractFurnaceScreenHandler.checkSize(inputInventory, 9);
        AbstractFurnaceScreenHandler.checkSize(moduleInventory, 3);
        AbstractFurnaceScreenHandler.checkDataCount(propertyDelegate, 12);
        this.propertyDelegate = propertyDelegate;
        this.moduleInventory = moduleInventory;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new AirConditioningSlot(inputInventory, j + i * 3, 26 + j * 18, 17 + i * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            this.addSlot(new AirConditioningSlot(moduleInventory, i, 98 + i * 18, 35));
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

    public int getProgress() {
        return propertyDelegate.get(0);
    }

    public Conditioning getConditioning() {
        return Conditioning.values()[Math.max(0, Math.min(Conditioning.values().length, propertyDelegate.get(1)))];
    }

    public int getLevel() {
        return context.get((world, blockPos) -> {
            BlockState state = world.getBlockState(blockPos);
            if(state.contains(AirConditioningBlock.LEVEL)) {
                return state.get(AirConditioningBlock.LEVEL);
            }
            return 0;
        }).orElse(0);
    }

    public boolean hasParticles() {
        return propertyDelegate.get(2) != 0;
    }

    public BurnSlot[] getBurnSlots() {
        return getBurnSlots(moduleInventory, propertyDelegate);
    }

    public static BurnSlot[] getBurnSlots(Inventory moduleInventory, PropertyDelegate propertyDelegate) {
        BurnSlot[] burnSlots = new BurnSlot[3];
        for(int index = 0; index < 3; index++) {
            int i = (index * 3) + 3;
            ItemStack stack = moduleInventory.getStack(index);
            boolean slotEmpty = stack.isEmpty();
            boolean slotNotFull = stack.getCount() < stack.getMaxCount() && stack.getCount() < moduleInventory.getMaxCountPerStack();
            burnSlots[index] = new BurnSlot(propertyDelegate.get(i) != 0, !slotEmpty && !slotNotFull,  propertyDelegate.get(i+1), propertyDelegate.get(i+2));
        }
        return burnSlots;
    }

    public static int getMaxProgress(BurnSlot[] burnSlots) {
        int maxProgress = 28+13;
        if(burnSlots[2].enabled && !burnSlots[2].full) {
            maxProgress = 64+13;
        }else if(burnSlots[1].enabled && !burnSlots[1].full) {
            maxProgress = 46+13;
        }else if(!burnSlots[0].enabled || burnSlots[0].full) {
            maxProgress = 0;
        }
        return maxProgress;
    }

    public void cycleButton(int button) {
        context.run((world, blockPos) -> {
            Optional<AirConditioningBlockEntity> optional = world.getBlockEntity(blockPos, FabricSeasonsExtras.AIR_CONDITIONING_TYPE);
            if(optional.isPresent()) {
                AirConditioningBlockEntity blockEntity = optional.get();
                blockEntity.cycleButton(button);
            }
        });
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index < 12 ? !this.insertItem(itemStack2, 12, 48, true) : !this.insertItem(itemStack2, 0, 12, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, itemStack2);
        }
        return itemStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return AirConditioningScreenHandler.canUse(context, player, block);
    }

    public class AirConditioningSlot extends Slot {

        public AirConditioningSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        public boolean canInsert(ItemStack stack) {
            Conditioning conditioning = AirConditioningScreenHandler.this.getConditioning();
            return conditioning.getFilter().test(ItemVariant.of(stack));
        }
    }


}
