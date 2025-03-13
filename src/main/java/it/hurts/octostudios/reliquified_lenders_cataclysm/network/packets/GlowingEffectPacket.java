package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GlowingEffectPacket(int targetId) implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, GlowingEffectPacket> STREAM_CODEC =
            StreamCodec.ofMember(GlowingEffectPacket::write, GlowingEffectPacket::decode);
    public static final Type<GlowingEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID, "glowing_effect"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void write(ByteBuf data) {
        data.writeInt(targetId);
    }

    public static GlowingEffectPacket decode(ByteBuf buf) {
        return new GlowingEffectPacket(buf.readInt());
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().getCommandSenderWorld();

            if (!level.isClientSide() || !(level.getEntity(targetId) instanceof LivingEntity target)) {
                return;
            }

            target.setGlowingTag(true);
        });
    }
}
