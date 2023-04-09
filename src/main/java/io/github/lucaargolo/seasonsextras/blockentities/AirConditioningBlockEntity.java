package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.utils.SlotSimpleInventory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class AirConditioningBlockEntity extends BlockEntity {

    private int lastProgress = 0;
    private int progress = 0;
    private boolean halt = false;
    private Conditioning conditioning;

    private final SimpleInventory inputInventory = new SimpleInventory(9) {
        @Override
        public void markDirty() {
            super.markDirty();
            AirConditioningBlockEntity.this.halt = false;
            AirConditioningBlockEntity.this.markDirty();
        }
    };

    private final SimpleInventory moduleInventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            super.markDirty();
            AirConditioningBlockEntity.this.halt = false;
            AirConditioningBlockEntity.this.markDirty();
        }
    };


    private final Module[] modules = new Module[] {
        new Module(true, 0, 0),
        new Module(false, 0, 0),
        new Module(false, 0, 0)
    };

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {

        @Override
        public int get(int index) {
            switch (index) {
                case 0 -> {
                    return AirConditioningBlockEntity.this.progress;
                }
                case 1 -> {
                    return AirConditioningBlockEntity.this.conditioning.ordinal();
                }
                case 2 -> {
                    return AirConditioningBlockEntity.this.modules[0].enabled ? 1 : 0;
                }
                case 3 -> {
                    return AirConditioningBlockEntity.this.modules[0].burnTime;
                }
                case 4 -> {
                    return AirConditioningBlockEntity.this.modules[0].burnTimeTotal;
                }
                case 5 -> {
                    return AirConditioningBlockEntity.this.modules[1].enabled ? 1 : 0;
                }
                case 6 -> {
                    return AirConditioningBlockEntity.this.modules[1].burnTime;
                }
                case 7 -> {
                    return AirConditioningBlockEntity.this.modules[1].burnTimeTotal;
                }
                case 8 -> {
                    return AirConditioningBlockEntity.this.modules[2].enabled ? 1: 0;
                }
                case 9 -> {
                    return AirConditioningBlockEntity.this.modules[2].burnTime;
                }
                case 10 -> {
                    return AirConditioningBlockEntity.this.modules[2].burnTimeTotal;
                }
            }
            return 0;
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int size() {
            return 11;
        }
    };

    public AirConditioningBlockEntity(BlockPos pos, BlockState state, Conditioning conditioning) {
        super(FabricSeasonsExtras.AIR_CONDITIONING_TYPE, pos, state);
        this.conditioning = conditioning;
    }

    public AirConditioningBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, Conditioning.HEATER);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("conditioning", conditioning.ordinal());
        nbt.putInt("progress", progress);
        NbtCompound inputInventoryNbt = new NbtCompound();
        Inventories.writeNbt(inputInventoryNbt, inputInventory.stacks);
        nbt.put("inputInventory", inputInventoryNbt);
        NbtCompound moduleInventoryNbt = new NbtCompound();
        Inventories.writeNbt(moduleInventoryNbt, moduleInventory.stacks);
        nbt.put("moduleInventory", moduleInventoryNbt);
        for (int i = 0 ; i < modules.length; i++) {
            nbt.put("module_" + i, modules[i].writeNbt(new NbtCompound()));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        conditioning = Conditioning.values()[Math.max(0, Math.min(Conditioning.values().length, nbt.getInt("conditioning")))];
        progress = nbt.getInt("progress");
        Inventories.readNbt(nbt.getCompound("inputInventory"), inputInventory.stacks);
        Inventories.readNbt(nbt.getCompound("moduleInventory"), moduleInventory.stacks);
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

    public void cycleModule(int module) {
        modules[module].enabled = !modules[module].enabled;
        markDirty();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void serverTick(World world, BlockPos pos, BlockState state, AirConditioningBlockEntity entity) {

        if(entity.halt) {
            entity.progress = 0;
            entity.lastProgress = 0;
            return;
        }

        Storage<ItemVariant> inputStorage = InventoryStorage.of(entity.inputInventory, null);

        Predicate<ItemVariant> filter = itemVariant -> {
            ItemStack stack = itemVariant.toStack();
            //TODO: Fix this
            return AbstractFurnaceBlockEntity.canUseAsFuel(stack);
        };

        ItemVariant variant;
        try(Transaction transaction = Transaction.openOuter()) {
            variant = StorageUtil.findExtractableResource(inputStorage, filter, transaction);
            if (variant == null || variant.isBlank()) {
                entity.halt = true;
            }
            transaction.commit();
        }

        int maxProgress = getMaxProgress(entity.modules);
        if(entity.progress >= maxProgress) {
            entity.progress = 0;
        }else if(entity.progress + 3 > maxProgress) {
            entity.progress = maxProgress;
        }else{
            entity.progress += 3;
        }

        for (int i = 0 ; i < entity.modules.length; i++) {
            Module module = entity.modules[i];
            int amount = 28+13+(18*i);
            if(module.enabled && entity.progress >= amount && entity.lastProgress < amount) {
                Storage<ItemVariant> moduleStorage = InventoryStorage.of(new SlotSimpleInventory(entity.moduleInventory, i), null);

                try(Transaction transaction = Transaction.openOuter()) {
                    StorageUtil.move(inputStorage, moduleStorage, filter, 1, transaction);
                    variant = StorageUtil.findExtractableResource(inputStorage, filter, transaction);
                    if (variant == null || variant.isBlank()) {
                        entity.halt = true;
                    }
                    transaction.commit();
                }
            }
        }

        entity.lastProgress = entity.progress;

    }

    public static int getMaxProgress(Module[] modules) {
        int maxProgress = 28+13;
        if(modules[2].enabled) {
            maxProgress = 64+13;
        }else if(modules[1].enabled) {
            maxProgress = 46+13;
        }else if(!modules[0].enabled) {
            maxProgress = 0;
        }
        return maxProgress;
    }

    public static class Module {

        public boolean enabled;
        public int burnTime;
        public int burnTimeTotal;

        public Module(boolean enabled, int burnTime, int burnTimeTotal) {
            this.enabled = enabled;
            this.burnTime = burnTime;
            this.burnTimeTotal = burnTimeTotal;
        }

        public void readNbt(NbtCompound nbt) {
            enabled = nbt.getBoolean("enabled");
            burnTime = nbt.getInt("burnTime");
            burnTimeTotal = nbt.getInt("burnTimeTotal");
        }

        public NbtCompound writeNbt(NbtCompound nbt) {
            nbt.putBoolean("enabled", enabled);
            nbt.putInt("burnTime", burnTime);
            nbt.putInt("burnTimeTotal", burnTimeTotal);
            return nbt;
        }

    }

    public enum Conditioning {
        HEATER,
        CHILLER;
    }

}
