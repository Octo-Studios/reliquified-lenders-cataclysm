package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.scouring_eye.ScouringRayEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.http.annotation.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScouringEyeUtils {
    public static final String ABILITY_ID = "glowing_scour";

    public static void resetData(LivingEntity entity, ItemStack stack) {
        setGlowingTime(stack, getGlowingTimeStat(entity, stack));
        setTargetUUID(stack, "");
        setTeleportSafe(stack, false);
    }

    public static void pierceTargets(LivingEntity target, Player player, Level level, float damage) {
        Vec3 fromPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 toPos = target.position().add(0, target.getBbHeight() / 2D, 0);

        double radius = target.getBbWidth() / 2D;

        List<LivingEntity> targetsPotential = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(fromPos, toPos).inflate(radius),
                entity -> !entity.equals(player) && entity.isAlive()
                        && !EntityUtils.isAlliedTo(player, entity));
        List<LivingEntity> targetsToHurt = new ArrayList<>();

        Vec3 direction = toPos.subtract(fromPos);
        Vec3 normalized = direction.normalize();

        for (LivingEntity targetOther : targetsPotential) {
            AABB box = targetOther.getBoundingBox();
            Vec3 centerPos = box.getCenter();
            double proj = centerPos.subtract(fromPos).dot(normalized);

            if (proj < 0 || proj > direction.length()) {
                continue;
            }

            Vec3 nearestPos = fromPos.add(normalized.scale(proj));

            double dx = Math.max(0, Math.abs(centerPos.x - nearestPos.x) - box.getXsize() / 2D);
            double dy = Math.max(0, Math.abs(centerPos.y - nearestPos.y) - box.getYsize() / 2D);
            double dz = Math.max(0, Math.abs(centerPos.z - nearestPos.z) - box.getZsize() / 2D);

            if (Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2) <= Math.pow(radius, 2)) {
                targetsToHurt.add(targetOther);
            }
        }

        targetsToHurt = targetsToHurt.stream().filter(entity ->
                        !entity.equals(player) && entity.isAlive()
                                && !EntityUtils.isAlliedTo(player, entity))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!targetsToHurt.contains(target)) {
            targetsToHurt.add(target);
        }

        ScouringRayEntity rayEntity = new ScouringRayEntity(level, targetsToHurt, fromPos, toPos, target.getBbWidth(), damage);
        rayEntity.setPos(fromPos);

        level.addFreshEntity(rayEntity);
    }

    public static void teleportToTarget(Player player, LivingEntity target, Vec3 pos, Vec3 motion) {
        player.teleportTo(pos.x, pos.y + 1.0D, pos.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition().add(0D, 1.0D, 0D));

        // set player movement
        NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()), (ServerPlayer) player);

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30), player);

        player.getCommandSenderWorld().playSound(null, BlockPos.containing(pos),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    public static Vec3 getTeleportPos(LivingEntity entity, LivingEntity target) {
        Level level = entity.getCommandSenderWorld();
        Vec3 pos = target.position().subtract(target.getViewVector(0F).scale(3.0F));

        if (ItemUtils.isBlockSafe(level, pos)) {
            return pos;
        }

        return ItemUtils.getValidSpawnPos(level, pos); // null if no safe pos found
    }

    @Nullable
    public static LivingEntity getEntityFromStack(Level level, ItemStack stack) {
        String uuid = getTargetUUID(stack);

        if (uuid.isEmpty() || level.isClientSide) {
            return null;
        }

        Entity entity = ((ServerLevel) level).getEntity(UUID.fromString(uuid));

        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }

    // simple getters & setters

    @Experimental
    public static boolean isRankModifierUnlocked(LivingEntity entity, ItemStack stack, String modifier) {
        if (!(stack.getItem() instanceof ScouringEyeItem relic)) {
            return false;
        }

        return relic.isAbilityRankModifierUnlocked(entity, stack, ABILITY_ID, modifier);
    }

    public static boolean isGlowingTimeInBounds(LivingEntity entity, ItemStack stack) {
        return getGlowingTime(stack) > 0 && getGlowingTime(stack) <= getGlowingTimeStat(entity, stack);
    }

    public static boolean isGlowingTimeTicking(LivingEntity entity, ItemStack stack, Level level) {
        return isGlowingTimeInBounds(entity, stack) && getStackTime(stack) >= level.getGameTime() - 1;
    }

    public static boolean isTeleportAllowed(LivingEntity entity, ItemStack stack, Level level) {
        return stack.getOrDefault(RECDataComponents.TP_SAFE, false)
                && isGlowingTimeTicking(entity, stack, level)
                && !stack.getOrDefault(RECDataComponents.PLAYER_DIED, false);
    }

    public static void setTeleportSafe(ItemStack stack, boolean value) {
        stack.set(RECDataComponents.TP_SAFE, value);
    }

    public static float getLastDamage(ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.LAST_DAMAGE, 0F);
    }

    public static void setLastDamage(ItemStack stack, float value) {
        stack.set(RECDataComponents.LAST_DAMAGE, value);
    }

    public static void setPlayerDied(ItemStack stack, boolean value) {
        stack.set(RECDataComponents.PLAYER_DIED, value);
    }

    public static String getTargetUUID(ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.TARGET_UUID, "");
    }

    public static void setTargetUUID(ItemStack stack, String value) {
        stack.set(RECDataComponents.TARGET_UUID, value);
    }

    public static int getStackTime(ItemStack stack) {
        return stack.getOrDefault(RelicsDataComponents.TIME, 0);
    }

    public static void setStackTime(ItemStack stack, int value) {
        stack.set(RelicsDataComponents.TIME, value);
    }

    public static int getGlowingTime(ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.GLOWING_TIME, 0);
    }

    public static void setGlowingTime(ItemStack stack, int value) {
        stack.set(RECDataComponents.GLOWING_TIME, value);
    }

    public static int getGlowingTimeStat(LivingEntity entity, ItemStack stack) {
        return ItemUtils.getTickStat(entity, stack, ABILITY_ID, "glowing_time");
    }

    public static int getParalysisStatTicks(LivingEntity entity, ItemStack stack) {
        return ItemUtils.getTickStat(entity, stack, ABILITY_ID, "paralysis_time");
    }

    public static double getDamagePercent(LivingEntity entity, ItemStack stack) {
        return ((RelicItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "damage_percent");
    }

    public static List<ItemStack> getAllInventoryStacks(Player player) {
        if (player == null) {
            return List.of();
        }

        List<ItemStack> inventoryRelics = player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof ScouringEyeItem).toList();

        if (inventoryRelics.isEmpty()) {
            return List.of();
        }

        return inventoryRelics;
    }
}
