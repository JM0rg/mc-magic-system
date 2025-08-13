package com.magicsystem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magicsystem.MagicSystemMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MagicSystemConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("magicsystem.json");
    
    // Mana settings
    public int baseMana = 100;
    public int manaPerLevel = 10;
    public int manaRegenerationRate = 1; // mana per second (default doubled)
    public int manaRegenerationInterval = 5; // ticks between regeneration
    
    // Spell settings
    public boolean enableSpellCooldowns = true;
    public boolean enableManaCosts = true;
    public float globalSpellDamageMultiplier = 1.0f;
    
    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                
                // Mana settings are code-defined. JSON values (if present) are ignored intentionally.
                // This preserves server-authoritative tuning and prevents client-side edits from changing behavior.
                
                // Load spell settings
                if (json.has("enableSpellCooldowns")) enableSpellCooldowns = json.get("enableSpellCooldowns").getAsBoolean();
                if (json.has("enableManaCosts")) enableManaCosts = json.get("enableManaCosts").getAsBoolean();
                if (json.has("globalSpellDamageMultiplier")) globalSpellDamageMultiplier = json.get("globalSpellDamageMultiplier").getAsFloat();
                
                MagicSystemMod.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } else {
                save();
                MagicSystemMod.LOGGER.info("Created default configuration at {}", CONFIG_PATH);
            }
        } catch (IOException e) {
            MagicSystemMod.LOGGER.error("Failed to load configuration", e);
        }
    }
    
    public void save() {
        try {
            JsonObject json = new JsonObject();
            
            // Save mana settings (for visibility only; edits are ignored on load)
            json.addProperty("baseMana", baseMana);
            json.addProperty("manaPerLevel", manaPerLevel);
            json.addProperty("manaRegenerationRate", manaRegenerationRate);
            json.addProperty("manaRegenerationInterval", manaRegenerationInterval);
            
            // Save spell settings
            json.addProperty("enableSpellCooldowns", enableSpellCooldowns);
            json.addProperty("enableManaCosts", enableManaCosts);
            json.addProperty("globalSpellDamageMultiplier", globalSpellDamageMultiplier);
            
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
            MagicSystemMod.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            MagicSystemMod.LOGGER.error("Failed to save configuration", e);
        }
    }
}
