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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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

        // --- Active effect timers (top-right) ---
        // Show remaining time for selected effects (e.g., Safe Descent / Slow Falling)
        int rightPadding = 8;
        int topPadding = 8;
        int effectY = topPadding;
        int maxNameWidth = 140;

        if (player != null) {
            for (StatusEffectInstance inst : player.getStatusEffects()) {
                // Identify effect id
                Identifier effId = Registries.STATUS_EFFECT.getId(inst.getEffectType().value());
                if (effId == null) continue;

                // Only show for slow_falling for now
                if (effId.equals(Identifier.of("minecraft:slow_falling"))) {
                    String name = "Safe Descent";
                    int seconds = Math.max(0, inst.getDuration() / 20);
                    String line = name + ": " + seconds + "s";
                    int w = textRenderer.getWidth(line);
                    int tx = client.getWindow().getScaledWidth() - rightPadding - w;
                    context.drawTextWithShadow(textRenderer, line, tx, effectY, 0xFFFFFFFF);
                    effectY += 10;
                }
            }
        }
    }
}
