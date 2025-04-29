package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathRandomUtils.randomized;

public record VolcanoParticlesPacket(int entityId) implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VolcanoParticlesPacket> STREAM_CODEC =
            StreamCodec.ofMember(VolcanoParticlesPacket::encode, VolcanoParticlesPacket::decode);
    public static final Type<VolcanoParticlesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "volcano_particles"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VolcanoParticlesPacket decode(ByteBuf buf) {
        return new VolcanoParticlesPacket(buf.readInt());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            if (!(level.getEntity(entityId) instanceof LivingEntity entity)) {
                return;
            }

            spawnVolcanoParticles(entity);
        });
    }

    private static void spawnVolcanoParticles(LivingEntity entity) {
        RandomSource random = entity.getRandom();
        Color color = new Color(255, 255 * (int) random.nextFloat(), 0); // somewhere between red & yellow

        entity.getCommandSenderWorld()
                .addParticle(ParticleUtils.constructSimpleSpark(color,
                                0.4F,
                                20 + randomized(random, 10),
                                0.8F + randomized(random, 0.1F)),
                        entity.getX() + randomized(random, 0.5D),
                        entity.getY() + 0.1D,
                        entity.getZ() + randomized(random, 0.5D),
                        0, 0, 0);
    }
}
