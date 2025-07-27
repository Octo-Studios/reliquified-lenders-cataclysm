package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidCloakUtils.spawnSeismicZone;
import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidCloakUtils.spawnVoidRune;

@EventBusSubscriber
public class VoidCloakItem extends RECItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("void_invulnerability")
                                .build())
                        .ability(AbilityTemplate.builder("void_rune")
                                .castData(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(20D, 16D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), -0.05625D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(1.4D, 1.76D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.525D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("seismic_zone")
                                .requiredLevel(5)
                                .stat(StatTemplate.builder("radius")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(ScalingModelRegistry.ADDITIVE.get(), 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("waves")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(ScalingModelRegistry.ADDITIVE.get(), 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(1.4D, 1.76D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.525D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
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

        if (stack.isEmpty() || !isAbilityTicking(entity, stack, "void_rune")) {
            return;
        }

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int voidRuneCooldown = stack.getOrDefault(RECDataComponentRegistry.VOID_RUNE_TIME, 0);

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
                voidRuneCooldown = ItemUtils.getCooldownStat(entity, stack, "void_rune");
            }
        }

        stack.set(RECDataComponentRegistry.VOID_RUNE_TIME, voidRuneCooldown);
    }

    // add relic xp on entity damaged by void rune
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Void_Rune_Entity voidRuneEntity)) {
            return;
        }

        LivingEntity entity = voidRuneEntity.getCaster();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem relic)) {
            return;
        }

        relic.spreadRelicExperience(entity, stack, 1);
    }

    /**
     * Ability {@code void_invulnerability}: complete suppression of void damage
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getCommandSenderWorld().isClientSide) {
            return;
        }

        // fix damaging player with his seismic zone after the death
        if (event.getSource().getDirectEntity() instanceof Void_Rune_Entity voidRuneEntity) {
            LivingEntity caster = voidRuneEntity.getCaster();

            if (caster != null && caster.getUUID().equals(entity.getUUID())) {
                event.setCanceled(true);
            }
        }

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem)) {
            return;
        }

        if (entity.getY() < entity.getCommandSenderWorld().getMinBuildHeight()) {
            event.setCanceled(true);
        }
    }

    /**
     * Ability {@code seismic_zone}: spawn seismic zone of void rune if player or entity owned by player dies
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entityDead = event.getEntity();
        LivingEntity entity = null;

        if (entityDead instanceof Player) {
            entity = entityDead;
        } else if (entityDead instanceof OwnableEntity ownableEntity) {
            entity = ownableEntity.getOwner();
        }

        if (entity == null) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem relic)
                || !relic.isAbilityUnlocked(entityDead, stack, "seismic_zone")) {
            return;
        }

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        level.explode(entity, entityDead.getX(), entityDead.getY(), entityDead.getZ(),
                1.0F, false, Level.ExplosionInteraction.NONE);

        spawnSeismicZone(level, entity, entityDead, stack);

        relic.spreadRelicExperience(entity, stack, 5);
    }
}
