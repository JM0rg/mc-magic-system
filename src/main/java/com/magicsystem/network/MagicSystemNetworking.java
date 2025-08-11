package com.magicsystem.network;

import com.magicsystem.MagicSystemMod;
import com.magicsystem.client.ClientManaManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class MagicSystemNetworking {
    
    public static void registerServer() {
        // Register packet types
        PayloadTypeRegistry.playS2C().register(ManaUpdatePacket.ID, ManaUpdatePacket.CODEC);
        
        MagicSystemMod.LOGGER.info("Magic System server networking registered");
    }
    
    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        // Register client packet handler
        ClientPlayNetworking.registerGlobalReceiver(ManaUpdatePacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientManaManager.updateMana(payload.currentMana(), payload.maxMana());
            });
        });
        
        MagicSystemMod.LOGGER.info("Magic System client networking registered");
    }
    
    public static void sendManaUpdate(ServerPlayerEntity player, int currentMana, int maxMana) {
        ManaUpdatePacket packet = new ManaUpdatePacket(currentMana, maxMana);
        ServerPlayNetworking.send(player, packet);
        
        MagicSystemMod.LOGGER.debug("Sent mana update to {}: {}/{}", 
            player.getName().getString(), currentMana, maxMana);
    }
}
