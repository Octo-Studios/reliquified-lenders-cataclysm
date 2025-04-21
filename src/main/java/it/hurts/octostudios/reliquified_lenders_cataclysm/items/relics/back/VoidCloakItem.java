package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECMathUtils;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.*;
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

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidCloakUtils.*;

@EventBusSubscriber
public class VoidCloakItem extends RECItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("void_invulnerability")
                                .build())
                        .ability(AbilityData.builder("void_rune")
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(20D, 16D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.05625D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(1.4D, 1.76D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.525D)
                                        .formatValue(RECMathUtils::roundHP)
                                        .build())
                                .build())
                        .ability(AbilityData.builder("seismic_zone")
                                .requiredLevel(5)
                                .stat(StatData.builder("radius")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("waves")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(1.4D, 1.76D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.525D)
                                        .formatValue(RECMathUtils::roundHP)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .step(100)
                        .maxLevel(15)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData
                                        .abilityBuilder("void_rune")
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .source(LevelingSourceData
                                        .abilityBuilder("seismic_zone")
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, RECLootEntries.FROSTED_PRISON,
                                LootEntries.THE_END)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFB98FD2)
                                .borderBottom(0xFF895DA4)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFB98FD2)
                                .endColor(0x00F8E096)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (stack.isEmpty() || !(slotContext.entity() instanceof Player player)
                || !isAbilityTicking(stack, "void_rune")) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int voidRuneCooldown = stack.getOrDefault(RECDataComponentRegistry.VOID_RUNE_TIME, 0);

        if (voidRuneCooldown > 0) {
            voidRuneCooldown--;
        } else {
            List<LivingEntity> entitiesInArea = ItemUtils.getEntitiesInArea(player, level,
                    player.getBoundingBox().inflate(10.0D));

            boolean runeSpawned = false;

            for (LivingEntity entity : entitiesInArea) {
                if (entity == null || entity.equals(player)
                        || entity instanceof ArmorStand || EntityUtils.isAlliedTo(player, entity)) {
                    continue;
                }

                if (entity instanceof Mob mob) {
                    LivingEntity targetEntity = mob.getTarget();

                    if (targetEntity != null && targetEntity.is(player)) {
                        spawnVoidRune(level, player, mob, stack);
                        runeSpawned = true;

                        break;
                    }
                } else {
                    spawnVoidRune(level, player, entity, stack);
                    runeSpawned = true;

                    break;
                }
            }

            if (runeSpawned) {
                voidRuneCooldown = ItemUtils.getCooldownStat(stack, "void_rune");
            }
        }

        stack.set(RECDataComponentRegistry.VOID_RUNE_TIME, voidRuneCooldown);
    }

    // add relic xp on entity damaged by void rune
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Void_Rune_Entity voidRuneEntity)
                || !(voidRuneEntity.getCaster() instanceof Player player)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem relic)) {
            return;
        }

        relic.spreadRelicExperience(player, stack, 1);
    }

    /**
     * Ability {@code void_invulnerability}: complete suppression of void damage
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide) {
            return;
        }

        // fix damaging player with his seismic zone after the death
        if (event.getSource().getDirectEntity() instanceof Void_Rune_Entity voidRuneEntity) {
            LivingEntity caster = voidRuneEntity.getCaster();

            if (caster != null && caster.getUUID().equals(player.getUUID())) {
                event.setCanceled(true);
            }
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem)) {
            return;
        }

        if (player.getY() < player.getCommandSenderWorld().getMinBuildHeight()) {
            event.setCanceled(true);
        }
    }

    /**
     * Ability {@code seismic_zone}: spawn seismic zone of void rune if player or entity owned by player dies
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = null;

        if (entity instanceof Player) {
            player = (Player) entity;
        } else if (entity instanceof OwnableEntity ownableEntity) {
            LivingEntity entityOwner = ownableEntity.getOwner();

            if (!(entityOwner instanceof Player entityOwnerPlayer)) {
                return;
            }

            player = entityOwnerPlayer;
        }

        if (player == null) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem relic)
                || !relic.isAbilityUnlocked(stack, "seismic_zone")) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        level.explode(player, entity.getX(), entity.getY(), entity.getZ(),
                1.0F, false, Level.ExplosionInteraction.NONE);

        spawnSeismicZone(level, player, entity, stack);

        relic.spreadRelicExperience(player, stack, 5);
    }
}
