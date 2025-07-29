package it.hurts.octostudios.reliquified_lenders_cataclysm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECEntities;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ReliquifiedLendersCataclysm.MOD_ID)
public class ReliquifiedLendersCataclysm {
    public static final String MOD_ID = "reliquified_lenders_cataclysm";

    public ReliquifiedLendersCataclysm(IEventBus bus) {
        bus.addListener(this::setupCommon);

        RECItems.register(bus);
        RECEntities.register(bus);
        RECDataComponents.register(bus);
    }

    private void setupCommon(final FMLCommonSetupEvent event) {
    }
}
