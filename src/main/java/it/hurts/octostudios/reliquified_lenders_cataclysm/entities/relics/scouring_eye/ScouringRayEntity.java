package it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.scouring_eye;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Getter
public class ScouringRayEntity extends Entity {
    private final double progress = 0.0D;

    private final Color startColor = new Color(139, 0, 105);
    private final Color endColor = new Color(47, 0, 97);

    private List<LivingEntity> targets;
    private Vec3 fromPos;
    private Vec3 toPos;
    private double width;
    private float damage;

    public ScouringRayEntity(EntityType<ScouringRayEntity> type, Level level) {
        super(type, level);

        this.noPhysics = true;
    }

    public ScouringRayEntity(Level level, List<LivingEntity> targets, Vec3 fromPos, Vec3 toPos, double width, float damage) {
        this(RECEntities.SCOURING_RAY.get(), level);

        this.targets = targets;
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.width = width;
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();

        if (toPos == null) {
            discard();

            return;
        }

        double distanceTotal = fromPos.distanceTo(toPos);
        int lifetimeTicks = (int) Math.ceil(distanceTotal / 1.2D);

        if (tickCount > lifetimeTicks) {
            discard();

            return;
        }

        Level level = getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        double currentProgress = (double) tickCount / lifetimeTicks;

        Vec3 direction = toPos.subtract(fromPos).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        if (Math.abs(direction.dot(up)) > 0.99D) {
            up = new Vec3(1, 0, 0);
        }

        Vec3 right = direction.cross(up).normalize();
        Vec3 perpendicular = direction.cross(right).normalize();

        int particlesNum = 16;

        for (int i = 0; i < particlesNum; i++) {
            double progressFrac = currentProgress + (double) i / (particlesNum * lifetimeTicks);

            if (progressFrac > 1D) {
                break;
            }

            double distance = progressFrac * distanceTotal;

            double angle = 2 * Math.PI * distanceTotal * progressFrac;

            double spiralRadius = Math.sin(Math.PI * progressFrac) * width;
            double dx = Math.cos(angle) * spiralRadius;
            double dz = Math.sin(angle) * spiralRadius;

            Vec3 offset = right.scale(dx).add(perpendicular.scale(dz));
            Vec3 pos = fromPos.add(direction.scale(distance)).add(offset);

            ((ServerLevel) level).sendParticles(
                    getParticle(currentProgress),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0);
        }

        // targets hitboxes

        if (targets.isEmpty()) {
            return;
        }

        double distanceCurrent = currentProgress * distanceTotal;
        double boxOffset = 0.5D;

        for (LivingEntity entity : targets) {
            double distanceTarget = fromPos.distanceTo(entity.getBoundingBox().getCenter());
            double delta = Math.abs(distanceCurrent - distanceTarget);

            if (delta <= boxOffset) {
                AABB box = entity.getBoundingBox().move(direction.scale(boxOffset));

                hurtEntity(level, entity, box, currentProgress);
            }
        }
    }

    private void hurtEntity(Level level, LivingEntity entity, AABB box, double progress) {
        entity.hurt(level.damageSources().magic(), damage);

        drawEntityBox(level, box, progress);
    }

    private void drawEntityBox(Level level, AABB box, double progress) {
        List<Vec3> corners = getCorners(box);

        int[][] edges = {
                {0, 1}, {0, 2}, {1, 3}, {2, 3},
                {4, 5}, {4, 6}, {5, 7}, {6, 7},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        for (var edge : edges) {
            Vec3 start = corners.get(edge[0]);
            Vec3 end = corners.get(edge[1]);

            int resolution = 8;

            for (int i = 0; i <= resolution; i++) {
                Vec3 pos = start.lerp(end, (double) i / resolution);

                ((ServerLevel) level).sendParticles(
                        getParticle(progress),
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0);
            }
        }
    }

    private static @NotNull List<Vec3> getCorners(AABB box) {
        Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);

        return Arrays.asList(
                new Vec3(min.x, min.y, min.z),
                new Vec3(max.x, min.y, min.z),
                new Vec3(min.x, min.y, max.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(min.x, max.y, min.z),
                new Vec3(max.x, max.y, min.z),
                new Vec3(min.x, max.y, max.z),
                new Vec3(max.x, max.y, max.z)
        );
    }

    private ParticleOptions getParticle(double progress) {
        return ParticleUtils.constructSimpleSpark(getCurrentColor(progress),
                0.7F, 20, 0.8F);
    }

    private Color getCurrentColor(double progress) {
        int r = (int) (startColor.getRed() - progress * Math.abs(startColor.getRed() - endColor.getRed()));
        int b = (int) (startColor.getBlue() - progress * Math.abs(startColor.getBlue() - endColor.getBlue()));

        return new Color(r, 0, b);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }
}
