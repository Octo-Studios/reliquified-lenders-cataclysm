package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VoidCloakItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands.VacuumGloveItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm.VoidVortexInBottleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ReliquifiedLendersCataclysm.MOD_ID);

    public static final DeferredHolder<Item, ScouringEyeItem> SCOURING_EYE =
            ITEMS.register("scouring_eye", ScouringEyeItem::new);
    public static final DeferredHolder<Item, VoidVortexInBottleItem> VOID_VORTEX_IN_BOTTLE =
            ITEMS.register("void_vortex_in_bottle", VoidVortexInBottleItem::new);
    public static final DeferredHolder<Item, VoidCloakItem> VOID_CLOAK =
            ITEMS.register("void_cloak", VoidCloakItem::new);
    public static final DeferredHolder<Item, VacuumGloveItem> VACUUM_GLOVE =
            ITEMS.register("vacuum_glove", VacuumGloveItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
