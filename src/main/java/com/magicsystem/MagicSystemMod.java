package com.magicsystem;

import com.magicsystem.commands.CastCommand;
import com.magicsystem.config.MagicSystemConfig;
import com.magicsystem.effects.EffectsManager;
import com.magicsystem.effects.WallManager;
import com.magicsystem.mana.ManaManager;
import com.magicsystem.network.MagicSystemNetworking;
import com.magicsystem.spells.SpellManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MagicSystemMod implements ModInitializer {
    public static final String MODID = "magicsystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    
    private static MagicSystemConfig config;
    private static SpellManager spellManager;
    private static ManaManager manaManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Magic System mod");
        
        // Load configuration
        config = new MagicSystemConfig();
        config.load();
        
        // Initialize managers (mana first so spells can reference it)
        manaManager = new ManaManager(config);
        spellManager = new SpellManager(config);
        
        // Register networking
        MagicSystemNetworking.registerServer();
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CastCommand.register(dispatcher);
        });
        
        // Register server events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            manaManager.onServerStarted(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            manaManager.onServerStopping(server);
            spellManager.onServerStopping();
        });
        
        // Register server tick for mana regeneration and effects
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            manaManager.tick(server);
            EffectsManager.tick(server);
            WallManager.tick(server);
        });

        // Send cooldowns to players periodically (each second)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (net.minecraft.server.network.ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                spellManager.sendCooldownsTo(p);
            }
        });
        
        LOGGER.info("Magic System mod initialized");
    }
    
    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }
    
    public static MagicSystemConfig getConfig() {
        return config;
    }
    
    public static SpellManager getSpellManager() {
        return spellManager;
    }
    
    public static ManaManager getManaManager() {
        return manaManager;
    }
}
