package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.head;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server.MaskOfRageMotionPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber
public class MaskOfRageItem extends RECItem {
    private static final String ABILITY_ID = "ram_mode";

    @Getter
    @Setter
    private static int ramModeTicks = 0;

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .stat(StatTemplate.builder("speed")
                                        .initialValue(7D, 6D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), -0.033D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(4D, 5D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.1D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("effect_duration")
                                        .initialValue(5D, 7D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.329D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_NETHER)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFE4A2D)
                                .borderBottom(0xFFA30022)
                                .textured(true)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (!level.isClientSide && getRamModeTicks() == 1) {
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS);
        }

        if (!level.isClientSide) {
            return;
        }

        if (getEntitySpeed(entity) >= ItemUtils.getSpeedStat(entity, stack, ABILITY_ID)) {
            setRamModeTicks(getRamModeTicks() + 1);

            AABB entityBox = entity.getBoundingBox();

            List<LivingEntity> entitiesColliding = level.getEntities(entity, entityBox, entityOther ->
                            entityBox.intersects(entityOther.getBoundingBox())).stream()
                    .map(entityOther -> entityOther instanceof LivingEntity livingEntity
                            && !EntityUtils.isAlliedTo(livingEntity, entity) ? livingEntity : null)
                    .filter(Objects::nonNull).toList();

            if (entitiesColliding.isEmpty()) {
                return;
            }

            Vec3 motion = entity.getDeltaMovement();

            for (LivingEntity entityColliding : entitiesColliding) {
                NetworkHandler.sendToServer(new MaskOfRageMotionPacket(entityColliding.getId(),
                        motion.x, motion.z, getDamageStat(entity, stack), getEffectDurationStat(entity, stack)));

                spreadRelicExperience(entity, stack, 1);
            }
        } else {
            setRamModeTicks(0);
        }
    }

    public static double getEntitySpeed(LivingEntity entity) {
        Vec3 motion = entity.getDeltaMovement();

        return Math.sqrt(Math.pow(motion.x, 2) + Math.pow(motion.z, 2)); // blocks per tick
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(event.getEntity(), ItemRegistry.MASK_OF_RAGE.get());

        if (stack.isEmpty()) {
            return;
        }

        if (getRamModeTicks() > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        CompoundTag tag = entity.getPersistentData();
        int ticks = tag.getInt("PushTicks");

        if (ticks > 0) {
            tag.putInt("PushTicks", ticks - 1);

            Vec3 motion = entity.getDeltaMovement();

            Vec3 from = entity.position();
            Vec3 to = from.add(motion.x, 0, motion.z);

            BlockHitResult hitResult = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));

            if (hitResult.getType().equals(HitResult.Type.BLOCK)) {
                entity.hurt(level.damageSources().source(DamageTypes.FLY_INTO_WALL),
                        tag.getFloat("PushDamage"));

                tag.remove("PushTicks");
                tag.remove("PushDamage");
            }
        }
    }

    private static float getDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((MaskOfRageItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "damage");
    }

    private static int getEffectDurationStat(LivingEntity entity, ItemStack stack) {
        return (int) ((MaskOfRageItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "effect_duration") * 20;
    }
}
