package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class IgnitedShieldEntity extends Entity {
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<Float> ANGLE =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.FLOAT);

    protected static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.FLOAT);

    private long ticks;

    public IgnitedShieldEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public IgnitedShieldEntity(Level level, LivingEntity owner, Vec3 pos,
                               float health) {
        this(RECEntities.IGNITED_SHIELD.get(), level);

        setOwner(owner);
        setPos(pos);
        setHealth(health);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        LivingEntity owner = getOwner();

        if (owner == null) {
            discard();

            return;
        }

        if (getHealth() <= 0) {
            remove(RemovalReason.DISCARDED);

            return;
        }

//        float angle = getAngle() + 4.0F;
//
//        if (angle >= 360F) {
//            angle -= 360F;
//        }

//        List<IgnitedShieldEntity> shieldsIntersecting =
//                level().getEntities(this, getBoundingBox()).stream()
//                        .map(entity -> entity instanceof IgnitedShieldEntity shieldEntity ? shieldEntity : null)
//                        .filter(Objects::nonNull).toList();
//
//        for (IgnitedShieldEntity shieldOther : shieldsIntersecting) {
//            shieldOther.setAngle(shieldOther.getAngle() - 4F);
//        }

        double elapsedTicks = (level().getGameTime() - getTicks());
        float speed = 4F;
        float angle = (float) (elapsedTicks * speed);
        double angleRad = Math.toRadians(angle);
        double radius = 2.0D;

        double x = Math.cos(angleRad) * radius;
        double z = Math.sin(angleRad) * radius;

        setPos(owner.getX() + x, owner.getY(), owner.getZ() + z);
        setAngle(angle);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();

        setTicks(level().getGameTime());
    }

    // entity data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityDataBuilder) {
        entityDataBuilder.define(OWNER_UUID, Optional.empty());
        entityDataBuilder.define(ANGLE, 0F);
        entityDataBuilder.define(HEALTH, 10F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setAngle(tag.getFloat("Angle"));
        setHealth(tag.getFloat("Health"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Angle", getAngle());
        tag.putFloat("Health", getHealth());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }

    public float getHealth() {
        return entityData.get(HEALTH);
    }

    public float getAngle() {
        return entityData.get(ANGLE);
    }

    public void setHealth(float value) {
        entityData.set(HEALTH, value);
    }

    public void setAngle(float value) {
        entityData.set(ANGLE, value);
    }

    @Nullable
    public LivingEntity getOwner() {
        Optional<UUID> ownerUUID = entityData.get(OWNER_UUID);

        if (ownerUUID.isPresent()) {
            if (level().isClientSide) {
                ClientLevel clientLevel = Minecraft.getInstance().level;

                if (clientLevel != null) {
                    return clientLevel.getPlayerByUUID(ownerUUID.get());
                }
            }

            return (LivingEntity) ((ServerLevel) level()).getEntity(ownerUUID.get());
        }

        return null;
    }

    public void setOwner(@Nullable LivingEntity ownerEntity) {
        if (ownerEntity != null) {
            entityData.set(OWNER_UUID, Optional.of(ownerEntity.getUUID()));
        }
    }
}
