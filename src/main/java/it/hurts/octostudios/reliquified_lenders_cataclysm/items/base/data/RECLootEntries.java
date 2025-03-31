package it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootEntry;

public class RECLootEntries {
    public static final LootEntry CURSED_PYRAMID = LootEntry.builder()
            .dimension("minecraft:overworld")
            .biome(".*")
            .table("cataclysm:archaeology/cursed_pyramid", "cataclysm:archaeology/cursed_pyramid_necklace")
            .weight(500)
            .build();

    public static final LootEntry FROSTED_PRISON = LootEntry.builder()
            .dimension("minecraft:overworld")
            .biome(".*")
            .table("cataclysm:chests/frosted_prison_treasure")
            .weight(500)
            .build();
}
