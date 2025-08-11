package com.magicsystem.client;

import com.magicsystem.client.hud.ManaHUD;
import com.magicsystem.network.MagicSystemNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

@Environment(EnvType.CLIENT)
public final class MagicSystemClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register networking
        MagicSystemNetworking.registerClient();
        
        // Initialize client-side components
        ManaHUD.init();
        
        // Reset mana data when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientManaManager.reset();
        });
        
        com.magicsystem.MagicSystemMod.LOGGER.info("Magic System client initialized");
    }
}
