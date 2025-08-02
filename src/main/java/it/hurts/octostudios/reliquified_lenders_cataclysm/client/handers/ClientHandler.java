package it.hurts.octostudios.reliquified_lenders_cataclysm.client.handers;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.entities.IgnitedShieldRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.entities.VoidVortexModifiedRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.sskirillss.relics.client.renderer.entities.NullRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ReliquifiedLendersCataclysm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RECEntities.SCOURING_RAY.get(), NullRenderer::new);
        event.registerEntityRenderer(RECEntities.VOID_VORTEX_MODIFIED.get(), VoidVortexModifiedRenderer::new);
        event.registerEntityRenderer(RECEntities.IGNITED_SHIELD.get(), IgnitedShieldRenderer::new);
    }
}
