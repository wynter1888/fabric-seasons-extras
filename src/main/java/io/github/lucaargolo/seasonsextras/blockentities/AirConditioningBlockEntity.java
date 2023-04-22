package io.github.lucaargolo.seasonsextras.blockentities;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class AirConditioningBlockEntity extends BlockEntity implements Inventory, SidedInventory {

    private GreenhouseCache.GreenHouseTicket ticket = null;


    private Conditioning conditioning;
    private boolean particles = true;
    private int progress = 0;

    private int lastProgress = 0;
    private int level = 0;
    private boolean halt = false;

    private final FilteredSimpleInventory inputInventory = new FilteredSimpleInventory(9);
    private final FilteredSimpleInventory moduleInventory = new FilteredSimpleInventory(3);

    private final BurnSlot[] burnSlots = new BurnSlot[] {
        new BurnSlot(true, false, 0, 0),
        new BurnSlot(false, false, 0, 0),
        new BurnSlot(false, false, 0, 0)
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
                    return AirConditioningBlockEntity.this.particles ? 1 : 0;
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
        public void set(int index, int value) {}

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
        writeClientNbt(nbt);
    }

    public void writeClientNbt(NbtCompound nbt) {
        nbt.putInt("conditioning", conditioning.ordinal());
        nbt.putBoolean("particles", particles);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        progress = nbt.getInt("progress");
        Inventories.readNbt(nbt.getCompound("inputInventory"), inputInventory.stacks);
        Inventories.readNbt(nbt.getCompound("moduleInventory"), moduleInventory.stacks);
        for (int i = 0; i < burnSlots.length; i++) {
            burnSlots[i].readNbt(nbt.getCompound("module_" + i));
        }
        readClientNbt(nbt);
    }

    public void readClientNbt(NbtCompound nbt) {
        conditioning = Conditioning.values()[Math.max(0, Math.min(Conditioning.values().length, nbt.getInt("conditioning")))];
        particles = nbt.getBoolean("particles");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        this.writeClientNbt(nbt);
        return nbt;
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

    public void cycleButton(int index) {
        markDirty();
        switch (index) {
            case 0 -> this.burnSlots[0].enabled = !this.burnSlots[0].enabled;
            case 1 -> this.burnSlots[1].enabled = !this.burnSlots[1].enabled;
            case 2 -> this.burnSlots[2].enabled = !this.burnSlots[2].enabled;
            case 3 -> {
                this.particles = !this.particles;
                if(world instanceof ServerWorld serverWorld) {
                    serverWorld.getChunkManager().markForUpdate(this.pos);
                }
            }
        }
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

        BurnSlot[] burnSlots = AirConditioningScreenHandler.getBurnSlots(entity.moduleInventory, entity.propertyDelegate);
        int maxProgress = AirConditioningScreenHandler.getMaxProgress(burnSlots);
        if(entity.halt || entity.progress >= maxProgress) {
            entity.progress = 0;
        }else if(entity.progress + 3 > maxProgress) {
            entity.progress = maxProgress;
        }else{
            entity.progress += 3;
        }
        burnSlots = entity.burnSlots;

        entity.level = 0;
        for (int i = 0; i < burnSlots.length; i++) {
            BurnSlot burnSlot = burnSlots[i];
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
                        ItemVariant result = StorageUtil.findExtractableResource(moduleStorage, filter, transaction);
                        if ((result == null || result.isBlank()) && variant.getItem().hasRecipeRemainder()) {
                            Item item = variant.getItem().getRecipeRemainder();
                            ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
                            ItemVariant remainder = ItemVariant.of(stack);
                            moduleStorage.insert(remainder, 1, transaction);
                        }
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

    public static void clientTick(World world, BlockPos pos, BlockState state, AirConditioningBlockEntity entity) {
        if(entity.particles) {
            Random random = world.getRandom();
            int level = state.get(AirConditioningBlock.LEVEL);
            if (level > 0) {
                for (int a = 0; a < random.nextInt(MathHelper.ceil(Math.pow(3.0, level))); a++) {
                    world.addParticle(entity.conditioning.getParticle(), pos.getX() + (random.nextInt(900) - 400) / 100.0, pos.getY() + (random.nextInt(900) - 400) / 100.0, pos.getZ() + (random.nextInt(900) - 400) / 100.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    public int size() {
        return inputInventory.size() + moduleInventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inputInventory.isEmpty() && moduleInventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot < inputInventory.size() ? inputInventory.getStack(slot) : moduleInventory.getStack(slot - inputInventory.size());
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return slot < inputInventory.size() ? inputInventory.removeStack(slot, amount) : moduleInventory.removeStack(slot - inputInventory.size(), amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return slot < inputInventory.size() ? inputInventory.removeStack(slot) : moduleInventory.removeStack(slot - inputInventory.size());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(slot < inputInventory.size()) {
            inputInventory.setStack(slot, stack);
        } else {
            moduleInventory.setStack(slot - inputInventory.size(), stack);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return inputInventory.canPlayerUse(player) && moduleInventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        inputInventory.clear();
        moduleInventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if(side == Direction.DOWN) {
            return Arrays.stream(moduleInventory.getAvailableSlots(side)).map(i -> i+inputInventory.size()).toArray();
        }
        return inputInventory.getAvailableSlots(side);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < inputInventory.size() ? inputInventory.canInsert(slot, stack, dir) : moduleInventory.canInsert(slot, stack, dir);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot < inputInventory.size() ? inputInventory.canExtract(slot, stack, dir) : moduleInventory.canExtract(slot, stack, dir);
    }


    public static class BurnSlot {

        public boolean enabled;
        public boolean full;
        public int burnTime;
        public int burnTimeTotal;

        public BurnSlot(boolean enabled, boolean full, int burnTime, int burnTimeTotal) {
            this.enabled = enabled;
            this.full = full;
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
            return !canInsert(slot, stack, dir);
        }
    }

    public enum Conditioning {

        HEATER(new ModIdentifier("textures/gui/heater.png"), stack -> {
            Integer fuel = FuelRegistry.INSTANCE.get(stack.getItem());
            if(fuel != null) {
                return fuel*3;
            }else{
                return 0;
            }
        }, (current, level) -> {
            if(level == 2 && current == Season.WINTER) return Season.SPRING;
            else if(level == 1 && current == Season.FALL) return Season.SPRING;
            else if(level == 1 && current == Season.WINTER) return Season.FALL;
            else return Season.SUMMER;
        }, ParticleTypes.FLAME),
        CHILLER(new ModIdentifier("textures/gui/chiller.png"), stack -> {
            if(stack.isOf(Items.POWDER_SNOW_BUCKET)) return 20*30;
            else if(stack.isOf(Items.SNOW_BLOCK)) return 20*30;
            if(stack.isOf(Items.PACKED_ICE)) return 360*30;
            if(stack.isOf(Items.BLUE_ICE)) return 3240*30;
            if(stack.isOf(Items.SNOWBALL)) return 5*30;
            if(stack.isOf(Items.SNOW)) return 10*30;
            if(stack.isOf(Items.ICE)) return 40*30;
            return 0;
        }, (current, level) -> {
            if(level == 2 && current == Season.SUMMER) return Season.FALL;
            else if(level == 1 && current == Season.SPRING) return Season.FALL;
            else if(level == 1 && current == Season.SUMMER) return Season.SPRING;
            else return Season.WINTER;
        }, ParticleTypes.SNOWFLAKE);

        private final Identifier texture;
        private final Function<ItemStack, Integer> fuel;

        private final BiFunction<Season, Integer, Season> conditioning;

        private final ParticleEffect particle;

        Conditioning(Identifier texture, Function<ItemStack, Integer> fuel, BiFunction<Season, Integer, Season> conditioning, ParticleEffect particle) {
            this.texture = texture;
            this.fuel = fuel;
            this.conditioning = conditioning;
            this.particle = particle;
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

        public ParticleEffect getParticle() {
            return particle;
        }
    }

}
