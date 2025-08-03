package it.hurts.octostudios.reliquified_lenders_cataclysm.client.handers;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.entities.IgnitedShieldRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.client.renderer.entities.VoidVortexModifiedRenderer;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils;
import it.hurts.sskirillss.relics.client.renderer.entities.NullRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ReliquifiedLendersCataclysm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(RECItems.SCOURING_EYE.get(), ResourceLocation.fromNamespaceAndPath(ReliquifiedLendersCataclysm.MOD_ID, "targeted"),
                    (stack, level, entity, id) -> ScouringEyeUtils.getTargetUUID(stack).isEmpty() ? 0 : 1);
        });
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RECEntities.SCOURING_RAY.get(), NullRenderer::new);
        event.registerEntityRenderer(RECEntities.VOID_VORTEX_MODIFIED.get(), VoidVortexModifiedRenderer::new);
        event.registerEntityRenderer(RECEntities.IGNITED_SHIELD.get(), IgnitedShieldRenderer::new);
    }
}
