package com.magicsystem.spells.core;

import com.magicsystem.effects.EffectsManager;
import com.magicsystem.spells.Spell;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ProjectileSpell extends Spell {

    public enum Variant { FIREBALL, SMALL_FIREBALL }

    private final Variant variant;
    private final double velocity;
    private final int explosionPower; // only for big fireball
    private final double startOffset;
    private final SoundEvent castSound; // nullable

    public ProjectileSpell(String id, String name, int manaCost, int cooldown,
                           Variant variant, double velocity, int explosionPower, double startOffset,
                           SoundEvent castSound,
                           float damage, float directHitRadius, float areaDamageRadius, float knockbackStrength) {
        super(id, name, manaCost, cooldown, damage, 64, false, directHitRadius, areaDamageRadius, knockbackStrength);
        this.variant = variant;
        this.velocity = velocity;
        this.explosionPower = explosionPower;
        this.startOffset = startOffset;
        this.castSound = castSound;
    }

    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            World world = player.getWorld();
            Vec3d dir = player.getRotationVec(1.0f).normalize();
            Vec3d startPos = player.getEyePos().add(dir.multiply(startOffset));

            if (castSound != null) {
                world.playSound(null, player.getBlockPos(), castSound, SoundCategory.PLAYERS, 0.9f, 1.0f);
            }

            switch (variant) {
                case FIREBALL -> {
                    FireballEntity fb = new FireballEntity(world, player, dir, Math.max(0, explosionPower));
                    fb.setPosition(startPos);
                    fb.setVelocity(dir.multiply(velocity));
                    if (world.spawnEntity(fb)) {
                        EffectsManager.trackProjectile(fb, this.damage, this.directHitRadius, this.areaDamageRadius, this.knockbackStrength);
                    }
                }
                case SMALL_FIREBALL -> {
                    SmallFireballEntity sfb = new SmallFireballEntity(world, player, dir.multiply(0.1));
                    sfb.setPosition(startPos);
                    sfb.setVelocity(dir.multiply(velocity));
                    if (world.spawnEntity(sfb)) {
                        EffectsManager.trackProjectile(sfb, this.damage, this.directHitRadius, this.areaDamageRadius, this.knockbackStrength);
                    }
                }
            }

            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("ProjectileSpell failed", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }

    public static ProjectileSpell.Variant variantFromString(String s) {
        if (s == null) return Variant.SMALL_FIREBALL;
        return switch (s.toLowerCase()) {
            case "fireball", "large", "big" -> Variant.FIREBALL;
            default -> Variant.SMALL_FIREBALL;
        };
    }

    public static net.minecraft.sound.SoundEvent soundOrNull(String id) {
        if (id == null || id.isEmpty()) return null;
        return net.minecraft.registry.Registries.SOUND_EVENT.get(net.minecraft.util.Identifier.of(id));
    }
}


