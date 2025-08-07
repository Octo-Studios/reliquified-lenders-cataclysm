package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ItemUtils {
    public static void resetMovementAttribute(LivingEntity entity, ItemStack stack, float value) {
        EntityUtils.resetAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                value, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void removeMovementAttribute(LivingEntity entity, ItemStack stack) {
        EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                AttributeModifier.Operation.ADD_VALUE);
    }

    public static void playCooldownSound(Level level, LivingEntity entity) {
        level.playSound(null, entity.blockPosition(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.0F, 1F);
    }

    public static List<LivingEntity> getEntitiesInArea(LivingEntity caster, Level level, AABB area) {
        return level.getEntities(null, area).stream()
                .map(entity -> entity instanceof LivingEntity livingEntity
                        && !entity.equals(caster) && !(EntityUtils.isAlliedTo(entity, caster))
                        && !(entity instanceof ArmorStand) ? livingEntity : null)
                .filter(Objects::nonNull).toList();
    }

    public static List<LivingEntity> getEntitiesInArea(LivingEntity caster, Level level, double radius) {
        return getEntitiesInArea(caster, level, getSphereArea(caster, radius));
    }

    public static AABB getSphereArea(LivingEntity entity, double radius) {
        return new AABB(entity.blockPosition()).inflate(radius);
    }

    @Nullable
    public static Vec3 getValidSpawnPos(Level level, Vec3 initialPos) {
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                double initialY = initialPos.y;

                // get the nearest blocks at the same height in opposite to target's view direction
                BlockPos pos = BlockPos.containing(
                        initialPos.x + i, initialY, initialPos.z + i);

                BlockPos newPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);

                // check for safety & possible height difference
                if (isBlockSafe(level, new Vec3(newPos.getX(), newPos.getY(), newPos.getZ()))
                        && Math.abs(initialY - newPos.getY()) <= 3.0D) {
                    return new Vec3(newPos.getX(), newPos.getY(), newPos.getZ());
                }
            }
        }

        return null;
    }

    public static boolean isBlockSafe(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);

        BlockState blockState = level.getBlockState(blockPos);
        BlockState blockStateBelow = level.getBlockState(blockPos.below());
        BlockState blockStateAbove = level.getBlockState(blockPos.above());

        return blockStateAbove.isAir() // player's head must be in air
                && (blockState.isAir() || !blockState.isCollisionShapeFullBlock(level, blockPos))
                && blockStateBelow.isSolid()
                && !(blockStateBelow.getBlock() instanceof LiquidBlock);
    }

    // stats

    public static int getIntStat(LivingEntity entity, ItemStack stack, String ability, String stat) {
        return (int) Math.round(((RelicItem) stack.getItem()).getStatValue(entity, stack, ability, stat));
    }

    private static int getTickStat(RelicItem relic, LivingEntity entity, ItemStack stack, String ability, String stat) {
        return (int) Math.floor(relic.getStatValue(entity, stack, ability, stat) * 20);
    }

    public static int getTickStat(LivingEntity entity, ItemStack stack, String ability, String stat) {
        return getTickStat((RelicItem) stack.getItem(), entity, stack, ability, stat);
    }

    public static int getCooldownStat(LivingEntity entity, ItemStack stack, String ability) {
        return getTickStat((RelicItem) stack.getItem(), entity, stack, ability, "cooldown");
    }

    public static float getSpeedStat(LivingEntity entity, ItemStack stack, String ability) {
        return (float) (((RelicItem) stack.getItem()).getStatValue(entity, stack, ability, "speed") / 20);
    }
}
