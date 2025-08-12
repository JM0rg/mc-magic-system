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

    // Trajectory tuning
    private static final double GRAVITY_PER_TICK = 0.01;   // was 0.04 - gentler arc
    private static final double DRAG = 0.996;               // was 0.99 - less slowdown
    private static final int MAX_LIFETIME_TICKS = 120;      // was 60 - longer flight (~6s)

    private EffectsManager() {}

    public static void trackProjectile(Entity entity) {
        trackProjectile(entity, 8.0f); // default fallback
    }

    public static void trackProjectile(Entity entity, float baseDamage) {
        trackProjectile(entity, baseDamage, 0.5f, 2.5f, 2.0f); // Default values for backward compatibility
    }
    
    public static void trackProjectile(Entity entity, float baseDamage, float directHitRadius, float areaDamageRadius, float knockbackStrength) {
        if (entity == null || entity.getWorld().isClient) return;
        tracked.put(entity.getUuid(), new TrackedProjectile(entity.getUuid(), entity.getWorld().getRegistryKey().getValue().toString(), entity.getPos(), baseDamage, directHitRadius, areaDamageRadius, knockbackStrength));
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
                // Apply arc: gravity and drag
                Vec3d v = entity.getVelocity();
                Vec3d vNext = new Vec3d(v.x * DRAG, (v.y - GRAVITY_PER_TICK) * DRAG, v.z * DRAG);
                entity.setVelocity(vNext);

                // Trail and lifetime handling
                Vec3d curr = entity.getPos();
                spawnTrail(world, tp.lastPos, curr);
                tp.lastPos = curr;

                tp.ticks++;
                if (tp.ticks > MAX_LIFETIME_TICKS) {
                    // Timed-out: explode and remove
                    spawnImpact(world, entity.getPos(), tp.baseDamage, tp.directHitRadius, tp.areaDamageRadius, tp.knockbackStrength);
                    entity.discard();
                    it.remove();
                }
            } else {
                // Impact occurred or despawned: spawn explosion effects at last known pos
                if (tp.lastPos != null) {
                    spawnImpact(world, tp.lastPos, tp.baseDamage, tp.directHitRadius, tp.areaDamageRadius, tp.knockbackStrength);
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

    private static void spawnImpact(ServerWorld world, Vec3d pos, float baseDamage, float directHitRadius, float areaDamageRadius, float knockbackStrength) {
        // Visual explosion (no block damage, no fire placement)
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 12, 0.25, 0.25, 0.25, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.9f, 1.1f);

        // Apply AoE damage with spell-specific zones
        
        double maxRadius = areaDamageRadius;
        Box box = new Box(pos.x - maxRadius, pos.y - maxRadius, pos.z - maxRadius, 
                         pos.x + maxRadius, pos.y + maxRadius, pos.z + maxRadius);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());
        
        for (LivingEntity le : entities) {
            double dist = le.getPos().distanceTo(pos);
            float dmg;
            float finalKnockback;
            
            if (dist <= directHitRadius) {
                // Direct hit zone: full damage
                dmg = baseDamage;
                finalKnockback = knockbackStrength;
            } else if (dist <= areaDamageRadius) {
                // Area damage zone: falloff from 75% to 25%
                float falloffFactor = (float)(1.0 - ((dist - directHitRadius) / (areaDamageRadius - directHitRadius)));
                dmg = baseDamage * (0.25f + 0.5f * falloffFactor); // 25% to 75% damage
                finalKnockback = knockbackStrength * (0.4f + 0.6f * falloffFactor); // Reduced knockback in outer zone
            } else {
                continue; // Outside damage range
            }
            
            le.damage(world, world.getDamageSources().explosion(null), dmg);
            le.setOnFireFor(3); // brief ignite feedback only
            
            // Apply knockback scaled by zone
            double dx = le.getX() - pos.x;
            double dz = le.getZ() - pos.z;
            double hDist = Math.sqrt(dx * dx + dz * dz);
            if (hDist > 0.0) {
                dx /= hDist;
                dz /= hDist;
            }
            le.takeKnockback(1.2 * finalKnockback, dx, dz);
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
        final float directHitRadius;
        final float areaDamageRadius;
        final float knockbackStrength;
        Vec3d lastPos;
        int ticks = 0;
        TrackedProjectile(UUID id, String worldKey, Vec3d lastPos, float baseDamage, float directHitRadius, float areaDamageRadius, float knockbackStrength) {
            this.id = id;
            this.worldKey = worldKey;
            this.lastPos = lastPos;
            this.baseDamage = baseDamage;
            this.directHitRadius = directHitRadius;
            this.areaDamageRadius = areaDamageRadius;
            this.knockbackStrength = knockbackStrength;
        }
    }
}
