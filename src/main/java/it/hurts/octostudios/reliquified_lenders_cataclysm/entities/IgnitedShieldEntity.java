package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.EntityRegistry;
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

public class IgnitedShieldEntity extends Entity {
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<Float> ANGLE =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.FLOAT);

    protected static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(IgnitedShieldEntity.class, EntityDataSerializers.FLOAT);

    public IgnitedShieldEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public IgnitedShieldEntity(Level level, LivingEntity owner, Vec3 pos,
                               float health) {
        this(EntityRegistry.IGNITED_SHIELD.get(), level);

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

        float angle = getAngle() + 4.0F;

        if (angle >= 360F) {
            angle -= 360F;
        }

        double angleRad = Math.toRadians(angle), radius = 2.0D;
        double x = Math.cos(angleRad) * radius;
        double z = Math.sin(angleRad) * radius;

        setPos(owner.getX() + x, owner.getY(), owner.getZ() + z);
        setAngle(angle);
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
