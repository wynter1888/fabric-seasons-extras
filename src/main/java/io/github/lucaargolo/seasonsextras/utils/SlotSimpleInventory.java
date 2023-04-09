package io.github.lucaargolo.seasonsextras.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class SlotSimpleInventory implements Inventory {

    private final SimpleInventory inventory;
    private final Set<Integer> slots;

    public SlotSimpleInventory(SimpleInventory inventory, int... slots) {
        this.inventory = inventory;
        this.slots = Arrays.stream(slots).boxed().collect(Collectors.toSet());
    }

    @Override
    public int size() {
        return inventory.size();
    }

    public boolean isEmpty() {
        Iterator<ItemStack> var1 = inventory.stacks.iterator();

        int index = 0;
        while (var1.hasNext()) {
            ItemStack stack = var1.next();
            if(slots.contains(index) && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slots.contains(slot) ? inventory.getStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return slots.contains(slot) ? inventory.removeStack(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return slots.contains(slot) ? inventory.removeStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(slots.contains(slot)) {
            inventory.setStack(slot, stack);
        }
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
        ListIterator<ItemStack> var1 = inventory.stacks.listIterator();

        int index = 0;
        while (var1.hasNext()){
            if (slots.contains(index)) {
                var1.set(ItemStack.EMPTY);
            }
            index++;
        }
    }
}
