package it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server;

import io.netty.buffer.ByteBuf;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VolcanoItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record VolcanoEnergyPacket(int stackIndex)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VolcanoEnergyPacket> STREAM_CODEC =
            StreamCodec.ofMember(VolcanoEnergyPacket::encode, VolcanoEnergyPacket::decode);
    public static final Type<VolcanoEnergyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "volcano_energy"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VolcanoEnergyPacket decode(ByteBuf buf) {
        return new VolcanoEnergyPacket(buf.readInt());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(stackIndex);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();
            ItemStack stack = EntityUtils.findEquippedCurios(player, ItemRegistry.VOLCANO.get()).get(stackIndex);

            if (level.isClientSide() || stack.isEmpty()) {
                return;
            }

            consumeEnergy(stack, level, player);
        });
    }

    private static void consumeEnergy(ItemStack stack, Level level, Player player) {
        int energyNew = VolcanoItem.getEnergy(stack) - ((VolcanoItem) stack.getItem()).getConsumptionStat(stack);

        stack.set(RECDataComponentRegistry.VOLCANO_ENERGY,
                Math.max(0, energyNew));

        if (energyNew <= 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS);
        }
    }
}
