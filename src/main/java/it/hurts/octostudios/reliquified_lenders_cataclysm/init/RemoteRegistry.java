package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.IgnitedShieldRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.VoidVortexModifiedRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VolcanoItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = ReliquifiedLendersCataclysm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RemoteRegistry {
//    @SubscribeEvent
//    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
//        event.register(VolcanoItem.VolcanoTooltip.class, VolcanoItem.VolcanoClientTooltip::new);
//    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.VOID_VORTEX_MODIFIED.get(), VoidVortexModifiedRenderer::new);
        event.registerEntityRenderer(EntityRegistry.IGNITED_SHIELD.get(), IgnitedShieldRenderer::new);
    }
}
