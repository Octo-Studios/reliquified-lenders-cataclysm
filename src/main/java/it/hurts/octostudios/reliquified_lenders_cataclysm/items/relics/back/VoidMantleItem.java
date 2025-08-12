package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle.VoidRuneModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidMantleUtils.*;

@EventBusSubscriber
public class VoidMantleItem extends RECItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .rankModifier(1, "stun")
                                .rankModifier(3, "attraction")
                                .rankModifier(5, "seismic_zone")
                                .modes("enabled", "disabled")
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(30D, 28D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0234D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2571D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("stun_time")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("attraction_radius")
                                        .initialValue(4D, 6D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.2857D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("attraction_force")
                                        .initialValue(0.5D, 1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
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

        if (stack.isEmpty() || isVoidRuneDisabled(entity, stack)) {
            return;
        }

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int voidRuneCooldown = stack.getOrDefault(RECDataComponents.VOID_RUNE_TIME, 0);

        if (voidRuneCooldown > 0) {
            voidRuneCooldown--;
        } else {
            List<LivingEntity> entitiesInArea = ItemUtils.getEntitiesInArea(entity, level, 10D);

            boolean runeSpawned = false;

            for (LivingEntity entityInArea : entitiesInArea) {
                if (entityInArea == null || entityInArea.equals(entity)
                        || entityInArea instanceof ArmorStand || EntityUtils.isAlliedTo(entity, entityInArea)) {
                    continue;
                }

                if (entityInArea instanceof Mob mob) {
                    LivingEntity targetEntity = mob.getTarget();

                    if (targetEntity != null && targetEntity.is(entity)) {
                        spawnVoidRune(level, entity, mob, stack);
                        runeSpawned = true;

                        break;
                    }
                } else {
                    spawnVoidRune(level, entity, entityInArea, stack);
                    runeSpawned = true;

                    break;
                }
            }

            if (runeSpawned) {
                voidRuneCooldown = ItemUtils.getCooldownStat(entity, stack, ABILITY_ID);
            }
        }

        stack.set(RECDataComponents.VOID_RUNE_TIME, voidRuneCooldown);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof VoidRuneModifiedEntity runeEntity)) {
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

        // rank 1
        if (isRankModifierUnlocked(caster, stack, 1)) {
            target.addEffect(new MobEffectInstance(RelicsMobEffects.PARALYSIS, getStunStatTicks(caster, stack)), caster);
        }

        // rank 3
        if (isRankModifierUnlocked(caster, stack, 3)) {
            // activate rune attraction
            runeEntity.setAttractionActivated(true);
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Void_Rune_Entity runeEntity)) {
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

        // rank 5

        if (stack.isEmpty() || !isRankModifierUnlocked(caster, stack, 5)
                || isVoidRuneDisabled(caster, stack)) {
            return;
        }

        LivingEntity entityDead = event.getEntity();

        level.explode(caster, entityDead.getX(), entityDead.getY(), entityDead.getZ(),
                1.0F, false, Level.ExplosionInteraction.NONE);

        spawnSeismicZone(level, caster, entityDead, stack);
    }
}
