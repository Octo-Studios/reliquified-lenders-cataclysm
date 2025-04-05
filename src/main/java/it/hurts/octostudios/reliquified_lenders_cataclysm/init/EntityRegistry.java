package it.hurts.octostudios.reliquified_lenders_cataclysm.init;

import it.hurts.octostudios.reliquified_lenders_cataclysm.ReliquifiedLendersCataclysm;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.ScreenShakeSoundedEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReliquifiedLendersCataclysm.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ScreenShakeSoundedEntity>> SCREEN_SHAKE_SOUNDED =
            ENTITIES.register("screen_shake_sounded", () ->
            EntityType.Builder.<ScreenShakeSoundedEntity>of(ScreenShakeSoundedEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .setUpdateInterval(Integer.MAX_VALUE)
                    .noSummon()
                    .build("screen_shake_sounded")
    );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
