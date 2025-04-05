package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import com.mojang.serialization.Codec;
import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;

public class RECDataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ReliquifiedLendersCataclysm.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GLOWING_TIME =
            registerIntComponent("glowing_time");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VOID_RUNE_TIME =
            registerIntComponent("void_rune_time");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VORTEX_ID =
            registerIntComponent("vortex_id");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ATTACK_BLOCKS =
            registerIntComponent("attack_blocks");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TARGET_UUID =
            registerStringComponent("target_uuid");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TP_SAFE =
            registerBoolComponent("tp_safe");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PLAYER_DIED =
            registerBoolComponent("player_died");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<UUID>>> SLOWED_ENTITIES =
            DATA_COMPONENTS.register("slowed_entities", () ->
                    DataComponentType.<List<UUID>>builder()
                            .persistent(UUIDUtil.CODEC.listOf())
                            .build());

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

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> registerBoolComponent(String name) {
        return DATA_COMPONENTS.register(name, () ->
                DataComponentType.<Boolean>builder()
                        .persistent(Codec.BOOL)
                        .build());
    }

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
