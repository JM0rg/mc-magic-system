package com.magicsystem.effects;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Manages temporary walls (Great Wall spell). Handles rise animation and timed dissolve.
 */
public final class WallManager {
    private static final List<WallInstance> ACTIVE = new ArrayList<>();

    private WallManager() {}

    public static void create(ServerWorld world,
                              List<BlockPos> positions,
                              int width, int height,
                              int riseTicks,
                              int holdTicks,
                              BlockPos center) {
        if (positions == null || positions.isEmpty()) return;
        ACTIVE.add(new WallInstance(world.getRegistryKey().getValue().toString(), positions, width, height, riseTicks, holdTicks, center));
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<WallInstance> it = ACTIVE.iterator();
        while (it.hasNext()) {
            WallInstance w = it.next();
            ServerWorld world = findWorld(server, w.worldKey);
            if (world == null) {
                it.remove();
                continue;
            }

            w.ticks++;

            // Phase 1: Rise animation over riseTicks
            if (w.ticks <= w.riseTicks) {
                int layersToPlace = Math.max(1, (int)Math.ceil((w.height * (w.ticks / (double) w.riseTicks))));
                placeLayers(world, w, layersToPlace);
                if (w.ticks % 5 == 0) {
                    world.playSound(null, w.center, SoundEvents.BLOCK_STONE_STEP, SoundCategory.BLOCKS, 0.7f, 1.0f);
                }
            } else if (w.ticks <= w.riseTicks + w.holdTicks) {
                // Ensure fully placed
                placeLayers(world, w, w.height);
                // Ambient smoke/sounds
                if ((w.ticks % 5) == 0) {
                    randomAmbient(world, w);
                }
            } else {
                // Dissolve
                dissolve(world, w);
                it.remove();
            }
        }
    }

    private static void placeLayers(ServerWorld world, WallInstance w, int layers) {
        for (BlockPos pos : w.positions) {
            int baseY = w.baseY.getOrDefault(new BlockPos(pos.getX(), 0, pos.getZ()), pos.getY());
            int y = pos.getY();
            int layerIndex = y - baseY; // 0..height-1
            if (layerIndex < layers) {
                BlockState current = world.getBlockState(pos);
                if (current.isAir()) {
                    // Place normally
                    world.setBlockState(pos, Blocks.STONE.getDefaultState());
                } else if (current.isOf(Blocks.STONE)) {
                    // Already our wall block; do nothing
                } else {
                    // Override non-air but remember original only once
                    if (!w.replaced.containsKey(pos)) {
                        w.replaced.put(pos, current);
                    }
                    world.setBlockState(pos, Blocks.STONE.getDefaultState());
                }
                // Rise particles (use simpler particles for compatibility)
                Vec3d p = Vec3d.ofCenter(pos);
                world.spawnParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private static void randomAmbient(ServerWorld world, WallInstance w) {
        net.minecraft.util.math.random.Random rand = world.getRandom();
        for (BlockPos pos : w.positions) {
            if (rand.nextFloat() < 0.10f) {
                Vec3d top = Vec3d.ofCenter(pos.up());
                world.spawnParticles(ParticleTypes.SMOKE, top.x, top.y, top.z, 1, 0.02, 0.05, 0.02, 0.01);
                if (rand.nextFloat() < 0.05f) {
                    world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_STEP, SoundCategory.BLOCKS, 0.5f, 1.0f);
                }
            }
        }
    }

    private static void dissolve(ServerWorld world, WallInstance w) {
        // Center dissolve sound
        world.playSound(null, w.center, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        for (BlockPos pos : w.positions) {
            if (world.getBlockState(pos).isOf(Blocks.STONE)) {
                // Restore replaced block if any; else air
                BlockState original = w.replaced.get(pos);
                if (original != null) {
                    world.setBlockState(pos, original);
                } else {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
                Vec3d p = Vec3d.ofCenter(pos);
                world.spawnParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 6, 0.15, 0.15, 0.15, 0.02);
                world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.10, 0.10, 0.10, 0.01);
            }
        }
    }

    private static ServerWorld findWorld(MinecraftServer server, String key) {
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(key)) return w;
        }
        return null;
    }

    private static final class WallInstance {
        final String worldKey;
        final List<BlockPos> positions;
        final int width;
        final int height;
        final int riseTicks;
        final int holdTicks;
        final BlockPos center;
        final Map<BlockPos, Integer> baseY = new HashMap<>();
        final Map<BlockPos, BlockState> replaced = new HashMap<>();
        int ticks = 0;

        WallInstance(String worldKey, List<BlockPos> positions, int width, int height, int riseTicks, int holdTicks, BlockPos center) {
            this.worldKey = worldKey;
            this.positions = positions;
            this.width = width;
            this.height = height;
            this.riseTicks = Math.max(1, riseTicks);
            this.holdTicks = Math.max(0, holdTicks);
            this.center = center;
            // compute baseY per column (x,z)
            for (BlockPos p : positions) {
                BlockPos key = new BlockPos(p.getX(), 0, p.getZ());
                baseY.merge(key, p.getY(), Math::min);
            }
        }
    }
}


