package it.hurts.octostudios.reliquified_lenders_cataclysm.network;

import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server.MaskOfRageMotionPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VacuumGloveParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VoidVortexMotionPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VoidVortexParticlesPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class RECNetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.NETWORK);

        registrar.playToClient(VacuumGloveParticlesPacket.TYPE, VacuumGloveParticlesPacket.STREAM_CODEC,
                VacuumGloveParticlesPacket::handle);
        registrar.playToClient(VoidVortexMotionPacket.TYPE, VoidVortexMotionPacket.STREAM_CODEC,
                VoidVortexMotionPacket::handle);
        registrar.playToClient(VoidVortexParticlesPacket.TYPE, VoidVortexParticlesPacket.STREAM_CODEC,
                VoidVortexParticlesPacket::handle);

        registrar.playToServer(MaskOfRageMotionPacket.TYPE, MaskOfRageMotionPacket.STREAM_CODEC,
                MaskOfRageMotionPacket::handle);
    }
}
