package io.github.lucaargolo.seasonsextras.patchouli.utils;

import io.github.lucaargolo.seasonsextras.patchouli.PatchouliMultiblockCreator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.QueryableTickScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class FakeStructureWorldAccess implements StructureWorldAccess {

    private final ServerWorld serverWorld;
    private final PatchouliMultiblockCreator testing;
    private final QueryableTickScheduler<Block> fakeBlockTickScheduler;
    private final QueryableTickScheduler<Fluid> fakeFluidTickScheduler;
    private final Random random;
    private final WorldBorder worldBorder;
    private final ChunkManager chunkManager;
    private final LightingProvider lightingProvider;

    public FakeStructureWorldAccess(ServerWorld serverWorld, PatchouliMultiblockCreator testing) {
        this.serverWorld = serverWorld;
        this.testing = testing;
        this.fakeBlockTickScheduler = new QueryableTickScheduler<>() {
            @Override
            public boolean isTicking(BlockPos pos, Block type) {
                return false;
            }

            @Override
            public void scheduleTick(OrderedTick<Block> orderedTick) {

            }

            @Override
            public boolean isQueued(BlockPos pos, Block type) {
                return false;
            }

            @Override
            public int getTickCount() {
                return 0;
            }
        };
        this.fakeFluidTickScheduler = new QueryableTickScheduler<Fluid>() {
            @Override
            public void scheduleTick(OrderedTick<Fluid> orderedTick) {

            }

            @Override
            public boolean isQueued(BlockPos pos, Fluid type) {
                return false;
            }

            @Override
            public int getTickCount() {
                return 0;
            }

            @Override
            public boolean isTicking(BlockPos pos, Fluid type) {
                return false;
            }
        };
        this.random = Random.create();
        this.worldBorder = new WorldBorder();
        this.chunkManager = new ChunkManager() {
            @Nullable
            @Override
            public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
                return FakeStructureWorldAccess.this.getChunk(x, z, leastStatus, create);
            }

            @Override
            public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {

            }

            @Override
            public String getDebugString() {
                return "";
            }

            @Override
            public int getLoadedChunkCount() {
                return 0;
            }

            @Override
            public LightingProvider getLightingProvider() {
                return FakeStructureWorldAccess.this.getLightingProvider();
            }

            @Override
            public BlockView getWorld() {
                return FakeStructureWorldAccess.this;
            }
        };
        this.lightingProvider = new LightingProvider(this.serverWorld.getChunkManager(), false, false);
    }

    @Override
    public ServerWorld toServerWorld() {
        return this.serverWorld;
    }


    @Override
    public long getSeed() {
        return this.serverWorld.getSeed();
    }

    @Override
    public long getTickOrder() {
        return this.serverWorld.getTickOrder();
    }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        return this.fakeBlockTickScheduler;
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        return this.fakeFluidTickScheduler;
    }

    @Override
    public WorldProperties getLevelProperties() {
        return this.serverWorld.getLevelProperties();
    }

    @Override
    public LocalDifficulty getLocalDifficulty(BlockPos pos) {
        return this.serverWorld.getLocalDifficulty(pos);
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return this.serverWorld.getServer();
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public ChunkManager getChunkManager() {
        return this.chunkManager;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return this.lightingProvider;
    }

    @Override
    public DynamicRegistryManager getRegistryManager() {
        return this.serverWorld.getRegistryManager();
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return this.serverWorld.getEnabledFeatures();
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return this.serverWorld.getBrightness(direction, shaded);
    }

    @Override
    public int getTopY(Heightmap.Type heightmap, int x, int z) {
        return serverWorld.getTopY();
    }

    @Override
    public int getAmbientDarkness() {
        return serverWorld.getAmbientDarkness();
    }


    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
        return new ArrayList<>();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return new ArrayList<>();
    }

    @Override
    public BiomeAccess getBiomeAccess() {
        return this.serverWorld.getBiomeAccess();
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return this.serverWorld.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public int getSeaLevel() {
        return this.serverWorld.getSeaLevel();
    }

    @Override
    public DimensionType getDimension() {
        return this.serverWorld.getDimension();
    }

    @Override
    public boolean isClient() {
        return false;
    }


    @Override
    public BlockState getBlockState(BlockPos pos) {
        Optional<BlockState> blockState = this.testing.getBlockState(pos);
        if(blockState.isPresent()) {
            return blockState.get();
        }else if(pos.getY() == 99) {
            return this.testing.getGround();
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        this.testing.setBlockState(pos, state);
        return false;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        return setBlockState(pos, Blocks.AIR.getDefaultState(), 3, 512);
    }

    @Override
    public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        return removeBlock(pos, drop);
    }

    @Override
    public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
        return state.test(getBlockState(pos));
    }

    @Override
    public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
        return state.test(getFluidState(pos));
    }

    @Nullable
    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return new WrapperProtoChunk(this.serverWorld.getChunk(chunkX, chunkZ), false);
    }

    @Override
    public void playSound(@Nullable PlayerEntity player, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {

    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

    }
}
