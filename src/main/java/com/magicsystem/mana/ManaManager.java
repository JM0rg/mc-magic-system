package com.magicsystem.mana;

import com.magicsystem.MagicSystemMod;
import com.magicsystem.config.MagicSystemConfig;
import com.magicsystem.network.MagicSystemNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManaManager {
    private final MagicSystemConfig config;
    private final Map<UUID, ManaData> playerMana = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    
    public ManaManager(MagicSystemConfig config) {
        this.config = config;
        
        // Register player connection events for proper cleanup
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            onPlayerJoin(handler.player);
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            onPlayerLeave(handler.player);
        });
        
        // Register respawn event to reset mana upon death
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            onPlayerRespawn(newPlayer, alive);
        });
    }
    
    public boolean hasMana(PlayerEntity player, int amount) {
        ManaData manaData = getManaData(player.getUuid());
        return manaData.getCurrentMana() >= amount;
    }
    
    public boolean consumeMana(PlayerEntity player, int amount) {
        ManaData manaData = getManaData(player.getUuid());
        if (manaData.getCurrentMana() >= amount) {
            manaData.setCurrentMana(manaData.getCurrentMana() - amount);
            updateMana(player);
            return true;
        }
        return false;
    }
    
    public void restoreMana(PlayerEntity player, int amount) {
        ManaData manaData = getManaData(player.getUuid());
        int newMana = Math.min(manaData.getCurrentMana() + amount, getMaxMana(player));
        manaData.setCurrentMana(newMana);
        updateMana(player);
    }
    
    public void setMana(PlayerEntity player, int amount) {
        ManaData manaData = getManaData(player.getUuid());
        manaData.setCurrentMana(Math.min(amount, getMaxMana(player)));
        updateMana(player);
    }
    
    public int getCurrentMana(PlayerEntity player) {
        return getManaData(player.getUuid()).getCurrentMana();
    }
    
    public int getMaxMana(PlayerEntity player) {
        return config.baseMana + (player.experienceLevel * config.manaPerLevel);
    }
    
    public float getManaPercentage(PlayerEntity player) {
        return (float) getCurrentMana(player) / getMaxMana(player);
    }
    
    private ManaData getManaData(UUID playerId) {
        return playerMana.computeIfAbsent(playerId, k -> new ManaData(config.baseMana));
    }
    
    private void updateMana(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Sync mana to client
            ManaData manaData = getManaData(player.getUuid());
            int maxMana = getMaxMana(player);
            
            MagicSystemNetworking.sendManaUpdate(serverPlayer, manaData.getCurrentMana(), maxMana);
            
            MagicSystemMod.LOGGER.debug("Updated mana for player {}: {}/{}", 
                player.getName().getString(), manaData.getCurrentMana(), maxMana);
        }
    }
    
    public void onPlayerJoin(ServerPlayerEntity player) {
        ManaData manaData = getManaData(player.getUuid());
        manaData.setCurrentMana(getMaxMana(player)); // Full mana on join
        updateMana(player);
        MagicSystemMod.LOGGER.info("Player {} joined with {}/{} mana", 
            player.getName().getString(), manaData.getCurrentMana(), getMaxMana(player));
    }
    
    public void onPlayerLeave(ServerPlayerEntity player) {
        // Clean up player data to prevent memory leaks
        UUID playerId = player.getUuid();
        playerMana.remove(playerId);
        
        // Also clean up spell manager data
        MagicSystemMod.getSpellManager().onPlayerLeave(playerId);
        
        MagicSystemMod.LOGGER.info("Player {} left, cleaned up mana and spell data", player.getName().getString());
    }
    
    public void onPlayerRespawn(ServerPlayerEntity player, boolean alive) {
        // Reset mana to 100 upon respawn (after death)
        if (!alive) { // Player died and respawned
            ManaData manaData = getManaData(player.getUuid());
            manaData.setCurrentMana(100);
            updateMana(player);
            MagicSystemMod.LOGGER.info("Player {} respawned after death, mana reset to 100", player.getName().getString());
        }
    }
    
    public void tick(MinecraftServer server) {
        // Only regenerate mana every interval (default 20 ticks = 1 second)
        tickCounter++;
        if (tickCounter < config.manaRegenerationInterval) {
            return;
        }
        tickCounter = 0;
        
        // Regenerate mana for all online players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            ManaData manaData = playerMana.get(playerId);
            
            if (manaData != null) {
                int currentMana = manaData.getCurrentMana();
                int maxMana = getMaxMana(player);
                
                if (currentMana < maxMana) {
                    int newMana = Math.min(currentMana + config.manaRegenerationRate, maxMana);
                    manaData.setCurrentMana(newMana);
                    manaData.setLastRegenerationTime(System.currentTimeMillis());
                    
                    // Send update to client
                    updateMana(player);
                    
                    MagicSystemMod.LOGGER.debug("Regenerated mana for {}: {} -> {}", 
                        player.getName().getString(), currentMana, newMana);
                }
            }
        }
    }
    
    public void onServerStarted(MinecraftServer server) {
        MagicSystemMod.LOGGER.info("ManaManager initialized for server with {} players", 
            server.getPlayerManager().getPlayerList().size());
    }
    
    public void onServerStopping(MinecraftServer server) {
        // Clean up all player data
        int playerCount = playerMana.size();
        playerMana.clear();
        tickCounter = 0;
        MagicSystemMod.LOGGER.info("ManaManager cleaned up data for {} players", playerCount);
    }
}
