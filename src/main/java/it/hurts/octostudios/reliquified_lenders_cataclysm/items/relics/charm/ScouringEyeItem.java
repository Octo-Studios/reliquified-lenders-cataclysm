package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.MathUtils;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.PredicateType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils.*;

@EventBusSubscriber
public class ScouringEyeItem extends RECItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .active(CastData.builder()
                                        .type(CastType.INSTANTANEOUS)
                                        .predicate("target", PredicateType.CAST,
                                                (player, stack) -> !getTargetUUID(stack).isEmpty())
                                        .predicate("safe_tp", PredicateType.CAST,
                                                (player, stack) -> isTeleportSafe(stack))
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(20D, 15D)
                                        .upgradeModifier(UpgradeOperation.ADD, -1D)
                                        .formatValue(MathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("glowing_limit")
                                        .initialValue(25D, 30D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(MathUtils::roundInt)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .step(100)
                        .maxLevel(10)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData
                                        .abilityBuilder(ABILITY_ID)
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .build())
                        .beams(BeamsData.builder()
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) ) {
            return;
        }

        LivingEntity entity = getEntityFromStack(player.getCommandSenderWorld(), stack);

        if (entity == null) {
            return;
        }

        // reset all data when entity's dead
        if (entity.isDeadOrDying()) {
            setGlowingLimit(stack, getGlowingLimitStat(stack));
            setTargetUUID(stack, "");
            setTeleportSafe(stack, false);

            return;
        }

        // handle glowing time limit

        int glowingLimit = getGlowingLimit(stack);

        if (glowingLimit >= 0) {
            glowingLimit--;

            if (glowingLimit > 0) {
                entity.setGlowingTag(true);
            } else if (glowingLimit == 0) {
                // play sound when limit exceeded
                player.getCommandSenderWorld().playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1F, 1F);
            }

            setGlowingLimit(stack, glowingLimit);
        } else {
            entity.setGlowingTag(false);
        }

        // safe tp predicate
        setTeleportSafe(stack, getTeleportPos(player, entity) != null);
    }

    /**
     * Ability {@code glowing_scour} <b>[active]</b>: tp player to the attacked entity, then set the cooldown
     */
    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (!ability.equals(ABILITY_ID)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            return;
        }

        BlockPos teleportPos = getTeleportPos(player, target);

        // if no safe pos found, reset this predicate
        if (teleportPos == null) {
            setTeleportSafe(stack, false);

            return;
        }

        teleportToTarget(player, target, teleportPos);
        setAbilityCooldown(stack, ABILITY_ID, ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }

    /**
     * Ability {@code glowing_scour} <b>[active]</b>:
     * write data of the attacked entity to the relic, then start glowing the entity on client for a while
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent.Post event) {
        Player player = null;

        if (event.getSource().getEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof Player ownerPlayer) {
            player = ownerPlayer;
        } else if (event.getSource().getEntity() instanceof Player sourcePlayer) {
            player = sourcePlayer;
        }

        if (player == null) {
            return;
        }

        Level level = player.getCommandSenderWorld();
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());
        LivingEntity target = event.getEntity();

        if (level.isClientSide || stack.isEmpty() || target.isDeadOrDying()) {
            return;
        }

        // set new glowing time limit on each attack
        setGlowingLimit(stack, getGlowingLimitStat(stack));
        setTargetUUID(stack, target.getUUID().toString());
    }
}
