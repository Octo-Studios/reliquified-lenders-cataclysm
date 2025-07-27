package it.hurts.octostudios.reliquified_lenders_cataclysm.items.base;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;

public class RECItem extends RelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return null;
    }

    @Override
    public String getConfigRoute() {
        return ReliquifiedLendersCataclysm.MOD_ID;
    }
}
