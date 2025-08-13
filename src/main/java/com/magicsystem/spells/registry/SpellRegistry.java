package com.magicsystem.spells.registry;

import com.google.gson.*;
import com.magicsystem.MagicSystemMod;
import com.magicsystem.spells.Spell;
import com.magicsystem.spells.core.ProjectileSpell;
import com.magicsystem.spells.core.StatusEffectSpell;
import com.magicsystem.spells.GreatWallSpell;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class SpellRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("magicsystem_spells.json");

    private SpellRegistry() {}

    public static Map<String, Spell> load() {
        ensureExists();
        try {
            String content = Files.readString(CONFIG_PATH);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            Map<String, Spell> out = new HashMap<>();

            for (String id : root.keySet()) {
                JsonObject s = root.getAsJsonObject(id);
                String type = getStr(s, "type", "status_effect");
                String name = getStr(s, "name", id);
                int mana = getInt(s, "manaCost", 0);
                int cd = getInt(s, "cooldown", 0);

                switch (type) {
                    case "status_effect" -> {
                        List<StatusEffectSpell.EffectDef> effects = new ArrayList<>();
                        if (s.has("effects") && s.get("effects").isJsonArray()) {
                            for (JsonElement e : s.getAsJsonArray("effects")) {
                                JsonObject eo = e.getAsJsonObject();
                                Identifier effId = Identifier.of(getStr(eo, "id", "minecraft:regeneration"));
                                int duration = getInt(eo, "duration", 100);
                                int amplifier = getInt(eo, "amplifier", 0);
                                effects.add(new StatusEffectSpell.EffectDef(effId, duration, amplifier));
                            }
                        }
                        SoundEvent castSound = ProjectileSpell.soundOrNull(getStr(s, "castSound", ""));
                        int particleCount = getInt(s, "castParticleCount", 24);
                        out.put(id, new StatusEffectSpell(id, name, mana, cd, effects, castSound, particleCount));
                    }
                    case "projectile" -> {
                        JsonObject p = s.getAsJsonObject("projectile");
                        String variant = getStr(p, "variant", "fireball");
                        double velocity = getDouble(p, "velocity", 1.0);
                        int explosionPower = getInt(p, "explosionPower", 0);
                        double startOffset = getDouble(p, "startOffset", 1.2);
                        float damage = (float) getDouble(s, "damage", 16.0);
                        float directHit = (float) getDouble(s, "directHitRadius", 0.5);
                        float areaRadius = (float) getDouble(s, "areaDamageRadius", 2.5);
                        float knockback = (float) getDouble(s, "knockbackStrength", 2.0);
                        SoundEvent castSound = ProjectileSpell.soundOrNull(getStr(s, "castSound", "minecraft:entity.firework_rocket.launch"));
                        out.put(id, new ProjectileSpell(
                            id, name, mana, cd,
                            ProjectileSpell.variantFromString(variant),
                            velocity, explosionPower, startOffset,
                            castSound,
                            damage, directHit, areaRadius, knockback
                        ));
                    }
                    case "wall" -> {
                        // Configurable Great Wall parameters
                        int width = getInt(s, "width", 7);
                        int height = getInt(s, "height", 4);
                        int range = getInt(s, "range", 20);
                        int riseTicks = getInt(s, "riseTicks", 20);
                        int holdTicks = getInt(s, "holdTicks", 100);
                        boolean allowReplace = s.has("allowReplace") && s.get("allowReplace").getAsBoolean();
                        out.put(id, new GreatWallSpell(id, name, mana, cd, width, height, range, riseTicks, holdTicks, allowReplace));
                    }
                    default -> {
                        MagicSystemMod.LOGGER.warn("Unknown spell type '{}' for id '{}'", type, id);
                    }
                }
            }
            // Ensure critical built-ins exist even if older configs don't have them yet
            out.putIfAbsent("greatwall", new com.magicsystem.spells.GreatWallSpell("greatwall", "Great Wall", 40, 10000, 7, 4, 20, 20, 100, true));
            return out;
        } catch (Exception e) {
            MagicSystemMod.LOGGER.error("Failed to load spells, falling back to empty set", e);
            return Map.of();
        }
    }

    private static void ensureExists() {
        if (Files.exists(CONFIG_PATH)) return;
        try {
            JsonObject root = new JsonObject();

            // Default fireball
            {
                JsonObject s = new JsonObject();
                s.addProperty("type", "projectile");
                s.addProperty("name", "Fireball");
                s.addProperty("manaCost", 20);
                s.addProperty("cooldown", 2000);
                JsonObject proj = new JsonObject();
                proj.addProperty("variant", "fireball");
                proj.addProperty("velocity", 1.0);
                proj.addProperty("explosionPower", 0);
                proj.addProperty("startOffset", 1.2);
                s.add("projectile", proj);
                s.addProperty("damage", 16.0);
                s.addProperty("directHitRadius", 0.5);
                s.addProperty("areaDamageRadius", 2.5);
                s.addProperty("knockbackStrength", 2.0);
                s.addProperty("castSound", "minecraft:entity.firework_rocket.launch");
                root.add("fireball", s);
            }

            // Default Safe Descent
            {
                JsonObject s = new JsonObject();
                s.addProperty("type", "status_effect");
                s.addProperty("name", "Safe Descent");
                s.addProperty("manaCost", 50);
                s.addProperty("cooldown", 40000);
                s.addProperty("castSound", "minecraft:entity.evoker.cast_spell");
                s.addProperty("castParticleCount", 24);
                JsonArray effects = new JsonArray();
                JsonObject e = new JsonObject();
                e.addProperty("id", "minecraft:slow_falling");
                e.addProperty("duration", 600);
                e.addProperty("amplifier", 0);
                effects.add(e);
                s.add("effects", effects);
                root.add("safedescent", s);
            }

            // Default Great Wall
            {
                JsonObject s = new JsonObject();
                s.addProperty("type", "wall");
                s.addProperty("name", "Great Wall");
                s.addProperty("manaCost", 40);
                s.addProperty("cooldown", 10000);
                s.addProperty("width", 7);
                s.addProperty("height", 4);
                s.addProperty("range", 20);
                s.addProperty("riseTicks", 20);
                s.addProperty("holdTicks", 100);
                s.addProperty("allowReplace", true);
                root.add("greatwall", s);
            }

            Files.writeString(CONFIG_PATH, GSON.toJson(root));
            MagicSystemMod.LOGGER.info("Created default spells config at {}", CONFIG_PATH);
        } catch (IOException ioe) {
            MagicSystemMod.LOGGER.error("Failed to create default spells config", ioe);
        }
    }

    private static String getStr(JsonObject o, String k, String def) { return o.has(k) ? o.get(k).getAsString() : def; }
    private static int getInt(JsonObject o, String k, int def) { return o.has(k) ? o.get(k).getAsInt() : def; }
    private static double getDouble(JsonObject o, String k, double def) { return o.has(k) ? o.get(k).getAsDouble() : def; }
}


