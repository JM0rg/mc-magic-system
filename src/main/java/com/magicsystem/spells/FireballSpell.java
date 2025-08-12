package com.magicsystem.spells;

import com.magicsystem.effects.EffectsManager;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FireballSpell extends Spell {
    
    public FireballSpell() {
        super("fireball", "Fireball", 20, 2000, 16.0f, 64, false, 
              0.5f, 2.5f, 2.0f); // directHitRadius=0.5 (2x2), areaDamageRadius=2.5 (5x5), knockbackStrength=2.0
    }
    
    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            World world = player.getWorld();
            Vec3d direction = player.getRotationVec(1.0f).normalize();

            // Play cast sound at player
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.8f, 1.2f);

            // Spawn vanilla small fireball slightly in front of eyes to avoid self hit
            Vec3d startPos = player.getEyePos().add(direction.multiply(1.2));
            SmallFireballEntity fireball = new SmallFireballEntity(world, player, direction.multiply(5)); // Increased from 1.6 to 2.4 (1.5x faster)
            fireball.setPosition(startPos);

            if (world.spawnEntity(fireball)) {
                EffectsManager.trackProjectile(fireball, this.damage, this.directHitRadius, this.areaDamageRadius, this.knockbackStrength);
            }
            
            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("Failed to cast fireball spell", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}
