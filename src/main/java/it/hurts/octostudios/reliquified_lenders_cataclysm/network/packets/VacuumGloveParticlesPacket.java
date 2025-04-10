package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECParticleUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public record VacuumGloveParticlesPacket(double radius, int targetId, double x,  double y, double z)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VacuumGloveParticlesPacket> STREAM_CODEC =
            StreamCodec.ofMember(VacuumGloveParticlesPacket::encode, VacuumGloveParticlesPacket::decode);
    public static final Type<VacuumGloveParticlesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "vacuum_glove_particles"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VacuumGloveParticlesPacket decode(ByteBuf buf) {
        return new VacuumGloveParticlesPacket(buf.readDouble(), buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(ByteBuf buf) {
        buf.writeDouble(radius);
        buf.writeInt(targetId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            if (!level.isClientSide() || !(level.getEntity(targetId) instanceof LivingEntity entity)) {
                return;
            }

            // (111, 24, 157) * light purple | (108, 22, 123) ~ magenta
            //    (69, 2, 78) - dark magenta |    (48, 2, 55) - dark purple
            RECParticleUtils.createCircleSegment(
                    ParticleUtils.constructSimpleSpark(new Color(111, 24, 157),
                    0.3F, 1, 0.8F),
                    level, new Vec3(x, y, z), entity.position(), radius, 0.3F);
        });
    }
}
