package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record VoidVortexMotionPacket(int targetId, double x, double y, double z)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VoidVortexMotionPacket> STREAM_CODEC =
            StreamCodec.ofMember(VoidVortexMotionPacket::encode, VoidVortexMotionPacket::decode);
    public static final Type<VoidVortexMotionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "void_vortex_motion"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VoidVortexMotionPacket decode(ByteBuf buf) {
        return new VoidVortexMotionPacket(buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(targetId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            if (!(level.getEntity(targetId) instanceof LivingEntity entity)) {
                return;
            }

            entity.setDeltaMovement(x, y, z);
        });
    }
}
