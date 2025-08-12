package com.magicsystem.spells.core;

import com.magicsystem.spells.Spell;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

import java.util.List;

public class StatusEffectSpell extends Spell {

    public static class EffectDef {
        public final Identifier id;
        public final int duration;
        public final int amplifier;
        public EffectDef(Identifier id, int duration, int amplifier) {
            this.id = id; this.duration = duration; this.amplifier = amplifier;
        }
    }

    private final List<EffectDef> effects;
    private final SoundEvent castSound; // nullable
    private final int particleCount;    // simple scalar for cloud particles

    public StatusEffectSpell(String id, String name, int manaCost, int cooldown,
                             List<EffectDef> effects, SoundEvent castSound, int particleCount) {
        super(id, name, manaCost, cooldown, 0f, 0, false, 0f, 0f, 0f);
        this.effects = effects;
        this.castSound = castSound;
        this.particleCount = particleCount;
    }

    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            for (EffectDef def : effects) {
                StatusEffect se = Registries.STATUS_EFFECT.get(def.id);
                if (se != null) {
                    var entry = Registries.STATUS_EFFECT.getEntry(se);
                    if (entry != null) {
                        player.addStatusEffect(new StatusEffectInstance(entry, def.duration, def.amplifier));
                    }
                }
            }

            if (castSound != null) {
                player.getWorld().playSound(null, player.getBlockPos(), castSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }

            if (!player.getWorld().isClient && player.getWorld() instanceof ServerWorld sw && particleCount > 0) {
                Vec3d c = player.getPos().add(0, 1.0, 0);
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / Math.max(1, particleCount);
                    double r = 0.7;
                    double x = c.x + Math.cos(angle) * r;
                    double y = c.y + (i * 0.02);
                    double z = c.z + Math.sin(angle) * r;
                    sw.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }

            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("StatusEffectSpell failed", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}


