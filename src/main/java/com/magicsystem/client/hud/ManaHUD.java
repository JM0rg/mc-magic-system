package com.magicsystem.client.hud;

import com.magicsystem.MagicSystemMod;
import com.magicsystem.client.ClientManaManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ManaHUD {
    private static boolean initialized = false;
    
    public static void init() {
        if (!initialized) {
            HudRenderCallback.EVENT.register((context, tickDelta) -> render(context, tickDelta));
            initialized = true;
            MagicSystemMod.LOGGER.info("Mana HUD initialized");
        }
    }
    
    private static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null || client.options.hudHidden) {
            return;
        }
        
        // Get screen dimensions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Position the mana bar (top right, above the hotbar)
        int barWidth = 100;
        int barHeight = 8;
        int x = screenWidth - barWidth - 10;
        int y = screenHeight - 40; // Above the hotbar
        
        // Get real mana data from ClientManaManager
        int currentMana = ClientManaManager.getCurrentMana();
        int maxMana = ClientManaManager.getMaxMana();
        
        // Draw background
        context.fill(x, y, x + barWidth, y + barHeight, 0x88000000);
        
        // Draw mana bar
        float manaPercentage = (float) currentMana / maxMana;
        int manaBarWidth = (int) (barWidth * manaPercentage);
        context.fill(x, y, x + manaBarWidth, y + barHeight, 0xFF0066FF); // Blue mana color
        
        // Draw border
        context.drawBorder(x, y, barWidth, barHeight, 0xFFFFFFFF);
        
        // Draw text
        TextRenderer textRenderer = client.textRenderer;
        String manaText = currentMana + "/" + maxMana;
        int textWidth = textRenderer.getWidth(manaText);
        int textX = x + (barWidth - textWidth) / 2;
        int textY = y + (barHeight - 8) / 2;
        
        context.drawTextWithShadow(textRenderer, manaText, textX, textY, 0xFFFFFFFF);
        
        // Draw "Mana" label
        String label = "Mana";
        int labelWidth = textRenderer.getWidth(label);
        int labelX = x + (barWidth - labelWidth) / 2;
        int labelY = y - 12;
        
        context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);
    }
}
