package io.github.lucaargolo.seasonsextras.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class SlotSimpleInventory implements Inventory {

    private final SimpleInventory inventory;
    private final int[] slots;

    public SlotSimpleInventory(SimpleInventory inventory, int... slots) {
        this.inventory = inventory;
        this.slots = slots;
    }

    @Override
    public int size() {
        return slots.length;
    }

    public boolean isEmpty() {
        for (int slot : slots) {
            ItemStack stack = getStack(slot);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slots[slot]);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return inventory.removeStack(slots[slot], amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return inventory.removeStack(slots[slot]);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slots[slot], stack);
    }

    @Override
    public void markDirty() {
        inventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        for (int slot : slots) {
            setStack(slot, ItemStack.EMPTY);
        }
    }
}
