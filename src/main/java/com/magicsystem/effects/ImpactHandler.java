package com.magicsystem.effects;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

@FunctionalInterface
public interface ImpactHandler {
    void onImpact(ServerWorld world, Vec3d pos);
}


