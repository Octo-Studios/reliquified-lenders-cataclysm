package it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;


public class VoidRuneModifiedEntity extends Void_Rune_Entity {
    @Setter
    private int warmupDelayTicks;
    @Setter
    private boolean attractionActivated = false;

    private boolean sentSpikeEvent;
    @Setter
    private int lifeTicks;
    private boolean clientSideAttackStarted;

    private LivingEntity target;
    private UUID targetUUID;

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
        setTarget(target);
    }

    @Override
    public void tick() {
        super.baseTick();

        prevactivateProgress = activateProgress;

        Level level = getCommandSenderWorld();

//        if (!level.isClientSide()) {
//            double scaleMax = 0.15D;
//            double movementScale = scaleMax - 0.5D * scaleMax * target.distanceTo(this);
//
//            Vec3 direction = target.position().subtract(position()).normalize().scale(movementScale);
//            Vec3 motion = target.getDeltaMovement().subtract(direction);
//
//            LivingEntity target = getTarget();
//
//            if (target == null) {
//                return;
//            }
//
//            if (target instanceof ServerPlayer player && !player.equals(getCaster())) {
//                NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()), player);
//            } else {
//                target.setDeltaMovement(motion);
//
//                // prevent getting stuck in blocks while vortex pulling
//                if (target.horizontalCollision) {
//                    getTarget().addDeltaMovement(new Vec3(0D, 0.25D, 0D));
//                }
//            }
//        }

        if (level.isClientSide) {
            if (clientSideAttackStarted) {
                --lifeTicks;

                if (!isActivate() && activateProgress < 10.0F) {
                    activateProgress++;
                }

                int i;

                if (lifeTicks == 33) {
                    for (i = 0; i < 80; i++) {
                        double x = getX() + (getRandom().nextDouble() * 2D - 1D) * getBbWidth() * 0.5D;
                        double y = getY() + 0.03D;
                        double z = getZ() + (getRandom().nextDouble() * 2D - 1D) * getBbWidth() * 0.5D;
                        double ox = getRandom().nextGaussian() * 0.07D;
                        double oy = getRandom().nextGaussian() * 0.07D;
                        double oz = getRandom().nextGaussian() * 0.07D;

                        level.addParticle(
                                ParticleUtils.constructSimpleSpark(new Color(255, 0, 0),
                                        0.25F, 20, 0.5F),
                                x, y, z, ox, oy, oz);
                    }
                }

                if (lifeTicks == 14) {
                    setActivate(true);

                    for (i = 0; i < 12; i++) {
                        double x = getX() + (getRandom().nextDouble() * 2D - 1D) * getBbWidth() * 0.5D;
                        double y = getY() + 0.05D + getRandom().nextDouble();
                        double z = getZ() + (getRandom().nextDouble() * 2D - 1D) * getBbWidth() * 0.5D;
                        double ox = (getRandom().nextDouble() * 2D - 1D) * 0.3D;
                        double oy = 0.3D + getRandom().nextDouble() * 0.3D;
                        double oz = (getRandom().nextDouble() * 2D - 1D) * 0.3D;

                        level.addParticle(
                                ParticleUtils.constructSimpleSpark(new Color(255, 255, 255),
                                        0.25F, 20, 0.5F),
                                x, y, z, ox, oy, oz);
                    }
                }
            }
        } else if (--warmupDelayTicks < 0) {
            if (warmupDelayTicks == -10 && isActivate()) {
                setActivate(false);
            }

            if (warmupDelayTicks < -10 && warmupDelayTicks > -30) {
                var entitiesInArea = level.getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(0.2D, 0, 0.2D));

                entitiesInArea.forEach(this::damage);
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

    @Nullable
    public LivingEntity getTarget() {
        if (target == null && targetUUID != null && getCommandSenderWorld() instanceof ServerLevel level) {
            Entity entity = level.getEntity(targetUUID);

            if (entity instanceof LivingEntity livingEntity) {
                target = livingEntity;
            }
        }

        return target;
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.target = target;
        targetUUID = target == null ? null : target.getUUID();
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

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            clientSideAttackStarted = true;

            if (!isSilent()) {
                getCommandSenderWorld().playLocalSound(getX(), getY(), getZ(),
                        ModSounds.VOID_RUNE_RISING.get(), getSoundSource(), 0.5F, random.nextFloat() * 0.2F + 0.85F, false);
            }
        }
    }
}
