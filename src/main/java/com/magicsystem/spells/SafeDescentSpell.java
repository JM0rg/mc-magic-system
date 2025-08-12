package com.magicsystem.spells;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class SafeDescentSpell extends Spell {

    public SafeDescentSpell() {
        // id, name, manaCost=50, cooldown=40000ms, damage=0, range=0, requiresTarget=false, radii/knockback unused
        super("safedescent", "Safe Descent", 50, 40000, 0.0f, 0, false, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            // Apply Slow Falling for 30 seconds (600 ticks), amplifier 0
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0));

            // Play a magical cast sound
            player.getWorld().playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Spawn gentle particle swirl around the player
            if (!player.getWorld().isClient && player.getWorld() instanceof ServerWorld serverWorld) {
                Vec3d center = player.getPos().add(0, 1.0, 0);
                for (int i = 0; i < 24; i++) {
                    double angle = (2 * Math.PI * i) / 24.0;
                    double radius = 0.7 + (i % 3) * 0.05;
                    double x = center.x + Math.cos(angle) * radius;
                    double y = center.y + (i * 0.03);
                    double z = center.z + Math.sin(angle) * radius;
                    serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }

            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("Failed to cast Safe Descent", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}


