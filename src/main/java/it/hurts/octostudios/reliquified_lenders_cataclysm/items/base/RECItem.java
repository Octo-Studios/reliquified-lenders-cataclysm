package it.hurts.octostudios.reliquified_lenders_cataclysm.items.base;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;

public class RECItem extends RelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return null;
    }

    @Override
    public String getConfigRoute() {
        return ReliquifiedLendersCataclysm.MOD_ID;
    }
}
