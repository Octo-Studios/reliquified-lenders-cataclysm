package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RECEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReliquifiedLendersCataclysm.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ScreenShakeSoundedEntity>> SCREEN_SHAKE_SOUNDED =
            ENTITIES.register("screen_shake_sounded", () ->
            EntityType.Builder.<ScreenShakeSoundedEntity>of(ScreenShakeSoundedEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .setUpdateInterval(Integer.MAX_VALUE)
                    .noSummon()
                    .build("screen_shake_sounded")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<VoidShardModifiedEntity>> VOID_SHARD_MODIFIED =
            ENTITIES.register("void_shard_modified", () ->
                    EntityType.Builder.<VoidShardModifiedEntity>of(VoidShardModifiedEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .setUpdateInterval(20)
                            .clientTrackingRange(4)
                            .noSummon()
                            .build("void_shard_modified")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<VoidVortexModifiedEntity>> VOID_VORTEX_MODIFIED =
            ENTITIES.register("void_vortex_modified", () ->
                    EntityType.Builder.<VoidVortexModifiedEntity>of(VoidVortexModifiedEntity::new, MobCategory.MISC)
                            .sized(2.5F, 0.5F)
                            .setUpdateInterval(Integer.MAX_VALUE)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("void_vortex_modified")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<FlameJetModifiedEntity>> FLAME_JET_MODIFIED =
            ENTITIES.register("flame_jet_modified", () ->
                    EntityType.Builder.<FlameJetModifiedEntity>of(FlameJetModifiedEntity::new, MobCategory.MISC)
                            .sized(0.6F, 2.5F)
                            .clientTrackingRange(6)
                            .setUpdateInterval(2)
                            .fireImmune()
                            .build("flame_jet_modified")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<IgnitedShieldEntity>> IGNITED_SHIELD =
            ENTITIES.register("ignited_shield", () ->
                    EntityType.Builder.<IgnitedShieldEntity>of(IgnitedShieldEntity::new, MobCategory.MISC)
                            .sized(1.0F, 2.0F)
                            .noSummon()
                            .fireImmune()
                            .build("ignited_shield")
            );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
