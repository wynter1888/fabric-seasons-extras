package io.github.lucaargolo.seasonsextras.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class PatchouliMultiblockCreator {

    private static PatchouliMultiblockCreator testing = null;

    private static final char[] VALID_CHARS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private final HashMap<BlockPos, BlockState> testingMap;
    private final Thread thread;

    private final BlockState ground;

    private final BlockState decoration;

    private final BlockPos offset;
    private final Runnable generate;



    public PatchouliMultiblockCreator(BlockState ground, BlockState decoration, BlockPos offset, Runnable generate) {
        this.testingMap = new HashMap<>();
        this.thread = Thread.currentThread();
        this.ground = ground;
        this.decoration = decoration;
        this.offset = offset;
        this.generate = generate;
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        this.testingMap.put(pos.add(offset), state);
    }

    public Optional<BlockState> getBlockState(BlockPos pos) {
        BlockPos off = pos.add(offset);
        if(this.testingMap.containsKey(off)) {
            return Optional.of(this.testingMap.get(off));
        }else{
            return Optional.empty();
        }
    }

    public Optional<JsonObject> getMultiblock(Predicate<Set<BlockState>> valid) {
        testing = this;
        try{ this.generate.run(); }catch (Exception ignored) { }
        testing = null;
        AtomicInteger index = new AtomicInteger();
        HashMap<BlockState, Character> mappings = new HashMap<>();


        HashMap<Integer, HashMap<Integer, HashMap<Integer, Character>>> pattern = new HashMap<>();
        BlockBox box;
        Optional<BlockBox> optional = BlockBox.encompassPositions(this.testingMap.keySet());

        if (optional.isPresent()) {
            box = optional.get();
            this.testingMap.forEach((pos, state) -> {
                if (!mappings.containsKey(state) && index.getAndIncrement() < VALID_CHARS.length) {
                    mappings.put(state, VALID_CHARS[index.get() - 1]);
                }
                Character mapping = mappings.get(state);
                if (mapping != null) {
                    int x = pos.getX() + (box.getMinX() <= 0 ? -box.getMinX() : 0);
                    int y = pos.getY() + (box.getMinY() <= 0 ? -box.getMinY() : 0);
                    int z = pos.getZ() + (box.getMinZ() <= 0 ? -box.getMinZ() : 0);
                    pattern.computeIfAbsent(y, i -> new HashMap<>()).computeIfAbsent(x, i -> new HashMap<>()).put(z, mapping);
                }
            });
        }else{
            box = new BlockBox(0, 0, 0, 4, 6, 4);
        }

        String[][] realPattern = new String[box.getBlockCountY()][box.getBlockCountX()];
        Random random = Random.create(1337L);

        BlockState centerState = Blocks.AIR.getDefaultState();
        boolean foundCenter = false;
        for (int i = 0; i < box.getBlockCountY()+2; i++) {
            int y = box.getBlockCountY() + 1 - i;
            for (int x = 0; x < box.getBlockCountX(); x++) {
                realPattern[y][x] = "";
                for (int z = 0; z < box.getBlockCountZ(); z++) {
                    char d = ' ';
                    if (x != 0 && x != box.getBlockCountX() - 1 && z != 0 && z != box.getBlockCountZ() - 1) {
                        if (y == (box.getBlockCountY() - 1)) {
                            d = '1';
                        } else if (y == (box.getBlockCountY() - 2) && random.nextInt(4) == 2) {
                            d = '2';
                        }
                    }
                    char f = pattern.getOrDefault(i, new HashMap<>()).getOrDefault(x, new HashMap<>()).getOrDefault(z, d);
                    if (!foundCenter && box.getBlockCountX()/2 == x && box.getBlockCountY()/2 == i && box.getBlockCountZ()/2 == z) {
                        foundCenter = true;
                        realPattern[y][x] += '0';
                        if(f != d) {
                            boolean foundState = false;
                            Iterator<Map.Entry<BlockState, Character>> iterator = mappings.entrySet().iterator();
                            while (iterator.hasNext() && !foundState) {
                                Map.Entry<BlockState, Character> entry = iterator.next();
                                if(entry.getValue() == f) {
                                    centerState = entry.getKey();
                                    foundState = true;
                                }
                            }
                        }
                    } else {
                        realPattern[y][x] += f;
                    }
                }
            }
        }

        JsonObject multiblock = new JsonObject();
        JsonObject m = new JsonObject();
        mappings.forEach((s, c) -> m.addProperty(c.toString(), stateToString(s)));
        m.addProperty("0", stateToString(centerState));
        m.addProperty("1", stateToString(this.ground));
        m.addProperty("2", stateToString(this.decoration));
        multiblock.add("mapping", m);
        multiblock.add("pattern", new Gson().toJsonTree(realPattern));


        if(valid.test(new HashSet<>(this.testingMap.values()))) {
            return Optional.of(multiblock);
        }else{
            return Optional.empty();
        }
    }

    public Thread getThread() {
        return this.thread;
    }

    public BlockState getGround() {
        return this.ground;
    }

    public static PatchouliMultiblockCreator getTesting() {
        return testing;
    }

    private static String stateToString(BlockState state) {
        return state.toString().replace("Block{", "").replace("}", "");
    }








}
