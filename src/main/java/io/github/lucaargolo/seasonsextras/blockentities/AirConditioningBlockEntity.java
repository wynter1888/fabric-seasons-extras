package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class AirConditioningBlockEntity extends BlockEntity {

    private int progress = 0;
    private final SimpleInventory inputInventory = new SimpleInventory(9) {
        @Override
        public void markDirty() {
            super.markDirty();
            AirConditioningBlockEntity.this.markDirty();
        }
    };
    private final SimpleInventory moduleInventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            super.markDirty();
            AirConditioningBlockEntity.this.markDirty();
        }
    };
    private final Module[] modules = new Module[] {
        new Module(true, moduleInventory.getStack(0), 0, 0),
        new Module(false, moduleInventory.getStack(1), 0, 0),
        new Module(false, moduleInventory.getStack(2), 0, 0)
    };

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {

        @Override
        public int get(int index) {
            switch (index) {
                case 0 -> {
                    return AirConditioningBlockEntity.this.progress;
                }
                case 1 -> {
                    return AirConditioningBlockEntity.this.modules[0].enabled ? 1 : 0;
                }
                case 2 -> {
                    return AirConditioningBlockEntity.this.modules[0].burnTime;
                }
                case 3 -> {
                    return AirConditioningBlockEntity.this.modules[0].burnTimeTotal;
                }
                case 4 -> {
                    return AirConditioningBlockEntity.this.modules[1].enabled ? 1 : 0;
                }
                case 5 -> {
                    return AirConditioningBlockEntity.this.modules[1].burnTime;
                }
                case 6 -> {
                    return AirConditioningBlockEntity.this.modules[1].burnTimeTotal;
                }
                case 7 -> {
                    return AirConditioningBlockEntity.this.modules[2].enabled ? 1: 0;
                }
                case 8 -> {
                    return AirConditioningBlockEntity.this.modules[2].burnTime;
                }
                case 9 -> {
                    return AirConditioningBlockEntity.this.modules[2].burnTimeTotal;
                }
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    AirConditioningBlockEntity.this.progress = value;
                }
                case 1 -> {
                    AirConditioningBlockEntity.this.modules[0].burnTime = value;
                }
                case 2 -> {
                    AirConditioningBlockEntity.this.modules[0].burnTimeTotal = value;
                }
                case 3 -> {
                    AirConditioningBlockEntity.this.modules[1].burnTime = value;
                }
                case 4 -> {
                    AirConditioningBlockEntity.this.modules[1].burnTimeTotal = value;
                }
                case 5 -> {
                    AirConditioningBlockEntity.this.modules[2].burnTime = value;
                }
                case 6 -> {
                    AirConditioningBlockEntity.this.modules[2].burnTimeTotal = value;
                }
            }
        }

        @Override
        public int size() {
            return 10;
        }
    };

    public AirConditioningBlockEntity(BlockPos pos, BlockState state) {
        super(FabricSeasonsExtras.AIR_CONDITIONING_TYPE, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("progress", progress);
        Inventories.writeNbt(nbt, inputInventory.stacks);
        for (int i = 0 ; i < modules.length; i++) {
            nbt.put("module_" + i, modules[i].writeNbt(new NbtCompound()));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        progress = nbt.getInt("progress");
        Inventories.readNbt(nbt, inputInventory.stacks);
        for (int i = 0 ; i < modules.length; i++) {
            modules[i].readNbt(nbt.getCompound("module_" + i));
        }
    }

    public PropertyDelegate getPropertyDelegate() {
        return this.propertyDelegate;
    }

    public SimpleInventory getInputInventory() {
        return this.inputInventory;
    }

    public SimpleInventory getModuleInventory() {
        return this.moduleInventory;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AirConditioningBlockEntity entity) {

        entity.progress += 4;
        int maxProgress = 28+13;
        if(entity.modules[2].enabled) {
            maxProgress = 64+13;
        }else if(entity.modules[1].enabled) {
            maxProgress = 46+13;
        }else if(!entity.modules[0].enabled) {
            maxProgress = 0;
        }
        if(entity.progress > maxProgress) {
            entity.progress = maxProgress;
        }

        for (int i = 0 ; i < entity.modules.length; i++) {
            Module module = entity.modules[i];
            if(module.enabled && entity.progress == 28+13+(18*i)) {
                if(module.stack.isEmpty()) {
                    module.stack = Items.COAL.getDefaultStack();
                }else{
                    module.stack.increment(1);
                }
            }
        }

        if(entity.progress == maxProgress) {
            entity.progress = 0;
        }

    }

    public static class Module {

        public boolean enabled;
        public ItemStack stack;
        public int burnTime;
        public int burnTimeTotal;

        public Module(boolean enabled, ItemStack stack, int burnTime, int burnTimeTotal) {
            this.enabled = enabled;
            this.stack = stack;
            this.burnTime = burnTime;
            this.burnTimeTotal = burnTimeTotal;
        }

        public void readNbt(NbtCompound nbt) {
            enabled = nbt.getBoolean("enabled");
            stack = ItemStack.fromNbt(nbt.getCompound("stack"));
            burnTime = nbt.getInt("burnTime");
            burnTimeTotal = nbt.getInt("burnTimeTotal");
        }

        public NbtCompound writeNbt(NbtCompound nbt) {
            nbt.putBoolean("enabled", enabled);
            nbt.put("stack", stack.writeNbt(new NbtCompound()));
            nbt.putInt("burnTime", burnTime);
            nbt.putInt("burnTimeTotal", burnTimeTotal);
            return nbt;
        }

    }

}
