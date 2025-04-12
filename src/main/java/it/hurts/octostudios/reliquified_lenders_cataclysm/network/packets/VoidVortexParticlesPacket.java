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
                case 1 -> spawnMergeParticles(level, vortex);
            }
        });
    }

    private static void spawnPullParticles(Level level, Entity entity, VoidVortexModifiedEntity vortex) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
            return;
        }

        Vec3 entityPos = entity.position();
        double step = 0.4F + 0.05F * (vortex.getHeight() - 1);

        // if entity in vortex, draw a ring around entity, else draw a "line" towards the vortex
        if (entity.distanceTo(vortex) <= 0.5D) {
            float radius = 0.5F;
            int particlesNum = (int) (2 * Math.PI * radius / step);
            double angleIncrement = 2 * Math.PI / particlesNum;

            for (int i = 0; i < particlesNum; i++) {
                double angle = i * angleIncrement;

                double x = entityPos.x + radius * Math.cos(angle);
                double y = entityPos.y + 0.1D + vortex.randomized(entity.getBoundingBox().getYsize());
                double z = entityPos.z + radius * Math.sin(angle);

                level.addParticle(vortex.getParticle(new Color(105, 0, 229), 0.15F),
                        x, y, z, 0, 0, 0);
            }
        } else {
            Vec3 direction = vortex.position().subtract(entityPos).normalize();

            for (int i = 0; i < 8; i++) {
                Vec3 pos = entityPos.add(direction.scale(i * step));

                double y = pos.y + 0.1D;
                float diameterMin = 0.2F;

                // if entity is another vortex, draw line with a height of vortex, else with Y size of entity
                if (entity instanceof VoidVortexModifiedEntity voidVortexEntity) {
                    y += voidVortexEntity.getHeight() * vortex.randomized(1.0D);
                    diameterMin += 0.5F;
                } else {
                    y += vortex.randomized(entity.getBoundingBox().getYsize());
                }

                level.addParticle(vortex.getParticle(new Color(112, 0, 156), diameterMin),
                        pos.x + vortex.randomized(entity.getBoundingBox().getXsize()), y,
                        pos.z + vortex.randomized(entity.getBoundingBox().getZsize()),
                        0, 0, 0);
            }
        }
    }

    private static void spawnMergeParticles(Level level, VoidVortexModifiedEntity vortex) {
        for (int i = 0; i < vortex.getHeight(); i++) {
            for (int j = 0; j < 24; j++) {
                level.addParticle(vortex.getParticle(new Color(61, 0, 135), 0.4F),
                        vortex.getX() + Math.pow(-1, i) * vortex.randomized(2.0D),
                        vortex.getY() + 0.1D + i + vortex.randomized(0.4D),
                        vortex.getZ() + Math.pow(-1, i) * vortex.randomized(2.0D),
                        0, 0, 0);
            }
        }
    }
}
