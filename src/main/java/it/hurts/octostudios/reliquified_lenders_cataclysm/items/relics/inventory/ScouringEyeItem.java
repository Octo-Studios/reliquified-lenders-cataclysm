package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory;

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
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
import org.jetbrains.annotations.NotNull;

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
                                .stat(StatTemplate.builder("glowing_time")
                                        .initialValue(10D, 12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.525D)
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof LivingEntity livingEntity) || level.isClientSide) {
            return;
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            return;
        }

        if (target.isDeadOrDying()) {
            resetData(livingEntity, stack);

            return;
        }

        // safe tp predicate
        setTeleportSafe(stack, getTeleportPos(livingEntity, target) != null);

        // handle glowing time

        int glowingTime = getGlowingTime(stack);

        if (glowingTime <= 0) {
            return;
        }

        int stackTime = getStackTime(stack);

        // play sound when limit exceeded
        if (glowingTime == 1) {
            level.playSound(null, livingEntity.blockPosition(),
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
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !canPlayerUseAbility(player, stack, ABILITY_ID)) {
            return InteractionResultHolder.pass(stack);
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos teleportPos = getTeleportPos(player, target);

        // if no safe pos found, reset safe_tp predicate
        if (teleportPos == null) {
            setTeleportSafe(stack, false);

            return InteractionResultHolder.pass(stack);
        }

        Vec3 teleportMovement = getMovementOnTeleport(teleportPos, target.blockPosition()).scale(0.12D);

        teleportToTarget(player, target, teleportPos, teleportMovement);
        setAbilityCooldown(player, stack, ABILITY_ID, ItemUtils.getCooldownStat(player, stack, ABILITY_ID));

        spreadRelicExperience(player, stack, 1);

        return InteractionResultHolder.success(stack);
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
        LivingEntity target = event.getEntity();

        if (level.isClientSide || target.isDeadOrDying() || target.equals(player)) {
            return;
        }

        ItemStack stack = ScouringEyeUtils.getFirstFromInventory(player);

        if (stack.isEmpty()) {
            return;
        }

        setStackTime(stack, (int) level.getGameTime());
        setGlowingTime(stack, getGlowingTimeStat(player, stack)); // set new glowing time on each attack
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
