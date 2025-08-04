package it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.scouring_eye;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
public class ScouringRayEntity extends Entity {
    private static final EntityDataAccessor<Vector3f> FROM_POS = SynchedEntityData.defineId(ScouringRayEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> TO_POS = SynchedEntityData.defineId(ScouringRayEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(ScouringRayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ScouringRayEntity.class, EntityDataSerializers.FLOAT);

    private final double progress = 0.0D;

    private final Color startColor = new Color(139, 0, 105);
    private final Color endColor = new Color(47, 0, 97);

    private List<LivingEntity> targets;

    public ScouringRayEntity(EntityType<ScouringRayEntity> type, Level level) {
        super(type, level);

        this.noPhysics = true;
    }

    public ScouringRayEntity(Level level, List<LivingEntity> targets, Vec3 fromPos, Vec3 toPos, float width, float damage) {
        this(RECEntities.SCOURING_RAY.get(), level);

        this.targets = targets;

        setFromPos(fromPos);
        setToPos(toPos);
        setWidth(width);
        setDamage(damage);
    }

    @Override
    public void tick() {
        super.tick();

        if (getFromPos() == null || getToPos() == null) {
            return;
        }

        double distanceTotal = getFromPos().distanceTo(getToPos());
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

        Vec3 direction = getToPos().subtract(getFromPos()).normalize();
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

            double spiralRadius = Math.sin(Math.PI * progressFrac) * getWidth();
            double dx = Math.cos(angle) * spiralRadius;
            double dz = Math.sin(angle) * spiralRadius;

            Vec3 offset = right.scale(dx).add(perpendicular.scale(dz));
            Vec3 pos = getFromPos().add(direction.scale(distance)).add(offset);

            ((ServerLevel) level).sendParticles(
                    getParticle(currentProgress),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0);
        }

        // targets hitboxes

        if (getTargets().isEmpty()) {
            return;
        }

        double distanceCurrent = currentProgress * distanceTotal;
        double boxOffset = 0.5D;

        for (LivingEntity entity : getTargets()) {
            double distanceTarget = getFromPos().distanceTo(entity.getBoundingBox().getCenter());
            double delta = Math.abs(distanceCurrent - distanceTarget);

            if (delta <= boxOffset) {
                AABB box = entity.getBoundingBox().move(direction.scale(boxOffset));

                hurtEntity(level, entity, box, currentProgress);
            }
        }
    }

    private void hurtEntity(Level level, LivingEntity entity, AABB box, double progress) {
        entity.hurt(level.damageSources().magic(), getDamage());

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
        int r = (int) (getStartColor().getRed() - progress * Math.abs(getStartColor().getRed() - getEndColor().getRed()));
        int b = (int) (getStartColor().getBlue() - progress * Math.abs(getStartColor().getBlue() - getEndColor().getBlue()));

        return new Color(r, 0, b);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(FROM_POS, Vec3.ZERO.toVector3f());
        builder.define(TO_POS, Vec3.ZERO.toVector3f());
        builder.define(WIDTH, 0F);
        builder.define(DAMAGE, 0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        setFromPos(new Vec3(tag.getDouble("fromX"), tag.getDouble("fromY"), tag.getDouble("fromZ")));
        setToPos(new Vec3(tag.getDouble("toX"), tag.getDouble("toY"), tag.getDouble("toZ")));

        setWidth(tag.getFloat("width"));
        setWidth(tag.getFloat("damage"));

        this.targets = new ArrayList<>();

        if (getCommandSenderWorld() instanceof ServerLevel level) {
            ListTag targetsUUIDList = tag.getList("targets", Tag.TAG_COMPOUND);

            for (var targetTag : targetsUUIDList) {
                CompoundTag uuidTag = (CompoundTag) targetTag;

                if (level.getEntity(uuidTag.getUUID("uuid")) instanceof LivingEntity target) {
                    targets.add(target);
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putDouble("fromX", getFromPos().x);
        tag.putDouble("fromY", getFromPos().y);
        tag.putDouble("fromZ", getFromPos().z);
        tag.putDouble("toX", getToPos().x);
        tag.putDouble("toY", getToPos().y);
        tag.putDouble("toZ", getToPos().z);

        tag.putFloat("width", getWidth());
        tag.putFloat("damage", getDamage());

        ListTag targetsUUIDList = new ListTag();

        for (LivingEntity entity : getTargets()) {
            if (entity == null) {
                continue;
            }

            CompoundTag targetTag = new CompoundTag();
            targetTag.putUUID("uuid", entity.getUUID());

            targetsUUIDList.add(targetTag);
        }

        tag.put("targets", targetsUUIDList);
    }

    public void setFromPos(Vec3 pos) {
        this.getEntityData().set(FROM_POS, pos.toVector3f());
    }

    public Vec3 getFromPos() {
        return new Vec3(this.getEntityData().get(FROM_POS));
    }

    public void setToPos(Vec3 pos) {
        this.getEntityData().set(TO_POS, pos.toVector3f());
    }

    public Vec3 getToPos() {
        return new Vec3(this.getEntityData().get(TO_POS));
    }

    public void setWidth(float width) {
        this.getEntityData().set(WIDTH, width);
    }

    public float getWidth() {
        return this.getEntityData().get(WIDTH);
    }

    public void setDamage(float damage) {
        this.getEntityData().set(DAMAGE, damage);
    }

    public float getDamage() {
        return this.getEntityData().get(DAMAGE);
    }
}
