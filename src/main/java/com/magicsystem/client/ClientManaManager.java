package com.magicsystem.client;

import com.magicsystem.MagicSystemMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClientManaManager {
    private static int currentMana = 0;
    private static int maxMana = 100;
    
    public static void updateMana(int current, int max) {
        currentMana = current;
        maxMana = max;
        MagicSystemMod.LOGGER.debug("Client mana updated: {}/{}", current, max);
    }
    
    public static int getCurrentMana() {
        return currentMana;
    }
    
    public static int getMaxMana() {
        return maxMana;
    }
    
    public static float getManaPercentage() {
        if (maxMana <= 0) return 0.0f;
        return (float) currentMana / maxMana;
    }
    
    public static void reset() {
        currentMana = 0;
        maxMana = 100;
        MagicSystemMod.LOGGER.debug("Client mana data reset");
    }
}
