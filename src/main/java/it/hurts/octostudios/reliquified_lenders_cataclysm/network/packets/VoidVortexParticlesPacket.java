package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidVortexModifiedEntity;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public record VoidVortexParticlesPacket(int vortexId, int targetId, int particlesID)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VoidVortexParticlesPacket> STREAM_CODEC =
            StreamCodec.ofMember(VoidVortexParticlesPacket::encode, VoidVortexParticlesPacket::decode);
    public static final Type<VoidVortexParticlesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "void_vortex_particles"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VoidVortexParticlesPacket decode(ByteBuf buf) {
        return new VoidVortexParticlesPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(vortexId);
        buf.writeInt(targetId);
        buf.writeInt(particlesID);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();

            if (!level.isClientSide || !(level.getEntity(vortexId) instanceof VoidVortexModifiedEntity vortex)) {
                return;
            }

            switch (particlesID) {
                case 0 -> {
                    if (!(level.getEntity(targetId) instanceof LivingEntity entity)) {
                        return;
                    }

                    spawnPullParticles(level, entity, vortex);
                }
                case 1 -> spawnDamageParticles(level, vortex);
            }
        });
    }

    private static void spawnPullParticles(Level level, Entity entity, VoidVortexModifiedEntity vortex) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
            return;
        }

        Vec3 entityPos = entity.position();
        double step = 0.3F;

        // if entity in vortex, draw a ring around entity, else draw a "line" towards the vortex
        if (entity.getBoundingBox().intersects(vortex.getBoundingBox())) {
            float radius = 0.7F;
            int particlesNum = (int) (2 * Math.PI * radius / step);
            double angleIncrement = 2 * Math.PI / particlesNum;

            for (int i = 0; i < particlesNum; i++) {
                double angle = i * angleIncrement;

                double x = entityPos.x + radius * Math.cos(angle);
                double y = entityPos.y + 0.5D + (vortex.randomized(2.0D) - 1.0D);
                double z = entityPos.z + radius * Math.sin(angle);

                level.addParticle(vortex.getParticle(new Color(105, 0, 229)),
                        x, y, z, 0, 0, 0);
            }
        } else {
            Vec3 direction = vortex.position().subtract(entityPos).normalize();

            for (int i = 0; i < 8; i++) {
                Vec3 pos = entityPos.add(direction.scale(i * step));

                double y = pos.y;

                // if entity is another vortex, draw line with a height of vortex, else a common line
                if (entity instanceof VoidVortexModifiedEntity voidVortexEntity) {
                    y += voidVortexEntity.getHeight() * vortex.randomized(1.0D);
                } else {
                    y += 0.4D + vortex.randomized(0.7D);
                }

                level.addParticle(vortex.getParticle(new Color(112, 0, 156)),
                        pos.x + vortex.randomized(0.8D), y, pos.z + vortex.randomized(0.8D),
                        0, 0, 0);
            }
        }
    }

    // todo
    private void spawnDamageParticles(Level level, VoidVortexModifiedEntity vortex) {
//        int particlesNum;
//
//        for (int j = 0; j < particlesNum; j++) {
//        }
    }
}
