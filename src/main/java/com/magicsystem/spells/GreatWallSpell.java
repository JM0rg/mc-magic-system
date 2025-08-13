package com.magicsystem.spells;

import com.magicsystem.effects.WallManager;
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

/**
 * Great Wall spell that reads its parameters from config.
 */
public class GreatWallSpell extends Spell {
    private final int width;
    private final int height;
    private final int range;
    private final int riseTicks;
    private final int holdTicks;
    private final boolean allowReplace;
    private final net.minecraft.block.BlockState material;

    public GreatWallSpell(String id, String name, int manaCost, int cooldown,
                                      int width, int height, int range, int riseTicks, int holdTicks,
                                      boolean allowReplace) {
        super(id, name, manaCost, cooldown, 0f, range, true, 0f, 0f, 0f);
        this.width = width;
        this.height = height;
        this.range = range;
        this.riseTicks = riseTicks;
        this.holdTicks = holdTicks;
        this.allowReplace = allowReplace;
        this.material = net.minecraft.block.Blocks.GRAVEL.getDefaultState();
    }

    @Override
    public boolean cast(ServerPlayerEntity player) {
        try {
            ServerWorld world = (ServerWorld) player.getWorld();

            // Raycast up to configured range
            Vec3d start = player.getEyePos();
            Vec3d dir = player.getRotationVec(1.0f).normalize();
            Vec3d end = start.add(dir.multiply(range));
            HitResult hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                sendFailureMessage(player, "No valid ground in sight.");
                return false;
            }

            BlockPos target = ((BlockHitResult) hit).getBlockPos().up();

            // Align wall perpendicular to player's facing
            Direction facing = player.getHorizontalFacing();
            boolean alongX = (facing == Direction.NORTH || facing == Direction.SOUTH);

            int half = width / 2;
            List<BlockPos> planned = new ArrayList<>();
            // Keep a level top height across the wall
            int topY = target.getY() + (height - 1);
            for (int dx = -half; dx <= half; dx++) {
                int x = alongX ? target.getX() + dx : target.getX();
                int z = alongX ? target.getZ() : target.getZ() + dx;

                // Desired bottom for the visible wall (keeps top level)
                int bottomDesired = topY - (height - 1);

                // Fill below the desired bottom if there is a gap (so gravel doesn't drop and over-stack)
                int scanY = bottomDesired - 1;
                int worldBottom = world.getBottomY();
                while (scanY >= worldBottom && world.getBlockState(new BlockPos(x, scanY, z)).isAir()) {
                    scanY--;
                }
                int fillStartY = scanY + 1; // first air above solid (or bottom)

                // Add fill blocks beneath bottomDesired (do not count towards wall height)
                for (int fy = fillStartY; fy < bottomDesired; fy++) {
                    planned.add(new BlockPos(x, fy, z));
                }

                // Add the actual wall blocks from bottomDesired up to topY (height tall)
                for (int wy = bottomDesired; wy <= topY; wy++) {
                    planned.add(new BlockPos(x, wy, z));
                }
            }

            // Casting sound
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GRAVEL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Create wall with configured material and replacement policy
            WallManager.create(world, planned, width, height, riseTicks, holdTicks, target, material, allowReplace);
            sendCastMessage(player);
            return true;
        } catch (Exception e) {
            com.magicsystem.MagicSystemMod.LOGGER.error("Failed to cast Great Wall", e);
            sendFailureMessage(player, "Internal error");
            return false;
        }
    }
}


