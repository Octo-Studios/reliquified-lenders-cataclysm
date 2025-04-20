package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server;

import com.github.L_Ender.cataclysm.init.ModEffect;
import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MaskOfRageMotionPacket(int targetId, double x, double z, float damage, int duration)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, MaskOfRageMotionPacket> STREAM_CODEC =
            StreamCodec.ofMember(MaskOfRageMotionPacket::encode, MaskOfRageMotionPacket::decode);
    public static final Type<MaskOfRageMotionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "mask_of_rage_motion"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MaskOfRageMotionPacket decode(ByteBuf buf) {
        return new MaskOfRageMotionPacket(buf.readInt(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readInt());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(targetId);
        buf.writeDouble(x);
        buf.writeDouble(z);
        buf.writeFloat(damage);
        buf.writeInt(duration);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            if (level.isClientSide() || !(level.getEntity(targetId) instanceof LivingEntity targetEntity)) {
                return;
            }

            pushTarget(player, targetEntity);
        });
    }

    private void pushTarget(LivingEntity sourceEntity, LivingEntity targetEntity) {
        if (hurtTarget(targetEntity, sourceEntity)) {
            double strength = 4.0D;

            targetEntity.push(x * strength, 0.0D, z * strength);
            targetEntity.addEffect(
                    new MobEffectInstance(ModEffect.EFFECTBONE_FRACTURE, duration));


            // add increased damage if entity hits the wall
            CompoundTag tag = targetEntity.getPersistentData();

            tag.putInt("PushTicks", 10);
            tag.putFloat("PushDamage", damage);
        }
    }

    private boolean hurtTarget(LivingEntity targetEntity, LivingEntity sourceEntity) {
        return targetEntity.hurt(sourceEntity.damageSources().mobAttack(sourceEntity), damage);
    }
}
