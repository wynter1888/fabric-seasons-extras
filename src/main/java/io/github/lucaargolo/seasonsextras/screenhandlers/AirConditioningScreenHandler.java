package io.github.lucaargolo.seasonsextras.screenhandlers;


import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.FurnaceFuelSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;

public class AirConditioningScreenHandler extends ScreenHandler {

    private final Inventory inputInventory;
    private final Inventory moduleInventory;
    private final PropertyDelegate propertyDelegate;
    private final ScreenHandlerContext context;

    private final Block block;

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block) {
        this(syncId, playerInventory, context, block, new SimpleInventory(9), new SimpleInventory(3), new ArrayPropertyDelegate(10));
    }

    public AirConditioningScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, Block block, Inventory inputInventory, Inventory moduleInventory, PropertyDelegate propertyDelegate) {
        super(FabricSeasonsExtras.AIR_CONDITIONING_SCREEN_HANDLER, syncId);
        int i;
        this.context = context;
        this.block = block;
        AbstractFurnaceScreenHandler.checkSize(inputInventory, 9);
        AbstractFurnaceScreenHandler.checkSize(moduleInventory, 3);
        AbstractFurnaceScreenHandler.checkDataCount(propertyDelegate, 10);
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

    public int getProgress() {
        return propertyDelegate.get(0);
    }

    public AirConditioningBlockEntity.Module getModule(int index) {
        int i = (index * 3) + 1;
        return new AirConditioningBlockEntity.Module(propertyDelegate.get(i) != 0, moduleInventory.getStack(index), propertyDelegate.get(i+1), propertyDelegate.get(i+2));
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
