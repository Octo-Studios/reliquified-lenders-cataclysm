package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ScouringEyeUtils {
    public static final String ABILITY_ID = "glowing_scour";

    public static void teleportToTarget(Player player, LivingEntity target, BlockPos pos) {
        player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        player.getCommandSenderWorld().playSound(null, pos,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    public static BlockPos getTeleportPos(Player player, LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        Direction targetDirection = target.getNearestViewDirection().getOpposite();

        Level level = player.getCommandSenderWorld();
        BlockPos pos = BlockPos.containing(x + targetDirection.getStepX() * 2, y,
                z + targetDirection.getStepZ() * 2);

        // firstly check initial pos for safety
        if (!isBlockSafe(level, pos)) {
            // then find nearest safe pos
            // there may be null if no safe pos found
            return getSafePos(level, pos, targetDirection);
        }

        return pos;
    }

    @Nullable
    public static BlockPos getSafePos(Level level, BlockPos initialPos, Direction direction) {
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                double initialY = initialPos.getY();

                // get the nearest blocks at the same height in opposite to target's view direction
                BlockPos pos = BlockPos.containing(
                        initialPos.getX() + direction.getStepX() * i, initialY,
                        initialPos.getZ() + direction.getStepZ() * i);

                BlockPos newPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);

                // check for safety & possible height difference
                if (isBlockSafe(level, newPos) && Math.abs(initialY - newPos.getY()) <= 3.0D) {
                    return newPos;
                }
            }
        }

        return null;
    }

    public static boolean isBlockSafe(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        BlockState blockStateBelow = level.getBlockState(pos.below());

        return blockState.isAir() && blockStateBelow.isSolid() // BlockState.isSolid() - deprecated
                && !(blockStateBelow.getBlock() instanceof LiquidBlock);
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

    // getters & setters

    public static boolean isTeleportSafe(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.TP_SAFE, false);
    }

    public static void setTeleportSafe(ItemStack stack, boolean value) {
        stack.set(RECDataComponentRegistry.TP_SAFE, value);
    }

    public static String getTargetUUID(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.TARGET_UUID, "");
    }

    public static void setTargetUUID(ItemStack stack, String value) {
        stack.set(RECDataComponentRegistry.TARGET_UUID, value);
    }

    public static int getGlowingLimit(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.GLOWING_TIME, 0);
    }

    public static int getGlowingLimitStat(ItemStack stack) {
        return ItemUtils.getTickStat(stack, ABILITY_ID, "glowing_limit");
    }

    public static void setGlowingLimit(ItemStack stack, int value) {
        stack.set(RECDataComponentRegistry.GLOWING_TIME, value);
    }
}
