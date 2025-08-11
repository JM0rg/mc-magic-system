package com.magicsystem.effects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;

import java.util.*;

public final class EffectsManager {
    private static final Map<UUID, TrackedProjectile> tracked = new HashMap<>();

    private EffectsManager() {}

    public static void trackProjectile(Entity entity) {
        trackProjectile(entity, 8.0f); // default fallback
    }

    public static void trackProjectile(Entity entity, float baseDamage) {
        if (entity == null || entity.getWorld().isClient) return;
        tracked.put(entity.getUuid(), new TrackedProjectile(entity.getUuid(), entity.getWorld().getRegistryKey().getValue().toString(), entity.getPos(), baseDamage));
    }

    public static void tick(MinecraftServer server) {
        if (tracked.isEmpty()) return;
        Iterator<Map.Entry<UUID, TrackedProjectile>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedProjectile> e = it.next();
            UUID id = e.getKey();
            TrackedProjectile tp = e.getValue();

            ServerWorld world = findWorld(server, tp.worldKey);
            if (world == null) {
                it.remove();
                continue;
            }

            Entity entity = world.getEntity(id);
            if (entity != null && entity.isAlive()) {
                Vec3d curr = entity.getPos();
                spawnTrail(world, tp.lastPos, curr);
                tp.lastPos = curr;
            } else {
                if (tp.lastPos != null) {
                    spawnImpact(world, tp.lastPos, tp.baseDamage);
                }
                it.remove();
            }
        }
    }

    private static void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
        if (from == null || to == null) {
            if (to != null) {
                world.spawnParticles(ParticleTypes.FLAME, to.x, to.y, to.z, 6, 0.03, 0.03, 0.03, 0.01);
                world.spawnParticles(ParticleTypes.SMOKE, to.x, to.y, to.z, 3, 0.03, 0.03, 0.03, 0.01);
            }
            return;
        }
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        int steps = Math.max(3, (int)(from.distanceTo(to) * 10));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = from.x + dx * t;
            double py = from.y + dy * t;
            double pz = from.z + dz * t;
            world.spawnParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
            if ((i % 2) == 0) world.spawnParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0.02, 0.02, 0.02, 0.005);
        }
    }

    private static void spawnImpact(ServerWorld world, Vec3d pos, float baseDamage) {
        // Visual explosion (no block damage, no fire placement)
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 12, 0.25, 0.25, 0.25, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.9f, 1.1f);

        // Apply AoE damage with falloff and knockback
        float radius = 3.0f; // blocks
        double r = radius;
        Box box = new Box(pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());
        for (LivingEntity le : entities) {
            double dist = le.getPos().distanceTo(pos);
            if (dist <= r) {
                float falloff = (float)Math.max(0.25, 1.0 - (dist / r));
                float dmg = baseDamage * falloff;
                le.damage(world, world.getDamageSources().explosion(null), dmg);
                le.setOnFireFor(3); // brief ignite feedback only
                // Knockback scaled by falloff
                double dx = le.getX() - pos.x;
                double dz = le.getZ() - pos.z;
                double strength = 0.8 * falloff; // tuneable
                le.takeKnockback(strength, dx, dz);
            }
        }
    }

    private static ServerWorld findWorld(MinecraftServer server, String key) {
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(key)) return w;
        }
        return null;
    }

    private static class TrackedProjectile {
        final UUID id;
        final String worldKey;
        final float baseDamage;
        Vec3d lastPos;
        TrackedProjectile(UUID id, String worldKey, Vec3d lastPos, float baseDamage) {
            this.id = id;
            this.worldKey = worldKey;
            this.lastPos = lastPos;
            this.baseDamage = baseDamage;
        }
    }
}
