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

        // Get real mana data from ClientManaManager
        int currentMana = ClientManaManager.getCurrentMana();
        int maxMana = ClientManaManager.getMaxMana();

        // Draw as pips similar to health/hunger above the health bar
        // Health/hotbar start x is centered - 91
        int baseX = (screenWidth / 2) - 91;
        int pipSize = 7;    // smaller squares so the bar is shorter than health
        int pipGap = 1;
        // Dynamically size pips based on max mana: 1 pip per 10 mana
        int unitsPerPip = 10;
        int pips = Math.max(1, (int) Math.ceil((double) maxMana / unitsPerPip));
        int totalWidth = pips * pipSize + (pips - 1) * pipGap;
        int xStart = baseX;
        int y = screenHeight - 49; // directly above the health row

        // Compute fill pips
        float ratio = maxMana > 0 ? Math.min(1f, (float) currentMana / (float) maxMana) : 0f;
        float filledExact = ratio * pips;

        // Draw pips
        for (int i = 0; i < pips; i++) {
            int px = xStart + i * (pipSize + pipGap);
            int py = y;

            // empty pip background (dark)
            context.fill(px, py, px + pipSize, py + pipSize, 0xAA111122);
            // border
            context.drawBorder(px, py, pipSize, pipSize, 0xFF2A2A55);

            // fill amount for this pip
            float remain = filledExact - i;
            if (remain > 0f) {
                int fillW;
                if (remain >= 1f) {
                    fillW = pipSize - 2;
                } else {
                    fillW = Math.max(1, (int) Math.floor((pipSize - 2) * remain));
                }
                int fx = px + 1;
                int fy = py + 1;
                int fy2 = py + pipSize - 1;
                // filled color (blue)
                context.fill(fx, fy, fx + fillW, fy2, 0xFF2FA3FF);
            }
        }

        // No label text to keep HUD clean

        // --- Active effect timers (top-right) ---
        // Show remaining time for selected effects (e.g., Safe Descent / Slow Falling)
        int rightPadding = 8;
        int topPadding = 8;
        int effectY = topPadding;
        int maxNameWidth = 140;

        if (player != null) {
            TextRenderer textRenderer = client.textRenderer;
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
