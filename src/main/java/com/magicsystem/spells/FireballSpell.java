package com.magicsystem.spells;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FireballSpell extends Spell {
    
    public FireballSpell() {
        super("fireball", "Fireball", 25, 3000, 8.0f, 64);
    }
    
    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            World world = player.getWorld();
            
            // Calculate direction (player's looking direction)
            Vec3d direction = player.getRotationVec(1.0f);
            
            // Create small fireball entity (more appropriate for player casting)
            SmallFireballEntity fireball = new SmallFireballEntity(EntityType.SMALL_FIREBALL, world);
            fireball.setOwner(player);
            
            // Set position slightly in front of player to avoid self-collision
            Vec3d startPos = player.getEyePos().add(direction.multiply(1.5));
            fireball.setPosition(startPos);
            
            // Set velocity
            fireball.setVelocity(direction.multiply(1.5)); // 1.5 speed multiplier
            
            // Spawn in world
            world.spawnEntity(fireball);
            
            // Send cast message
            sendCastMessage(player);
            
            return true;
            
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("Failed to cast fireball spell", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}
