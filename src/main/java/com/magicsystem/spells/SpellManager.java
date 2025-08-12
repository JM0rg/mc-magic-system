package com.magicsystem.spells;

import com.magicsystem.MagicSystemMod;
import com.magicsystem.config.MagicSystemConfig;
import com.magicsystem.mana.ManaManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpellManager {
    private final MagicSystemConfig config;
    private final ManaManager manaManager;
    private final Map<String, Spell> spells = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private final Map<UUID, Long> lastCastTimes = new HashMap<>(); // For rate limiting
    private static final int MIN_CAST_INTERVAL_MS = 100; // Minimum 100ms between casts
    
    public SpellManager(MagicSystemConfig config) {
        this.config = config;
        this.manaManager = MagicSystemMod.getManaManager();
        registerSpells();
    }
    
    private void registerSpells() {
        // Register all available spells
        spells.put("fireball", new FireballSpell());
        // Add more spells here as they're created
    }
    
    public boolean castSpell(ServerPlayerEntity player, String spellId) {
        UUID playerId = player.getUuid();
        long currentTime = System.currentTimeMillis();
        
        // Rate limiting check - prevent spam casting
        Long lastCastTime = lastCastTimes.get(playerId);
        if (lastCastTime != null && (currentTime - lastCastTime) < MIN_CAST_INTERVAL_MS) {
            MagicSystemMod.LOGGER.warn("Player {} attempted to cast too quickly (rate limited)", 
                player.getName().getString());
            return false;
        }
        
        Spell spell = spells.get(spellId);
        if (spell == null) {
            MagicSystemMod.LOGGER.warn("Player {} attempted to cast unknown spell: {}", 
                player.getName().getString(), spellId);
            return false;
        }
        
        // Optional per-spell raycast validation (only if spell requires target)
        if (spell.getRequiresTarget()) {
            boolean hit = hasLineOfSightTarget(player, spell.getRange());
            if (!hit) {
                player.sendMessage(net.minecraft.text.Text.literal("§cNo valid target in sight."));
                return false;
            }
        }
        
        // Check cooldown
        if (config.enableSpellCooldowns && isOnCooldown(playerId, spellId, spell.getCooldown())) {
            player.sendMessage(net.minecraft.text.Text.literal("§cSpell is on cooldown!"));
            MagicSystemMod.LOGGER.debug("Player {} spell {} on cooldown", 
                player.getName().getString(), spellId);
            return false;
        }
        
        // Check mana
        if (config.enableManaCosts && !manaManager.hasMana(player, spell.getManaCost())) {
            player.sendMessage(net.minecraft.text.Text.literal("§cNot enough mana!"));
            MagicSystemMod.LOGGER.debug("Player {} insufficient mana for spell {}", 
                player.getName().getString(), spellId);
            return false;
        }
        
        // Cast the spell
        if (spell.cast(player)) {
            // Update rate limiting timestamp
            lastCastTimes.put(playerId, currentTime);
            
            // Consume mana
            if (config.enableManaCosts) {
                manaManager.consumeMana(player, spell.getManaCost());
            }
            
            // Set cooldown
            if (config.enableSpellCooldowns) {
                setCooldown(playerId, spellId, spell.getCooldown());
            }
            
            MagicSystemMod.LOGGER.info("Player {} cast spell {} (mana cost: {}, cooldown: {}ms)", 
                player.getName().getString(), spellId, spell.getManaCost(), spell.getCooldown());
            return true;
        } else {
            MagicSystemMod.LOGGER.warn("Player {} spell {} cast failed during execution", 
                player.getName().getString(), spellId);
        }
        
        return false;
    }

    private boolean hasLineOfSightTarget(ServerPlayerEntity player, int range) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f).normalize();
        Vec3d end = start.add(dir.multiply(range));
        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
        HitResult hit = player.getWorld().raycast(ctx);
        return hit != null && hit.getType() != HitResult.Type.MISS;
    }
    
    private boolean isOnCooldown(UUID playerId, String spellId, int cooldownMs) {
        Map<String, Long> playerSpells = playerCooldowns.get(playerId);
        if (playerSpells == null) {
            return false;
        }
        
        Long lastCast = playerSpells.get(spellId);
        if (lastCast == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastCast < cooldownMs;
    }
    
    private void setCooldown(UUID playerId, String spellId, int cooldownMs) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(spellId, System.currentTimeMillis());
    }
    
    public Spell getSpell(String spellId) {
        return spells.get(spellId);
    }
    
    public Map<String, Spell> getAllSpells() {
        return new HashMap<>(spells);
    }
    
    public int getCooldownRemaining(ServerPlayerEntity player, String spellId) {
        Map<String, Long> playerSpells = playerCooldowns.get(player.getUuid());
        if (playerSpells == null) {
            return 0;
        }
        
        Long lastCast = playerSpells.get(spellId);
        if (lastCast == null) {
            return 0;
        }
        
        Spell spell = spells.get(spellId);
        if (spell == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lastCast;
        int remaining = (int) (spell.getCooldown() - elapsed);
        return Math.max(0, remaining);
    }
    
    public void onPlayerLeave(UUID playerId) {
        // Clean up player data to prevent memory leaks
        playerCooldowns.remove(playerId);
        lastCastTimes.remove(playerId);
        MagicSystemMod.LOGGER.debug("Cleaned up spell data for player {}", playerId);
    }
    
    public void onServerStopping() {
        // Clean up all data
        int cooldownCount = playerCooldowns.size();
        int rateLimitCount = lastCastTimes.size();
        
        playerCooldowns.clear();
        lastCastTimes.clear();
        
        MagicSystemMod.LOGGER.info("SpellManager cleaned up {} cooldown entries and {} rate limit entries", 
            cooldownCount, rateLimitCount);
    }
}
