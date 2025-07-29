package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import com.github.L_Ender.cataclysm.client.particle.Options.StormParticleOptions;
import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VoidVortexParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathRandomUtils;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class VoidVortexModifiedEntity extends Entity {
    protected static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> HEIGHT =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> LIFESPAN =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> LIFESPAN_STAT =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.FLOAT);

    private float maxCircleRadius;
    private boolean madeOpenNoise;
    private boolean madeCloseNoise;

    private UUID ownerUUID;
    private LivingEntity ownerEntity;

    public VoidVortexModifiedEntity(EntityType<?> type, Level level) {
        super(type, level);

        setMadeOpenNoise(false);
        setMadeCloseNoise(false);
    }

    public VoidVortexModifiedEntity(Level level, double x, double y, double z, float yRot,
                                    LivingEntity owner, int lifespan, int height, float damage) {
        this(RECEntities.VOID_VORTEX_MODIFIED.get(), level);

        setLifespan(lifespan);
        setLifespanStat(lifespan);
        setYRot((float) (yRot * (180F / Math.PI)));
        setOwner(owner);
        setPos(x, y, z);
        setHeight(height);
        setDamage(damage);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        setMaxCircleRadius(1.0F + 0.5F * getHeight());

        return EntityDimensions.scalable(getMaxCircleRadius(), getHeight());
    }

    @Override
    public void tick() {
        super.tick();

        refreshDimensions();

        if (tickCount == 1) {
            if (getLifespan() == 0) {
                setLifespan(100);
            }
        }

        // vortex opening
        if (!isMadeOpenNoise()) {
            gameEvent(GameEvent.ENTITY_PLACE);
            playSound(SoundEvents.END_PORTAL_SPAWN, 1.0F, 1.0F + this.random.nextFloat() * 0.2F);

            setMadeOpenNoise(true);

            // spawn particles on first explode
            if (level().isClientSide) {
                spawnExplodeParticles();
            }
        }

        if ((int) Math.min(tickCount, getLifespan()) >= 16 && level().isClientSide) {
            float r = 0.4F, g = 0.1F, b = 0.8F; // circles rgb
            float stepD = 0.25F, stepWRandom = 0.11F; // steps for width and its random
            float stepH = 1.0F, stepHRandom = 0.075F; // steps for height and its random

            for (int i = 0; i < getHeight(); i++) {
                float width = 0.75F + stepD * i + getRandom().nextFloat() * (0.25F + stepWRandom * i);
                float height = 0.5F + stepH * i + getRandom().nextFloat() * (0.45F + stepHRandom * i);

                LocalPlayer player = Minecraft.getInstance().player;
                Vec3 pos = position();

                // fix of particles disappearing
                if (player != null) {
                    Vector3f lookVec = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
                    Vec3 direction = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
                    pos = player.getEyePosition().add(direction.scale(8D))
                            .add(0, 1.5D, 0);
                }

                level().addParticle(new StormParticleOptions(r, g, b, width, height, getId()),
                        pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
            }
        }

        if (getLifespan() > 0 && !level().isClientSide) {
            for (Entity entity : getEntitiesInVortex()) {
                if (entity instanceof LivingEntity livingEntity) {
                    if (livingEntity.isDeadOrDying()) {
                        continue;
                    }
                }

                double scaleMax = 0.15D;
                double movementScale = scaleMax - 0.5D * scaleMax * entity.distanceTo(this) / getHeight();
                Vec3 direction = entity.position().subtract(position()).normalize().scale(movementScale);
                Vec3 motion = entity.getDeltaMovement().subtract(direction);

                if (entity instanceof ServerPlayer player && !player.equals(getOwner())) {
                    NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()), player);
                } else {
                    double vortexMotionScale = 0.8D - 0.025D * (getHeight() - 3); // move vortices slower than other entities

                    entity.setDeltaMovement(entity instanceof LivingEntity ? motion : motion.scale(vortexMotionScale));

                    // prevent getting stuck in blocks while vortex pulling
                    if (entity.horizontalCollision) {
                        entity.addDeltaMovement(new Vec3(0D, 0.25D, 0D));
                    }
                }

                // vortices comparison priority: 1. height 2. targets num 3. lifespan
                if (entity instanceof VoidVortexModifiedEntity vortexOther && getHeight() >= vortexOther.getHeight()) {
                    int entitiesNum = getEntitiesInVortex().size();
                    int entitiesNumOther = vortexOther.getEntitiesInVortex().size();

                    if (getHeight() > vortexOther.getHeight() || getHeight() == vortexOther.getHeight()
                            && (entitiesNum > entitiesNumOther ||
                            (entitiesNum == entitiesNumOther && getLifespan() >= vortexOther.getLifespan()))) {
                        vortexOther.setLifespan(getLifespanStat());
                        vortexOther.move(MoverType.SELF, vortexOther.getDeltaMovement());

                        AABB vortexOtherBox = vortexOther.getBoundingBox();

                        // if vortices collide, remove the smaller one and upgrade parameters of bigger vertex
                        if (vortexOtherBox.intersects(getBoundingBox())
                                && vortexOtherBox.getBottomCenter().distanceTo(getBoundingBox().getBottomCenter()) <= 1.5D) {
                            setLifespan(getLifespanStat());
                            setHeight(getHeight() + vortexOther.getHeight());
                            setDamage(getDamage() + vortexOther.getDamage());

                            vortexOther.remove(RemovalReason.DISCARDED);

                            level().playSound(null, blockPosition(),
                                    SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL);

                            // spawn merge particles
                            NetworkHandler.sendToClientsTrackingEntityAndSelf(
                                    new VoidVortexParticlesPacket(getId(), -1, 1), this);
                        }
                    }
                }

                // spawn pulling particles
                NetworkHandler.sendToClientsTrackingEntityAndSelf(
                        new VoidVortexParticlesPacket(getId(), entity.getId(), 0), entity);
            }
        }

        // vortex closing
        if (getLifespan() <= 16 && !isMadeCloseNoise()) {
            gameEvent(GameEvent.ENTITY_PLACE);
            setMadeCloseNoise(true);
        }

        if (getLifespan() == 1 && level().isClientSide) {
            spawnExplodeParticles();
        }

        if (getLifespan() <= 0) {
            if (!level().isClientSide) {
                damageMobs(getMaxCircleRadius());
            }

            remove(RemovalReason.DISCARDED);
        }

        if (getLifespan() % 10 == 0) {
            ScreenShake_Entity.ScreenShake(level(), position(), getHeight(),
                    Math.clamp(0.005F * getHeight(), 0.015F, 0.1F), 1, 40);
        }

        setLifespan(getLifespan() - 1);
    }

    // utils

    private List<Entity> getEntitiesInVortex() {
        AABB area = new AABB(getX() - getHeight(), getY(), getZ() - getHeight(),
                getX() + getHeight(), getY() + getHeight(), getZ() + getHeight());

        return level().getEntitiesOfClass(Entity.class, area).stream()
                .map(entity -> !entity.equals(getOwner()) && !EntityUtils.isAlliedTo(entity, getOwner())
                        && (entity instanceof LivingEntity
                        || (entity instanceof VoidVortexModifiedEntity voidVortexEntity
                        && !this.equals(voidVortexEntity))) ? entity : null)
                .filter(Objects::nonNull).toList();
    }

    private void damageMobs(float radius) {
        AABB damageBox = new AABB(getX() - radius, getY(), getZ() - radius,
                getX() + radius, getY() + getHeight(), getZ() + radius);

        for (LivingEntity entity : ItemUtils.getEntitiesInArea(getOwner(), level(), damageBox)) {
            Vec3 deltaMovement = position().subtract(entity.position()).normalize();
            Vec3 motion = entity.getDeltaMovement().subtract(deltaMovement);

            if (entity instanceof ServerPlayer player && !player.equals(getOwner())) {
                NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()  ), player);
            } else {
                entity.setDeltaMovement(motion);
            }

            entity.hurt(damageSources().magic(), getDamage());
        }

        level().playSound(null, blockPosition(),
                ModSounds.EXPLOSION.get(), SoundSource.NEUTRAL);
    }

    public void spawnExplodeParticles() {
        for (int i = 0; i < getHeight(); i++) {
            for (int j = 0; j < 32; j++) {
                double xMax = 0.25D * (randomized(2.0D) - 1.0D);
                double yMax = 0.05D + randomized(0.1D);
                double zMax = 0.25D * (randomized(2.0D) - 1.0D);

                level().addParticle(getParticle(new Color(61, 0, 135), 0.7F),
                        getX() + Math.pow(-1, i) * randomized(2.0D),
                        getY() + 0.1D * randomized(0.5D) + i,
                        getZ() + Math.pow(-1, i) * randomized(2.0D),
                        xMax, yMax, zMax);
            }
        }
    }

    public ParticleOptions getParticle(Color color, float diameterMin) {
        return ParticleUtils.constructSimpleSpark(color,
                diameterMin + randomized(0.2F),
                20 + randomized(10),
                0.8F + randomized(0.1F));
    }

    public float randomized(float value) {
        return MathRandomUtils.randomized(getRandom(), value);
    }

    public double randomized(double value) {
        return MathRandomUtils.randomized(getRandom(), value);
    }

    public int randomized(int value) {
        return MathRandomUtils.randomized(getRandom(), value);
    }

    // entity data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityDataBuilder) {
        entityDataBuilder.define(DAMAGE, 20F);
        entityDataBuilder.define(HEIGHT, 7);
        entityDataBuilder.define(LIFESPAN, 200F);
        entityDataBuilder.define(LIFESPAN_STAT, 200F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDamage(tag.getFloat("Damage"));
        setHeight(tag.getInt("Height"));
        setLifespan(tag.getFloat("Lifespan"));
        setLifespanStat(tag.getFloat("LifespanStat"));

        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", getDamage());
        tag.putInt("Height", getHeight());
        tag.putFloat("Lifespan", getLifespan());
        tag.putFloat("LifespanStat", getLifespanStat());

        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(float value) {
        entityData.set(DAMAGE, value);
    }

    public int getHeight() {
        return entityData.get(HEIGHT);
    }

    public void setHeight(int value) {
        entityData.set(HEIGHT, value);
    }

    public float getLifespan() {
        return entityData.get(LIFESPAN);
    }

    public void setLifespan(float value) {
        entityData.set(LIFESPAN, value);
    }

    public float getLifespanStat() {
        return entityData.get(LIFESPAN_STAT);
    }

    public void setLifespanStat(float value) {
        entityData.set(LIFESPAN_STAT, value);
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerEntity != null && !ownerEntity.isRemoved()) {
            return ownerEntity;
        } else if (ownerUUID != null && level() instanceof ServerLevel serverlevel) {
            this.ownerEntity = (LivingEntity) serverlevel.getEntity(ownerUUID);

            return ownerEntity;
        }

        return null;
    }

    public void setOwner(@Nullable LivingEntity ownerEntity) {
        if (ownerEntity == null) {
            return;
        }

        this.ownerUUID = ownerEntity.getUUID();
        this.ownerEntity = ownerEntity;
    }
}
