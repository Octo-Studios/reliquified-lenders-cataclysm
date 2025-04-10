package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RECParticleUtils {
    public static void createCircleSegment(ParticleOptions particle, Level level,
                                           Vec3 center, Vec3 target, double radius, float step) {
        BlockHitResult result = level.clip(new ClipContext(center, center.add(0.0D, -radius, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));

        if (result.getType() == HitResult.Type.MISS) {
            return;
        }

        double distanceToCircle = radius - target.distanceTo(center);

        if (distanceToCircle < 0D) {
            return;
        }

        double distanceFrac = distanceToCircle / radius;

        double arcMin = Math.PI / 12, arcMax = 2 * Math.PI / 3;
        double arcWidth = Math.clamp(1.0D - Math.clamp(distanceFrac, 0.0D, 1.0D), arcMin, arcMax);
        double targetAngle = Math.atan2(target.z - center.z, target.x - center.x);

        int n = (int) (Math.abs(targetAngle) * 180 / Math.PI); // arc degree measure
        int arcLength = (int) (Math.PI * radius * (360 - n) / (180));
        int segmentPointsNum = (int) (target.distanceTo(center) * arcLength / radius);

        int maxTries = (int) Math.ceil(radius * 2.0D);

        for (int i = 0; i < segmentPointsNum; i++) {
            double pointsFrac = (double) i / segmentPointsNum;
            double angle = Mth.lerp(pointsFrac, targetAngle - arcWidth / 2, targetAngle + arcWidth / 2);

            double x = center.x() + radius * Math.cos(angle);
            double y = center.y();
            double z = center.z() + radius * Math.sin(angle);

            spawnParticleOnSolid(level, particle, x, y, z, maxTries, step);
        }
    }

    private static void spawnParticleOnSolid(Level level, ParticleOptions particle,
                                             double x, double y, double z, int maxTries, float step) {
        int tries = 0;
        boolean foundSolid = false;

        for (; tries < maxTries; tries++) {
            BlockPos pos = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);

            if (state.getBlock() instanceof LiquidBlock) {
                shape = Shapes.block();
            }

            if (shape.isEmpty()) {
                if (foundSolid) {
                    break;
                }

                y--;
            } else {
                foundSolid = true;
                AABB bounds = shape.bounds();

                if (!bounds.move(pos).contains(new Vec3(x, y, z))) {
                    if (!(bounds.maxY >= 1.0)) {
                        break;
                    }

                    y++;
                } else {
                    y += step;
                }
            }
        }

        if (tries < maxTries) {
            level.addParticle(particle, x, y + 0.1D, z, 0, 0, 0);
        }
    }
}
