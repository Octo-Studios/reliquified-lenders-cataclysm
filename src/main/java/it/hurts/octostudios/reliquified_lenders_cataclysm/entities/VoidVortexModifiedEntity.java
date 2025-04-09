package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import com.github.L_Ender.cataclysm.client.particle.Options.StormParticleOptions;
import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.EntityRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class VoidVortexModifiedEntity extends Entity {
    protected static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> HEIGHT =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> LIFESPAN =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTER =
            SynchedEntityData.defineId(VoidVortexModifiedEntity.class, EntityDataSerializers.INT);

    private float maxCircleRadius;
    private boolean madeOpenNoise;
    private boolean madeCloseNoise;

    @Nullable
    private LivingEntity owner;

    public VoidVortexModifiedEntity(EntityType<?> type, Level level) {
        super(type, level);

        setMaxCircleRadius(1.0F);
        setMadeOpenNoise(false);
        setMadeCloseNoise(false);
    }

    public VoidVortexModifiedEntity(Level level, double x, double y, double z, float yRot,
                                    LivingEntity caster, int lifespan, int height, float damage) {
        this(EntityRegistry.VOID_VORTEX_MODIFIED.get(), level);

        setLifespan(lifespan);
        setOwner(caster);
        setYRot((float) (yRot * (180F / Math.PI)));
        setPos(x, y, z);
        setHeight(height);
        setDamage(damage);

        if (!level.isClientSide) {
            this.setCasterID(caster.getId());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount == 1) {
            if (getLifespan() == 0) {
                setLifespan(100);
            }

            if (level().isClientSide) {
                setOwner((LivingEntity) level().getEntity(getCasterID()));
            }
        }

        // vortex opening
        if (!isMadeOpenNoise()) {
            gameEvent(GameEvent.ENTITY_PLACE);
            playSound(SoundEvents.END_PORTAL_SPAWN, 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
            setMadeOpenNoise(true);

            ScreenShake_Entity.ScreenShake(level(), position(), getHeight(),
                    0.02F * getHeight(), getLifespan(), 40);
        }

        if (Math.min(tickCount, getLifespan()) >= 16) {
            float r = 0.4F, g = 0.1F, b = 0.8F; // circles rgb
            float stepD = 0.25F, stepWRandom = 0.11F; // steps for width and its random
            float stepH = 1.0F, stepHRandom = 0.075F; // steps for height and its random

            for (int i = 0; i < getHeight(); i++) {
                float width = 0.75F + stepD * i + getRandom().nextFloat() * (0.25F + stepWRandom * i);
                float height = 0.5F + stepH * i + getRandom().nextFloat() * (0.45F + stepHRandom * i);

                if (i == getHeight() - 1) {
                    setMaxCircleRadius(Math.max(getMaxCircleRadius(), width));
                }

                level().addParticle(new StormParticleOptions(r, g, b, width, height, getId()),
                        getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

        if (getLifespan() > 0) {
            AABB screamBox = new AABB(getX() - getHeight(), getY(), getZ() - getHeight(),
                    getX() + getHeight(), getY() + getHeight(), getZ() + getHeight());

            for (Entity entity : getEntitiesInArea(screamBox)) {
                if (entity instanceof LivingEntity livingEntity) {
                    if (livingEntity.isDeadOrDying()) {
                        continue;
                    }

                    // turn around smoothly
                    entity.yRotO = entity.getYRot();
                    entity.setYRot((entity.getYRot() + 30.0F) % 360F);
                    entity.setYHeadRot(entity.getYRot());
                    entity.setYBodyRot(entity.getYRot());
                }

                float distanceToEntity = entity.distanceTo(this);
                double scaleMax = 0.15D;
                double movementScale = scaleMax - 0.5D * scaleMax * distanceToEntity / getHeight();
                Vec3 deltaMovement = entity.position().subtract(position()).normalize().scale(movementScale);

                entity.setDeltaMovement(entity.getDeltaMovement().subtract(deltaMovement));

                if (entity instanceof VoidVortexModifiedEntity vortexOther && getHeight() > vortexOther.getHeight()) {
                    entity.move(MoverType.SELF, entity.getDeltaMovement());


                    AABB vortexOtherBox = vortexOther.getBoundingBox();

                    // if vortices collide, remove the smaller one and upgrade parameters of bigger vertex
                    if (vortexOtherBox.intersects(getBoundingBox())
                            && vortexOtherBox.getBottomCenter().distanceTo(getBoundingBox().getBottomCenter()) <= 1.5D) {
                        setLifespan(getLifespan() + vortexOther.getLifespan());
                        setHeight(getHeight() + vortexOther.getHeight());
                        setDamage(getDamage() + vortexOther.getDamage());

                        vortexOther.gameEvent(GameEvent.ENTITY_PLACE);
                        entity.remove(RemovalReason.DISCARDED);

                        level().playSound(null, blockPosition(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL);
                    }
                }
            }
        }

        // vortex closing
        if (getLifespan() <= 16 && !isMadeCloseNoise()) {
            gameEvent(GameEvent.ENTITY_PLACE);
            setMadeCloseNoise(true);
        }

        if (getLifespan() <= 0) {
            if (level().isClientSide) {
                spawnExplodeParticles();
            }

            damageMobs(getMaxCircleRadius());

            remove(RemovalReason.DISCARDED);
        }

        setLifespan(getLifespan() - 1);
    }

    private void spawnExplodeParticles() {
        for (int i = 0; i < getHeight(); i++) {
            double xMax = (getRandom().nextDouble() * 2.0D - 1.0D) * 0.3D;
            double yMax = 0.3D + getRandom().nextDouble() * 0.3D;
            double zMax = (getRandom().nextDouble() * 2.0D - 1.0D) * 0.3D;

            level().addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), xMax, yMax, zMax);
        }
    }

    private void damageMobs(float radius) {
        AABB damageBox = new AABB(getX() - radius, getY(), getZ() - radius,
                getX() + radius, getY() + getHeight(), getZ() + radius);

        for (LivingEntity entity : ItemUtils.getEntitiesInArea(getOwner(), level(), damageBox)) {
            Vec3 deltaMovement = position().subtract(entity.position()).normalize();

            entity.setDeltaMovement(entity.getDeltaMovement().subtract(deltaMovement));
            entity.hurt(damageSources().magic(), getDamage());
        }

        level().playSound(null, blockPosition(),
                ModSounds.EXPLOSION.get(), SoundSource.NEUTRAL);
    }

    private List<Entity> getEntitiesInArea(AABB area) {
        return level().getEntitiesOfClass(Entity.class, area).stream()
                .map(entity -> !entity.equals(getOwner()) && !entity.isDescending()
                        && (entity instanceof LivingEntity || entity instanceof VoidVortexModifiedEntity)
                        ? entity : null)
                .filter(Objects::nonNull).toList();
    }

    protected void defineSynchedData(SynchedEntityData.Builder entityDataBuilder) {
        entityDataBuilder.define(DAMAGE, 20F);
        entityDataBuilder.define(HEIGHT, 7);
        entityDataBuilder.define(LIFESPAN, 300);
        entityDataBuilder.define(CASTER, -1);
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        setDamage(tag.getFloat("Damage"));
        setHeight(tag.getInt("Height"));
        setLifespan(tag.getInt("Lifespan"));
        setCasterID(tag.getInt("CasterId"));
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", getDamage());
        tag.putInt("Height", getHeight());
        tag.putInt("Lifespan", getLifespan());
        tag.putInt("CasterId", getCasterID());
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

    public int getLifespan() {
        return entityData.get(LIFESPAN);
    }

    public void setLifespan(int value) {
        entityData.set(LIFESPAN, value);
    }

    public int getCasterID() {
        return entityData.get(CASTER);
    }

    public void setCasterID(int id) {
        entityData.set(CASTER, id);
    }

    @Nullable
    public LivingEntity getOwner() {
        if (owner == null && getCasterID() != 0 && level() instanceof ServerLevel) {
            Entity entity = level().getEntity(getCasterID());

            if (entity instanceof LivingEntity livingEntity) {
                setOwner(livingEntity);
            }
        }

        return owner;
    }

    public void setOwner(@Nullable LivingEntity ownerEntity) {
        this.owner = ownerEntity;
        this.setCasterID(ownerEntity == null ? 0 : ownerEntity.getId());
    }
}
