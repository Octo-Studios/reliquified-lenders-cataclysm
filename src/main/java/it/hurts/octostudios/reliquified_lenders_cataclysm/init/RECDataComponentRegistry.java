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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VORTEX_ID =
            registerIntComponent("vortex_id");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VOID_RUNE_TIME =
            registerIntComponent("void_rune_time");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ATTACK_BLOCKS =
            registerIntComponent("attack_blocks");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TARGET_UUID =
            registerStringComponent("target_uuid");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> registerIntComponent(String name) {
        return DATA_COMPONENTS.register(name, () ->
                DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<String>> registerStringComponent(String name) {
        return DATA_COMPONENTS.register(name, () ->
                DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .build());
    }

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
