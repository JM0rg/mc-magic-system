package com.magicsystem.spells;

import com.magicsystem.effects.WallManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class GreatWallSpell extends Spell {

    public GreatWallSpell() {
        super("greatwall", "Great Wall", 40, 10000, 0f, 20, true, 0, 0, 0);
    }

    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            ServerWorld world = (ServerWorld) player.getWorld();

            // Raycast up to 20 blocks
            Vec3d start = player.getEyePos();
            Vec3d dir = player.getRotationVec(1.0f).normalize();
            Vec3d end = start.add(dir.multiply(20.0));
            RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
            HitResult hit = world.raycast(ctx);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                sendFailureMessage(player, "No valid ground in sight.");
                return false;
            }

            BlockPos target = ((BlockHitResult) hit).getBlockPos().up();

            // Align wall perpendicular to player's facing (horizontal)
            Direction facing = player.getHorizontalFacing();
            // If player faces north/south, wall runs east-west (x varies). If east/west, wall runs north-south (z varies)
            boolean alongX = (facing == Direction.NORTH || facing == Direction.SOUTH);

            int width = 7;  // columns
            int height = 4; // rows
            int half = width / 2; // 3

            // Build candidate block positions (override any blocks; will be restored on dissolve)
            List<BlockPos> planned = new ArrayList<>();
            for (int dx = -half; dx <= half; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    int x = alongX ? target.getX() + dx : target.getX();
                    int z = alongX ? target.getZ() : target.getZ() + dx;
                    BlockPos p = new BlockPos(x, target.getY() + dy, z);
                    planned.add(p);
                }
            }

            // Casting effects at player
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Create wall instance (rise 20 ticks = 1s, hold 100 ticks = 5s)
            WallManager.create(world, planned, width, height, 20, 100, target);

            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("Failed to cast Great Wall", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}


