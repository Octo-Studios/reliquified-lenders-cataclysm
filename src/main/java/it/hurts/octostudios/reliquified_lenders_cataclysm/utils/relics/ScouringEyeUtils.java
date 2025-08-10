package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import it.hurts.sskirillss.relics.utils.ServerScheduler;
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

import java.awt.*;
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

    public static void pierceTargets(LivingEntity targetFinal, Player player, Level level, float damage) {
        Vec3 fromPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 toPos = targetFinal.position().add(0, targetFinal.getBbHeight() / 2D, 0);

        double radius = targetFinal.getBbWidth() / 2D;

        List<LivingEntity> targetsPotential = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(fromPos, toPos).inflate(radius),
                entity -> !entity.equals(player) && entity.isAlive()
                        && !EntityUtils.isAlliedTo(player, entity));
        List<LivingEntity> targetsToHurt = new ArrayList<>();

        Vec3 direction = toPos.subtract(fromPos);
        Vec3 normalized = direction.normalize();

        for (LivingEntity target : targetsPotential) {
            AABB box = target.getBoundingBox();
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
                targetsToHurt.add(target);
            }
        }

        targetsToHurt = targetsToHurt.stream().filter(entity ->
                        !entity.equals(player) && entity.isAlive()
                                && !EntityUtils.isAlliedTo(player, entity))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!targetsToHurt.contains(targetFinal)) {
            targetsToHurt.add(targetFinal);
        }

        drawRay(level, fromPos, toPos, targetFinal.getBbWidth());

        for (var target : targetsToHurt) {
            target.hurt(level.damageSources().magic(), damage);

            drawEntityBox(level, targetsToHurt, target);
        }
    }

    private static void drawRay(Level level, Vec3 fromPos, Vec3 toPos, double width) {
        double distanceTotal = fromPos.distanceTo(toPos);

        Vec3 direction = toPos.subtract(fromPos).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        if (Math.abs(direction.dot(up)) > 0.99D) {
            up = new Vec3(1, 0, 0);
        }

        Color startColor = new Color(139, 0, 105);
        Color endColor = new Color(47, 0, 97);

        int particleLifetime = (int) (distanceTotal * 5);
        int particlesNum = 64;

        for (int i = 0; i <= particlesNum; i++) {
            double progress = (double) i / (particlesNum - 1);

            // drawing the spiral

            double angle = 2 * Math.PI * 1.2D * distanceTotal * progress;
            double spiralRadius = Math.sin(Math.PI * progress) * width;

            Vec3 pDirection = toPos.subtract(fromPos).normalize();
            Vec3 pos = fromPos.add(pDirection.scale(i * distanceTotal / particlesNum));

            Vec3 rightVec = pDirection.cross(new Vec3(0, 1, 0)).normalize();

            if (rightVec.length() == 0) {
                rightVec = new Vec3(1, 0, 0);
            }

            Vec3 upVec = pDirection.cross(rightVec).normalize();

            pos = pos.add(rightVec.scale(spiralRadius * Math.cos(angle)))
                    .add(upVec.scale(spiralRadius * Math.sin(angle)));

            // coloring the spiral

            Color color;

            if (i % 8 == 0) {
                color = getSpecialColor();
            } else if (progress <= 0D) {
                color = startColor;
            } else if (progress >= 1D) {
                color = endColor;
            } else {
                int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * progress);
                int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * progress);
                int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * progress);

                color = new Color(r, g, b);
            }

            // sending

            ((ServerLevel) level).sendParticles(
                    ParticleUtils.constructSimpleSpark(color,
                            0.9F, particleLifetime, 0.85F),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0);
        }
    }

    private static void drawEntityBox(Level level, List<LivingEntity> targets, LivingEntity target) {
        LivingEntity targetFinal = targets.getLast();

        Vec3 targetPos = target.position();
        Vec3 finalPos = targetFinal.position();

        int pMin = 12, pMax = 32;
        int particlesNum = pMin + target.getRandom().nextInt(pMax - pMin + 1);

        int ticksTotal = 16;

        for (int i = 0; i < particlesNum; i++) {
            double px = (target.getRandom().nextDouble()) * target.getBbWidth();
            double py = (target.getRandom().nextDouble()) * target.getBbHeight();
            double pz = (target.getRandom().nextDouble()) * target.getBbWidth();

            Vec3 pos = targetPos.add(px, py, pz);
            Vec3 toPos;

            if (target.equals(targetFinal)) {
                Entity targetPrev = targets.size() > 1 ? targets.get(targets.size() - 2) : targetFinal;

                toPos = pos.add(finalPos.subtract(targetPrev.position()).normalize().scale(1.0D));
            } else {
                toPos = pos.add(finalPos.subtract(targetPos).normalize().scale(1.0D));
            }

            for (int ticks = 1; ticks <= ticksTotal; ticks++) {
                double ticksProgress = (double) ticks / ticksTotal;
                Vec3 spawnPos = pos.add(toPos.subtract(pos).scale(ticksProgress));
//                Vec3 motion = toPos.subtract(spawnPos).scale(1.0D / (ticksTotal + 5));

                ServerScheduler.schedule(ticks, () -> ((ServerLevel) level).sendParticles(
                        ParticleUtils.constructSimpleSpark(getSpecialColor(),
                                0.25F, ticksTotal + 20, 0.25F),
                        spawnPos.x, spawnPos.y, spawnPos.z,
                        1, 0, 0, 0, 0));
            }
        }
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

    private static Color getSpecialColor() {
        return new Color(255, 58, 204);
    }
}
