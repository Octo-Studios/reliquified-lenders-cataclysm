package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathUtils;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.PredicateType;
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
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
                                        .predicate("tp_allowed", PredicateType.CAST,
                                                (player, stack) -> isTeleportAllowed(stack))
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(20D, 15D)
                                        .upgradeModifier(UpgradeOperation.ADD, -1D)
                                        .formatValue(MathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("glowing_time")
                                        .initialValue(10D, 12D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.525D)
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
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, LootEntries.THE_END)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFA171D1)
                                .borderBottom(0xFF8441AB)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF460357)
                                .endColor(0x00A827C6)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.level();

        if (level.isClientSide) {
            return;
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            return;
        }

        if (target.isDeadOrDying()) {
            resetData(stack);

            return;
        }

        // safe tp predicate
        setTeleportSafe(stack, getTeleportPos(entity, target) != null);

        // handle glowing time

        int glowingTime = getGlowingTime(stack);

        if (glowingTime <= 0) {
            return;
        }

        int stackTime = getStackTime(stack);

        // play sound when limit exceeded
        if (glowingTime == 1) {
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS);
        }

        int gameTime = (int) level.getGameTime();

        if (stackTime == 0) {
            stackTime = gameTime;
            glowingTime -= 1;
        } else {
            int deltaTime = gameTime - stackTime;

            if (deltaTime > 0) {
                stackTime += deltaTime;
                glowingTime -= deltaTime;
            }
        }

        setStackTime(stack, stackTime);
        setGlowingTime(stack, glowingTime);
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

        // if no safe pos found, reset safe_tp predicate
        if (teleportPos == null) {
            setTeleportSafe(stack, false);

            return;
        }

        Vec3 teleportMovement = getMovementOnTeleport(teleportPos, target.blockPosition()).scale(0.12D);

        teleportToTarget(player, target, teleportPos, teleportMovement);
        setAbilityCooldown(stack, ABILITY_ID, ItemUtils.getCooldownStat(stack, ABILITY_ID));

        spreadRelicExperience(player, stack, 1);
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

        if (level.isClientSide || stack.isEmpty() || target.isDeadOrDying() || target.equals(player)) {
            return;
        }

        setStackTime(stack, (int) level.getGameTime());
        setGlowingTime(stack, getGlowingTimeStat(stack)); // set new glowing time on each attack
        setTargetUUID(stack, target.getUUID().toString());
        setPlayerDied(stack, false);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());

        if (player.getCommandSenderWorld().isClientSide || stack.isEmpty()) {
            return;
        }

        setPlayerDied(stack, true);
    }
}
