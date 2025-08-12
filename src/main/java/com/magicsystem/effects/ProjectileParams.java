package com.magicsystem.effects;

public final class ProjectileParams {
    public final double gravityPerTick;
    public final double drag;
    public final int maxLifetimeTicks;
    public final int trailFlameCount;
    public final int trailSmokeEveryNSteps;

    private ProjectileParams(Builder builder) {
        this.gravityPerTick = builder.gravityPerTick;
        this.drag = builder.drag;
        this.maxLifetimeTicks = builder.maxLifetimeTicks;
        this.trailFlameCount = builder.trailFlameCount;
        this.trailSmokeEveryNSteps = builder.trailSmokeEveryNSteps;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private double gravityPerTick = 0.01;
        private double drag = 0.996;
        private int maxLifetimeTicks = 120;
        private int trailFlameCount = 6;
        private int trailSmokeEveryNSteps = 2;

        public Builder gravityPerTick(double value) { this.gravityPerTick = value; return this; }
        public Builder drag(double value) { this.drag = value; return this; }
        public Builder maxLifetimeTicks(int value) { this.maxLifetimeTicks = value; return this; }
        public Builder trailFlameCount(int value) { this.trailFlameCount = value; return this; }
        public Builder trailSmokeEveryNSteps(int value) { this.trailSmokeEveryNSteps = value; return this; }

        public ProjectileParams build() { return new ProjectileParams(this); }
    }
}


