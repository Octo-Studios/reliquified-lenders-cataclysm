package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks.IRankModifier;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import it.hurts.sskirillss.relics.utils.ServerScheduler;
import lombok.Getter;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

            drawEntityBox((ServerLevel) level, player, targetsToHurt, target);
        }
    }

    private static void drawRay(Level level, Vec3 fromPos, Vec3 toPos, double width) {
        double distanceTotal = fromPos.distanceTo(toPos);

        Color startColor = getStartColor();
        Color endColor = getEndCclor();

        int particleLifetime = (int) (distanceTotal * 5);
        int particlesNum = (int) (distanceTotal * 10);

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

    private static void drawEntityBox(ServerLevel level, LivingEntity source, List<LivingEntity> targets, LivingEntity target) {
        LivingEntity targetFinal = targets.getLast();

        Vec3 targetPos = target.position();

        RandomSource random = target.getRandom();

        int particlesNum = MathUtils.randomBetween(target.getRandom(), 16, 32);
        int particleLifetime = 10;

        for (int i = 0; i < particlesNum; i++) {
            double px = (random.nextDouble() - 0.5D) * target.getBbWidth();
            double py = random.nextDouble() * target.getBbHeight();
            double pz = (random.nextDouble() - 0.5D) * target.getBbWidth();

            Vec3 spawnPos = targetPos.add(px, py, pz);

            Vec3 toPos = spawnPos.add(targetFinal.position().subtract(source.position()).normalize().scale(1D));

            for (int ticks = 1; ticks <= particleLifetime; ticks++) {
                double progress = (double) ticks / particleLifetime;

                Vec3 pos = spawnPos.add(toPos.subtract(spawnPos).scale(progress));
                Vec3 motion = toPos.subtract(pos).scale(2D / Math.max(1, (particleLifetime - ticks + 1)));

                ServerScheduler.schedule(ticks, () -> level.sendParticles(
                        ParticleUtils.constructSimpleSpark(getBoxColor(pos, target.getBoundingBox()),
                                0.25F, particleLifetime, 0.45F),
                        pos.x, pos.y, pos.z,
                        0, motion.x, motion.y, motion.z, 1D));
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

        if (RECItemUtils.isBlockSafe(level, pos)) {
            return pos;
        }

        return RECItemUtils.getValidSpawnPos(level, pos); // null if no safe pos found
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
    public static boolean isRankModifierUnlocked(LivingEntity entity, ItemStack stack, int rank) {
        if (!(stack.getItem() instanceof ScouringEyeItem relic)) {
            return false;
        }

        return relic.isAbilityRankModifierUnlocked(entity, stack, ABILITY_ID, RankModifier.getModifierByRank(rank));
    }

    public static boolean isGlowingTimeInBounds(LivingEntity entity, ItemStack stack) {
        return getGlowingTime(stack) > 0 && getGlowingTime(stack) <= getGlowingTimeStat(entity, stack);
    }

    public static boolean isTeleportAllowed(LivingEntity entity, ItemStack stack, Level level) {
        return stack.getOrDefault(RECDataComponents.TP_SAFE, false)
                && isGlowingTimeInBounds(entity, stack) && getStackTime(stack) >= level.getGameTime() - 1
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
        return RECItemUtils.getTickStat(entity, stack, ABILITY_ID, "glowing_time");
    }

    public static int getParalysisStatTicks(LivingEntity entity, ItemStack stack) {
        return RECItemUtils.getTickStat(entity, stack, ABILITY_ID, "paralysis_time");
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

    private static Color getBoxColor(Vec3 pos, AABB box) {
        double distance = pos.distanceTo(box.getBottomCenter());

        if (distance <= 1D / distance) {
            return getEndCclor();
        } else if (distance >= 2D / distance) {
            return getSpecialColor();
        } else {
            return getStartColor();
        }
    }

    private static Color getStartColor() {
        return new Color(139, 0, 105);
    }

    private static Color getEndCclor() {
        return new Color(47, 0, 97);
    }

    private static Color getSpecialColor() {
        return new Color(255, 58, 204);
    }

    @Getter
    public enum RankModifier implements IRankModifier {
        A(new RankModifierData("glowing", 1)),
        B(new RankModifierData("paralysis", 3)),
        C(new RankModifierData("glowing_attack", 5));

        private final RankModifierData data;

        RankModifier(RankModifierData data) {
            this.data = data;
        }

        public static String getModifierByRank(int rank) {
            return IRankModifier.getModifierByRank(RankModifier.class, rank);
        }
    }
}
