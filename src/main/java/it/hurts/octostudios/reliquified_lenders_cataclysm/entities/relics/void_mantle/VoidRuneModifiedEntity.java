package it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECEntityUtils;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class VoidRuneModifiedEntity extends Void_Rune_Entity {
    private int warmupDelayTicks;
    private int lifeTicks;
    private float attractionRadius = 0F;
    private double attractionForce = 0D;

    private boolean sentSpikeEvent;
    private boolean clientSideAttackStarted;

    private LivingEntity targetCached;
    private UUID targetUUID;

    private int arcProgress;
    private int lastTargetId = -1;

    public VoidRuneModifiedEntity(EntityType<? extends Void_Rune_Entity> type, Level level) {
        super(type, level);

        setLifeTicks(34);
    }

    public VoidRuneModifiedEntity(Level level, double x, double y, double z, float yRot, int delayTicks, float damage,
                                  LivingEntity caster, LivingEntity target) {
        this(RECEntities.VOID_RUNE_MODIFIED.get(), level);

        setPos(x, y, z);
        setYRot(yRot * (180F / (float) Math.PI));
        setWarmupDelayTicks(delayTicks);
        setDamage(damage);
        setCaster(caster);
        setTargetCached(target);
    }

    @Override
    public void tick() {
        super.baseTick();

        prevactivateProgress = activateProgress;

        if (isActivate() && activateProgress > 0F) {
            --activateProgress;
        }

        Level level = getCommandSenderWorld();

        if (getAttractionRadius() > 0F && level instanceof ServerLevel serverLevel) {
            LivingEntity targetFinal = getTarget();

            if (targetFinal == null) {
                return;
            }

            List<LivingEntity> entitiesInArea = RECEntityUtils.getEntitiesInArea(getCaster(), level, getAttractionRadius());

            for (var entity : entitiesInArea) {
                Vec3 direction = entity.position().subtract(position()).normalize().scale(getMovementScale(entity));
                Vec3 motion = entity.getDeltaMovement().subtract(direction);

                if (entity instanceof ServerPlayer player && !player.equals(getCaster())) {
                    NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()), player);
                } else {
                    entity.setDeltaMovement(motion);

                    if (entity.horizontalCollision) {
                        targetFinal.addDeltaMovement(new Vec3(0D, 0.25D, 0D));
                    }
                }

                drawLineParticle(serverLevel, entity);
            }
        }

        if (level.isClientSide) {
            if (isClientSideAttackStarted()) {
                --lifeTicks;

                if (!isActivate() && activateProgress < 10.0F) {
                    ++activateProgress;
                }

                int i;

                if (lifeTicks == 33) {
                    for (i = 0; i < 80; ++i) {
                        BlockState block = level.getBlockState(blockPosition().below());

                        double x = getX() + (random.nextDouble() * 2D - 1D) * (double) getBbWidth() * 0.5D;
                        double y = getY() + 0.03D;
                        double z = getZ() + (random.nextDouble() * 2D - 1D) * (double) getBbWidth() * 0.5D;
                        double ox = random.nextGaussian() * 0.07D;
                        double oy = random.nextGaussian() * 0.07D;
                        double oz = random.nextGaussian() * 0.07D;

                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block),
                                x, y, z, ox, oy, oz);
                    }
                }

                if (lifeTicks == 14) {
                    setActivate(true);

                    for (i = 0; i < randomized(6, 12); i++) {
                        double x = getX() + (random.nextDouble() * 2D - 1D) * (double) getBbWidth() * 0.5D;
                        double y = getY() + 0.05D + random.nextDouble();
                        double z = getZ() + (random.nextDouble() * 2D - 1D) * (double) getBbWidth() * 0.5D;
                        double ox = (random.nextDouble() * 2D - 1D) * 0.3D;
                        double oy = 0.3D + random.nextDouble() * 0.3D;
                        double oz = (random.nextDouble() * 2D - 1D) * 0.3D;

                        level.addParticle(
                                ParticleUtils.constructSimpleSpark(new Color(randomized(50, 100), 0, randomized(150, 200)),
                                        0.5F, 20, 1.0F),
                                x, y, z, ox, oy, oz);
                    }
                }
            }
        } else if (--warmupDelayTicks < 0) {
            if (warmupDelayTicks == -10 && isActivate()) {
                setActivate(false);
            }

            if (warmupDelayTicks < -10 && warmupDelayTicks > -30) {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(0.2D, 0, 0.2D))) {
                    damage(entity);
                }
            }

            if (!sentSpikeEvent) {
                level.broadcastEntityEvent(this, (byte) 4);
                sentSpikeEvent = true;
            }

            if (--lifeTicks < 0) {
                discard();
            }
        }
    }

    // todo: make sth else, not just a line
    private void drawLineParticle(ServerLevel level, LivingEntity target) {
        Vec3 from = getBoundingBox().getCenter();
        Vec3 delta = from.subtract(target.getBoundingBox().getCenter());

        int particlesNum = (int) (this.distanceTo(target) * 4);

        for (int i = 0; i <= particlesNum; i++) {
            double progress = i / (double) particlesNum;
            Vec3 pos = from.add(delta.scale(progress));

            level.sendParticles(
                    ParticleUtils.constructSimpleSpark(new Color(randomized(50, 120), 0, randomized(150, 220)),
                            0.5F, 20, 0.5F),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0);
        }
    }

    private void damage(LivingEntity target) {
        LivingEntity caster = getCaster();

        if (target.isAlive() && !target.isInvulnerable() && !target.equals(caster) && tickCount % 5 == 0) {
            if (caster == null) {
                target.hurt(damageSources().magic(), getDamage());
            } else if (!EntityUtils.isAlliedTo(caster, target)) {
                target.hurt(damageSources().indirectMagic(this, caster), getDamage());
            }
        }
    }

    private int randomized(int min, int max) {
        return MathUtils.randomBetween(getRandom(), min, max);
    }

    private double getMovementScale(LivingEntity target) {
        double scaleMax = getAttractionForce() / 20;
        double movementScale = scaleMax - 0.5D * scaleMax * target.distanceTo(this);

        return movementScale >= 0 ? movementScale : 0;
    }

    @Nullable
    public LivingEntity getTarget() {
        if (targetCached == null && targetUUID != null && getCommandSenderWorld() instanceof ServerLevel level) {
            Entity entity = level.getEntity(targetUUID);

            if (entity instanceof LivingEntity livingEntity) {
                targetCached = livingEntity;
            }
        }

        return targetCached;
    }

    public void setTargetCached(@Nullable LivingEntity target) {
        this.targetCached = target;
        targetUUID = target == null ? null : target.getUUID();
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            clientSideAttackStarted = true;
        }
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("Target")) {
            targetUUID = tag.getUUID("Target");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (targetUUID != null) {
            tag.putUUID("Target", targetUUID);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }
}
