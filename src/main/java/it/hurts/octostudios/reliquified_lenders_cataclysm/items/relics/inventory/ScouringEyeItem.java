package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils;
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
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
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
                                .rankModifier(1, "glowing")
                                .rankModifier(3, "paralysis")
                                .rankModifier(5, "glowing_attack")
                                .stat(StatTemplate.builder("glowing_time")
                                        .initialValue(10D, 12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.02D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("paralysis_time")
                                        .initialValue(0.8D, 1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.028D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage_percent")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.114D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .maxRank(5)
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

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !canPlayerUseAbility(player, stack, ABILITY_ID)
                || getTargetUUID(stack).isEmpty() || !isTeleportAllowed(player, stack)) {
            return InteractionResultHolder.pass(stack);
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }

        // rank 3
        if (isRankModifierUnlocked(target, stack, "paralysis")) {
            target.addEffect(new MobEffectInstance(RelicsMobEffects.PARALYSIS, getParalysisStatTicks(player, stack)), player);
        }

        // rank 5
        if (isRankModifierUnlocked(target, stack, "glowing_attack")) {
            float damage = (float) (getLastDamage(stack) * getDamagePercent(player, stack));

            hurtTargets(target, player, level, damage);
        }

        BlockPos teleportPos = getTeleportPos(player, target);

        // if no safe pos found, reset safe_tp predicate
        if (teleportPos == null) {
            setTeleportSafe(stack, false);

            return InteractionResultHolder.pass(stack);
        }

        Vec3 teleportMovement = getMovementOnTeleport(teleportPos, target.blockPosition()).scale(0.12D);
        teleportToTarget(player, target, teleportPos, teleportMovement);

        spreadRelicExperience(player, stack, 1);

        player.getCooldowns().addCooldown(this, 20);

        return InteractionResultHolder.success(stack);
    }

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
        setLastDamage(stack, event.getNewDamage());
        setPlayerDied(stack, false);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, RECItems.SCOURING_EYE.get());

        if (entity.getCommandSenderWorld().isClientSide || stack.isEmpty()) {
            return;
        }

        setPlayerDied(stack, true);
    }
}
