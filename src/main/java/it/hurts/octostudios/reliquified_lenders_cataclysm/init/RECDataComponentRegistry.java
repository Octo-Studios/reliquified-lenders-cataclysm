package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import com.mojang.serialization.Codec;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RECDataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ReliquifiedLendersCataclysm.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TARGET_ID =
            registerIntComponent("target_id");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VORTEX_ID =
            registerIntComponent("vortex_id");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VOID_RUNE_TIME =
            registerIntComponent("void_rune_time");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> registerIntComponent(String name) {
        return DATA_COMPONENTS.register(name, () ->
                DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .build());
    }

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
