package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.inventory.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.packets.S2CSetEntityMotion;
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
import net.minecraft.world.phys.Vec3;
import org.apache.http.annotation.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ScouringEyeUtils {
    public static final String ABILITY_ID = "glowing_scour";

    public static void resetData(LivingEntity entity, ItemStack stack) {
        setGlowingTime(stack, getGlowingTimeStat(entity, stack));
        setTargetUUID(stack, "");
        setTeleportSafe(stack, false);
    }

//    public static void paralyzeEntity(LivingEntity entity, int ticks) {
//        Vec3 motion = entity.getDeltaMovement();
//
//        entity.setDeltaMovement(Vec3.ZERO);
//
////        entity.getPersistentData().putInt("Paralysis", ticks);
//
//        entity.getCommandSenderWorld().addParticle(
//                ParticleUtils.constructSimpleSpark(new Color(111, 24, 157),
//                        0.4F, 20, 0.8F),
//                entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
//                0, 0, 0
//        );
//    }

    public static void teleportToTarget(Player player, LivingEntity target, BlockPos pos, Vec3 motion) {
        Vec3 posCenter = pos.getBottomCenter();
        player.teleportTo(posCenter.x, pos.getY() + 1.0D, posCenter.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition().add(0D, 0.5D, 0D));

        // set player movement
        NetworkHandler.sendToClient(new S2CSetEntityMotion(player.getId(), motion.toVector3f()), (ServerPlayer) player);

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30), player);

        player.getCommandSenderWorld().playSound(null, pos,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    public static BlockPos getTeleportPos(LivingEntity entity, LivingEntity target) {
        Level level = entity.getCommandSenderWorld();

        Vec3 pos = target.position().add(target.getViewVector(1.0F).scale(-3.0F));
        BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);

        // firstly check initial pos for safety
        if (!ItemUtils.isBlockSafe(level, blockPos)) {
            // then find nearest safe pos (there may be null if no safe pos found)
            return ItemUtils.getValidSpawnPos(level, blockPos);
        }

        return blockPos;
    }

    public static Vec3 getMovementOnTeleport(BlockPos teleportPos, BlockPos targetPos) {
        return new Vec3(targetPos.getX(), 0.0D, targetPos.getZ())
                .subtract(teleportPos.getX(), 0.0D, teleportPos.getZ());
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

    public static boolean isTeleportAllowed(LivingEntity entity, ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.TP_SAFE, false)
                && isGlowingTimeInBounds(entity, stack)
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

    public static float getDamagePercent(LivingEntity entity, ItemStack stack) {
        return ItemUtils.getIntStat(entity, stack, ABILITY_ID, "damage");
    }

    public static ItemStack getFirstFromInventory(Player player) {
        ItemStack emptyStack = ItemStack.EMPTY;

        if (player == null) {
            return emptyStack;
        }

        return player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof ScouringEyeItem)
                .findFirst().orElse(emptyStack);
    }
}
