package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.PredicateType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
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
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .castData(CastData.builder()
                                        .type(CastType.INSTANTANEOUS)
                                        .predicate("target", PredicateType.CAST,
                                                (player, stack) -> !getTargetUUID(stack).isEmpty())
                                        .predicate("tp_allowed", PredicateType.CAST,
                                                ScouringEyeUtils::isTeleportAllowed)
                                        .build())
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(20D, 15D)
                                        .upgradeModifier(ScalingModelRegistry.ADDITIVE.get(), -1D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("glowing_time")
                                        .initialValue(10D, 12D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.525D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, LootEntries.THE_END)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFA171D1)
                                .borderBottom(0xFF8441AB)
                                .textured(true)
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
            resetData(entity, stack);

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
    public void castActiveAbility(Player player, ItemStack stack, String ability, CastType type, CastStage stage) {
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
        setAbilityCooldown(player, stack, ABILITY_ID, ItemUtils.getCooldownStat(player, stack, ABILITY_ID));

        spreadRelicExperience(player, stack, 1);
    }

    /**
     * Ability {@code glowing_scour} <b>[active]</b>:
     * write data of the attacked entity to the relic, then start glowing the entity on client for a while
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent.Post event) {
        LivingEntity entity = null;

        if (event.getSource().getEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity ownerEntity) {
            entity = ownerEntity;
        } else if (event.getSource().getEntity() instanceof LivingEntity sourceEntity) {
            entity = sourceEntity;
        }

        if (entity == null) {
            return;
        }

        Level level = entity.getCommandSenderWorld();
        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.SCOURING_EYE.get());
        LivingEntity target = event.getEntity();

        if (level.isClientSide || stack.isEmpty() || target.isDeadOrDying() || target.equals(entity)) {
            return;
        }

        setStackTime(stack, (int) level.getGameTime());
        setGlowingTime(stack, getGlowingTimeStat(entity, stack)); // set new glowing time on each attack
        setTargetUUID(stack, target.getUUID().toString());
        setPlayerDied(stack, false);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.SCOURING_EYE.get());

        if (entity.getCommandSenderWorld().isClientSide || stack.isEmpty()) {
            return;
        }

        setPlayerDied(stack, true);
    }
}
