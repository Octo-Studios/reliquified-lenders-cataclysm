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

public record VolcanoOperationPacket(int stackIndex, int operationId, boolean toggled)
        implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, VolcanoOperationPacket> STREAM_CODEC =
            StreamCodec.ofMember(VolcanoOperationPacket::encode, VolcanoOperationPacket::decode);
    public static final Type<VolcanoOperationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID,
                    "volcano_energy"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VolcanoOperationPacket decode(ByteBuf buf) {
        return new VolcanoOperationPacket(buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public void encode(ByteBuf buf) {
        buf.writeInt(stackIndex);
        buf.writeInt(operationId);
        buf.writeBoolean(toggled);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.getCommandSenderWorld();
            ItemStack stack = EntityUtils.findEquippedCurios(player, ItemRegistry.VOLCANO.get()).get(stackIndex);

            if (stack.isEmpty()) {
                return;
            }

            switch (operationId) {
                case 0 -> VolcanoItem.setToggled(stack, toggled);
                case 1 -> consumeEnergy(stack, level, player);
                case 2 -> ((VolcanoItem) stack.getItem()).spreadRelicExperience(player, stack, 1);
            }
        });
    }

    private static void consumeEnergy(ItemStack stack, Level level, Player player) {
        int energyNew = VolcanoItem.getEnergy(player, stack) - ((VolcanoItem) stack.getItem()).getConsumptionStat(player, stack);

        stack.set(RECDataComponentRegistry.VOLCANO_ENERGY,
                Math.max(0, energyNew));

        if (energyNew <= 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS);
        }
    }
}
