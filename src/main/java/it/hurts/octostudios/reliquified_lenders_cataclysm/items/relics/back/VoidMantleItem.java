package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle.VoidRuneModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECEntityUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.awt.*;
import java.util.List;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidMantleUtils.*;

@EventBusSubscriber
public class VoidMantleItem extends RECItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .rankModifier(1, RankModifier.getModifierByRank(1))
                                .rankModifier(3, RankModifier.getModifierByRank(3))
                                .rankModifier(5, RankModifier.getModifierByRank(5))
                                .modes("enabled", "disabled")
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(30D, 28D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0234D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("radius")
                                        .initialValue(4D, 5D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.3143D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("stun_time")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("attraction_force")
                                        .initialValue(2D, 2.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("zone_radius")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.3714D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("waves")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.3714D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("zone_damage")
                                        .initialValue(1.5D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.114D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .maxRank(5)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, RECLootEntries.FROSTED_PRISON,
                                LootEntries.THE_END)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFB98FD2)
                                .borderBottom(0xFF895DA4)
                                .textured(true)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide || stack.isEmpty() || isRuneDisabled(entity, stack)) {
            return;
        }

        int runeCooldown = stack.getOrDefault(RECDataComponents.VOID_RUNE_TIME, 0);

        // firstly check cd
        if (runeCooldown > 20) {
            runeCooldown--;
        } else {
            float radius = getRadiusStat(entity, stack);
            List<LivingEntity> entitiesInArea = RECEntityUtils.getEntitiesInArea(entity, level, radius);

            double minDistance = radius;
            LivingEntity runeTarget = null;

            // rune target defining
            for (LivingEntity entityInArea : entitiesInArea) {

                if (entityInArea == null || entityInArea.equals(entity)
                        || entityInArea instanceof ArmorStand || EntityUtils.isAlliedTo(entity, entityInArea)) {
                    continue;
                }

                double distance = entity.position()
                        .subtract(entityInArea.position().add(entityInArea.getDeltaMovement()))
                        .length();

                if (distance > minDistance) {
                    continue;
                }

                /* if mob -> check if its target is caster, define target
                else -> it's 100% non-ally player, define target
                break the loop after rune spawn to prevent multiple runes */
                if (entityInArea instanceof Mob mob) {
                    LivingEntity targetEntity = mob.getTarget();

                    if (targetEntity != null && targetEntity.is(entity)) {
                        runeTarget = entityInArea;

                        minDistance = distance;
                    }
                } else {
                    runeTarget = entityInArea;

                    minDistance = distance;
                }

                if (runeTarget != null) {
                    break;
                }
            }

            if (runeTarget != null) {
                if (runeCooldown > 0) {
                    drawRuneCircle((ServerLevel) level, runeTarget, runeCooldown);

                    runeCooldown--;
                } else {
                    spawnVoidRune(level, entity, runeTarget, stack);

                    runeCooldown = RECItemUtils.getCooldownStat(entity, stack, ABILITY_ID); // reset cd
                }
            }
        }

        // set new cd to data component
        stack.set(RECDataComponents.VOID_RUNE_TIME, runeCooldown);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof VoidRuneModifiedEntity runeEntity)
                || runeEntity.isSummonedWithZone()) {
            return;
        }

        LivingEntity caster = runeEntity.getCaster();
        ItemStack stack = EntityUtils.findEquippedCurio(caster, RECItems.VOID_MANTLE.get());

        if (stack.isEmpty()) {
            return;
        }

        LivingEntity target = event.getEntity();

        // fixed damaging entity with its seismic zone after the death
        if (caster != null && caster.getUUID().equals(target.getUUID())) {
            event.setCanceled(true);
        }

        // rank 1 - paralysis
        if (isRankModifierUnlocked(caster, stack, 1)) {
            target.addEffect(new MobEffectInstance(RelicsMobEffects.PARALYSIS, getStunStatTicks(caster, stack)), caster);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof VoidRuneModifiedEntity runeEntity)
                || runeEntity.isSummonedWithZone()) {
            return;
        }

        LivingEntity caster = runeEntity.getCaster();

        if (caster == null) {
            return;
        }

        Level level = caster.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(caster, RECItems.VOID_MANTLE.get());

        // rank 5 - seismic zone

        if (stack.isEmpty() || !isRankModifierUnlocked(caster, stack, 5)
                || isRuneDisabled(caster, stack)) {
            return;
        }

        LivingEntity entityDead = event.getEntity();

        level.explode(caster, entityDead.getX(), entityDead.getY(), entityDead.getZ(),
                1.0F, false, Level.ExplosionInteraction.NONE);

        spawnSeismicZone(level, caster, entityDead, stack);
    }
}
