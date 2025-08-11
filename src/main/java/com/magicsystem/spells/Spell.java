package com.magicsystem.spells;

import net.minecraft.server.network.ServerPlayerEntity;

public abstract class Spell {
    protected final String id;
    protected final String name;
    protected final int manaCost;
    protected final int cooldown;
    protected final float damage;
    protected final int range;
    
    public Spell(String id, String name, int manaCost, int cooldown, float damage, int range) {
        this.id = id;
        this.name = name;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.damage = damage;
        this.range = range;
    }
    
    public abstract boolean cast(ServerPlayerEntity player);
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getManaCost() { return manaCost; }
    public int getCooldown() { return cooldown; }
    public float getDamage() { return damage; }
    public int getRange() { return range; }
    
    protected void sendCastMessage(ServerPlayerEntity player) {
        player.sendMessage(net.minecraft.text.Text.literal("§aCasting " + name + "!"));
    }
    
    protected void sendFailureMessage(ServerPlayerEntity player, String reason) {
        player.sendMessage(net.minecraft.text.Text.literal("§cFailed to cast " + name + ": " + reason));
    }
}
