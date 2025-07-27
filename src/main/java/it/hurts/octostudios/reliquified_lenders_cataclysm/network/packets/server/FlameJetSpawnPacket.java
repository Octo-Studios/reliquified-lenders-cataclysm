package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server;

import com.github.L_Ender.cataclysm.init.ModParticle;
import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FlameJetSpawnPacket(double x, double y, double z)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, FlameJetSpawnPacket> STREAM_CODEC =
            StreamCodec.ofMember(FlameJetSpawnPacket::encode, FlameJetSpawnPacket::decode);
    public static final Type<FlameJetSpawnPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "flame_jet_spawn"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static FlameJetSpawnPacket decode(ByteBuf buf) {
        return new FlameJetSpawnPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            level.addAlwaysVisibleParticle(ModParticle.FLAME_JET.get(), x, y + 2D, z, 0, 0, 0);
        });
    }
}
