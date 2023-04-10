package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.utils.SlotSimpleInventory;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class AirConditioningBlockEntity extends BlockEntity {

    private GreenhouseCache.GreenHouseTicket ticket = null;

    private int lastProgress = 0;
    private int progress = 0;

    private int level = 0;
    private boolean halt = false;
    private Conditioning conditioning;

    private final SimpleInventory inputInventory = new FilteredSimpleInventory(9);
    private final SimpleInventory moduleInventory = new FilteredSimpleInventory(3);

    private final BurnSlot[] burnSlots = new BurnSlot[] {
        new BurnSlot(true, 0, 0),
        new BurnSlot(false, 0, 0),
        new BurnSlot(false, 0, 0)
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
                    return AirConditioningBlockEntity.this.level;
                }
                case 3 -> {
                    return AirConditioningBlockEntity.this.burnSlots[0].enabled ? 1 : 0;
                }
                case 4 -> {
                    return AirConditioningBlockEntity.this.burnSlots[0].burnTime;
                }
                case 5 -> {
                    return AirConditioningBlockEntity.this.burnSlots[0].burnTimeTotal;
                }
                case 6 -> {
                    return AirConditioningBlockEntity.this.burnSlots[1].enabled ? 1 : 0;
                }
                case 7 -> {
                    return AirConditioningBlockEntity.this.burnSlots[1].burnTime;
                }
                case 8 -> {
                    return AirConditioningBlockEntity.this.burnSlots[1].burnTimeTotal;
                }
                case 9 -> {
                    return AirConditioningBlockEntity.this.burnSlots[2].enabled ? 1: 0;
                }
                case 10 -> {
                    return AirConditioningBlockEntity.this.burnSlots[2].burnTime;
                }
                case 11 -> {
                    return AirConditioningBlockEntity.this.burnSlots[2].burnTimeTotal;
                }
            }
            return 0;
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int size() {
            return 12;
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
        for (int i = 0; i < burnSlots.length; i++) {
            nbt.put("module_" + i, burnSlots[i].writeNbt(new NbtCompound()));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        conditioning = Conditioning.values()[Math.max(0, Math.min(Conditioning.values().length, nbt.getInt("conditioning")))];
        progress = nbt.getInt("progress");
        Inventories.readNbt(nbt.getCompound("inputInventory"), inputInventory.stacks);
        Inventories.readNbt(nbt.getCompound("moduleInventory"), moduleInventory.stacks);
        for (int i = 0; i < burnSlots.length; i++) {
            burnSlots[i].readNbt(nbt.getCompound("module_" + i));
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
        burnSlots[module].enabled = !burnSlots[module].enabled;
        markDirty();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void updateHalt(Storage<ItemVariant> inputStorage, Predicate<ItemVariant> filter) {
        ItemVariant variant;
        try(Transaction transaction = Transaction.openOuter()) {
            variant = StorageUtil.findExtractableResource(inputStorage, filter, transaction);
            if (variant == null || variant.isBlank()) {
                halt = true;
            }
            transaction.commit();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void serverTick(World world, BlockPos pos, BlockState state, AirConditioningBlockEntity entity) {
        Storage<ItemVariant> inputStorage = InventoryStorage.of(entity.inputInventory, null);
        Predicate<ItemVariant> filter = entity.conditioning.getFilter();
        entity.updateHalt(inputStorage, filter);

        int maxProgress = getMaxProgress(entity.burnSlots);
        if(entity.halt || entity.progress >= maxProgress) {
            entity.progress = 0;
        }else if(entity.progress + 3 > maxProgress) {
            entity.progress = maxProgress;
        }else{
            entity.progress += 3;
        }

        entity.level = 0;
        for (int i = 0; i < entity.burnSlots.length; i++) {
            BurnSlot burnSlot = entity.burnSlots[i];
            int amount = 28+13+(18*i);
            Storage<ItemVariant> moduleStorage = InventoryStorage.of(new SlotSimpleInventory(entity.moduleInventory, i), null);
            if(burnSlot.enabled && entity.progress >= amount && entity.lastProgress < amount) {
                try(Transaction transaction = Transaction.openOuter()) {
                    StorageUtil.move(inputStorage, moduleStorage, filter, 1, transaction);
                    transaction.commit();
                }
                entity.updateHalt(inputStorage, filter);
            }
            if(burnSlot.burnTime <= 0 || --burnSlot.burnTime <= 0) {
                try(Transaction transaction = Transaction.openOuter()) {
                    ItemVariant variant = StorageUtil.findExtractableResource(moduleStorage, filter, transaction);
                    if (variant != null && !variant.isBlank() && moduleStorage.extract(variant, 1, transaction) == 1) {
                        burnSlot.burnTime = burnSlot.burnTimeTotal = entity.conditioning.getFuel(variant);
                        transaction.commit();
                    }else{
                        transaction.abort();
                    }
                }
            }
            if(burnSlot.burnTime > 0) {
                entity.level++;
            }
        }

        if(entity.level > 0) {
            Season worldSeason = FabricSeasons.getCurrentSeason(world);
            Season conditionedSeason = entity.conditioning.getConditioned(worldSeason, entity.level);
            if (entity.ticket == null || entity.ticket.expired || !entity.ticket.seasons.contains(conditionedSeason)) {
                BlockBox box = BlockBox.create(pos.add(-4, -4, -4), pos.add(4, 4, 4));
                entity.ticket = new GreenhouseCache.GreenHouseTicket(box, conditionedSeason);
                ChunkPos corner1 = new ChunkPos(pos.add(-4, -4, -4));
                ChunkPos corner2 = new ChunkPos(pos.add(4, 4, 4));
                for(int x = Math.min(corner1.x, corner2.x); x < (Math.max(corner1.x, corner2.x) - Math.min(corner1.x, corner2.x))+1+Math.min(corner1.x, corner2.x); x++) {
                    for(int z = Math.min(corner1.z, corner2.z); z < (Math.max(corner1.z, corner2.z) - Math.min(corner1.z, corner2.z))+1+Math.min(corner1.z, corner2.z); z++) {
                        GreenhouseCache.add(world, new ChunkPos(x, z), entity.ticket);
                    }
                }
            } else {
                //If the greenhouse glass is removed / its season get changed, the ticket will stop updating and will be removed when tested.
                entity.ticket.age++;
            }
        }
        if(state.get(AirConditioningBlock.LEVEL) != entity.level) {
            world.setBlockState(pos, state.with(AirConditioningBlock.LEVEL, entity.level));
        }

        entity.lastProgress = entity.progress;
    }

    public static int getMaxProgress(BurnSlot[] burnSlots) {
        int maxProgress = 28+13;
        if(burnSlots[2].enabled) {
            maxProgress = 64+13;
        }else if(burnSlots[1].enabled) {
            maxProgress = 46+13;
        }else if(!burnSlots[0].enabled) {
            maxProgress = 0;
        }
        return maxProgress;
    }

    public static class BurnSlot {

        public boolean enabled;
        public int burnTime;
        public int burnTimeTotal;

        public BurnSlot(boolean enabled, int burnTime, int burnTimeTotal) {
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

    @SuppressWarnings("UnstableApiUsage")
    private class FilteredSimpleInventory extends SimpleInventory implements SidedInventory {

        private final int[] slots;

        public FilteredSimpleInventory(int size) {
            super(size);
            slots = new int[size];
            for(int s = 0; s < size; s++) {
                slots[s] = s;
            }
        }

        @Override
        public void markDirty() {
            super.markDirty();
            AirConditioningBlockEntity.this.halt = false;
            AirConditioningBlockEntity.this.markDirty();
        }

        @Override
        public int[] getAvailableSlots(Direction side) {
            return slots;
        }

        @Override
        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return AirConditioningBlockEntity.this.conditioning.getFilter().test(ItemVariant.of(stack));
        }

        @Override
        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return false;
        }
    }

    public enum Conditioning {

        HEATER(new ModIdentifier("textures/gui/heater.png"), stack -> FuelRegistry.INSTANCE.get(stack.getItem()), (current, level) -> {
            if(level == 2 && current == Season.WINTER) return Season.SPRING;
            else if(level == 1 && current == Season.FALL) return Season.SPRING;
            else if(level == 1 && current == Season.WINTER) return Season.FALL;
            else return Season.SUMMER;
        }),
        CHILLER(new ModIdentifier("textures/gui/chiller.png"), stack -> {
            if(stack.isOf(Items.POWDER_SNOW_BUCKET)) return 20;
            else if(stack.isOf(Items.SNOW_BLOCK)) return 20;
            if(stack.isOf(Items.PACKED_ICE)) return 360;
            if(stack.isOf(Items.BLUE_ICE)) return 3240;
            if(stack.isOf(Items.SNOWBALL)) return 5;
            if(stack.isOf(Items.SNOW)) return 10;
            if(stack.isOf(Items.ICE)) return 40;
            return 0;
        }, (current, level) -> {
            if(level == 2 && current == Season.SUMMER) return Season.FALL;
            else if(level == 1 && current == Season.SPRING) return Season.FALL;
            else if(level == 1 && current == Season.SUMMER) return Season.SPRING;
            else return Season.WINTER;
        });

        private final Identifier texture;
        private final Function<ItemStack, Integer> fuel;

        private final BiFunction<Season, Integer, Season> conditioning;

        Conditioning(Identifier texture, Function<ItemStack, Integer> fuel, BiFunction<Season, Integer, Season> conditioning) {
            this.texture = texture;
            this.fuel = fuel;
            this.conditioning = conditioning;
        }

        public Season getConditioned(Season current, int level) {
            return conditioning.apply(current, level);
        }

        public Identifier getTexture() {
            return texture;
        }

        @SuppressWarnings("UnstableApiUsage")
        public int getFuel(ItemVariant variant) {
            Integer fuelTime = fuel.apply(variant.toStack());
            return fuelTime != null ? fuelTime : 0;
        }

        @SuppressWarnings("UnstableApiUsage")
        public Predicate<ItemVariant> getFilter() {
            return variant -> {
                Integer fuelTime = fuel.apply(variant.toStack());
                return fuelTime != null && fuelTime > 0;
            };
        }
    }

}
