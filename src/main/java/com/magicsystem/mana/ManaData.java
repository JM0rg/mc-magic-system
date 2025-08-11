package com.magicsystem.mana;

public class ManaData {
    private int currentMana;
    private long lastRegenerationTime;
    
    public ManaData(int initialMana) {
        this.currentMana = initialMana;
        this.lastRegenerationTime = System.currentTimeMillis();
    }
    
    public int getCurrentMana() {
        return currentMana;
    }
    
    public void setCurrentMana(int currentMana) {
        this.currentMana = Math.max(0, currentMana);
    }
    
    public long getLastRegenerationTime() {
        return lastRegenerationTime;
    }
    
    public void setLastRegenerationTime(long lastRegenerationTime) {
        this.lastRegenerationTime = lastRegenerationTime;
    }
}
