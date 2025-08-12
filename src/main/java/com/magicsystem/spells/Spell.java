package com.magicsystem.spells;

import net.minecraft.server.network.ServerPlayerEntity;

public abstract class Spell {
    protected final String id;
    protected final String name;
    protected final int manaCost;
    protected final int cooldown;
    protected final float damage;
    protected final int range;
    protected final boolean requiresTarget;
    // Impact/effects parameters
    protected final float directHitRadius;
    protected final float areaDamageRadius;
    protected final float knockbackStrength;
    
    public Spell(String id, String name, int manaCost, int cooldown, float damage, int range, boolean requiresTarget, 
                 float directHitRadius, float areaDamageRadius, float knockbackStrength) {
        this.id = id;
        this.name = name;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.damage = damage;
        this.range = range;
        this.requiresTarget = requiresTarget;
        this.directHitRadius = directHitRadius;
        this.areaDamageRadius = areaDamageRadius;
        this.knockbackStrength = knockbackStrength;
    }
    
    public abstract boolean cast(ServerPlayerEntity player);

    // Hook: spells can override for custom one-off impact visuals or extra effects
    public void onImpact(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.Vec3d pos) {
        // default no-op
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getManaCost() { return manaCost; }
    public int getCooldown() { return cooldown; }
    public float getDamage() { return damage; }
    public int getRange() { return range; }
    public boolean getRequiresTarget() { return requiresTarget; }
    public float getDirectHitRadius() { return directHitRadius; }
    public float getAreaDamageRadius() { return areaDamageRadius; }
    public float getKnockbackStrength() { return knockbackStrength; }
    
    protected void sendCastMessage(ServerPlayerEntity player) {
        player.sendMessage(net.minecraft.text.Text.literal("§aCasting " + name + "!"));
    }
    
    protected void sendFailureMessage(ServerPlayerEntity player, String reason) {
        player.sendMessage(net.minecraft.text.Text.literal("§cFailed to cast " + name + ": " + reason));
    }
}
