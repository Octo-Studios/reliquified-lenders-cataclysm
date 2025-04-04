package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VoidCloakItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoidCloakUtils {
    public static void spawnVoidRune(Level level, Player player, PathfinderMob mob,
                                     ItemStack stack) {
        Vec3 mobMovement = mob.getDeltaMovement();

        spawnFang(level, player, mob, mob.getX() + mobMovement.x, mob.getZ() + mobMovement.z,
                0, -20, getDamageStat(stack));
    }

    public static void spawnSeismicZone(ItemStack stack, Player player, LivingEntity dyingEntity, int quakeIndex) {
        Level level = player.getCommandSenderWorld();
        Vec3 pos = dyingEntity.position();

        float shift = 1.25F; // fangs shift along circle
        float shiftMultiplier = 2.5F; // distance from center
        int delayTicks = 3;

        for (int r = 0; r < getRadiusStat(stack); r++) {
            int fangsNum = 6;

            for (int i = 0; i < fangsNum; i++) {
                float angle = (float) (i * Math.PI * 2.0F / fangsNum + shift);

                spawnFang(level, player, dyingEntity,
                        pos.x + (double) Mth.cos(angle) * shiftMultiplier,
                        pos.z + (double) Mth.sin(angle) * shiftMultiplier,
                        i, quakeIndex * 50 + delayTicks, getDamageStat(stack));
            }

            shift -= shiftMultiplier;
            shiftMultiplier++;

            delayTicks += 2;
        }
    }

    public static void spawnFang(Level level, Player player, LivingEntity entity,
                                  double posX, double posZ, float yRot, int delayTicks, float damage) {
        BlockPos blockPos = BlockPos.containing(posX, entity.getY() + 1.0D, posZ);
        double shiftY = 0.0D;

        do {
            BlockPos belowBlockPos = blockPos.below();
            BlockState belowBlockState = level.getBlockState(belowBlockPos);

            if (belowBlockState.isFaceSturdy(level, belowBlockPos, Direction.UP)
                    && !level.isEmptyBlock(blockPos)) {
                BlockState blockState = level.getBlockState(blockPos);
                VoxelShape collisionShape = blockState.getCollisionShape(level, blockPos);

                if (!collisionShape.isEmpty()) {
                    shiftY = collisionShape.max(Direction.Axis.Y);
                }

                break;
            }

            blockPos = blockPos.below();
        } while (blockPos.getY() >= Math.floor(entity.getY() - 1));

        Void_Rune_Entity voidRuneEntity = new Void_Rune_Entity(level,
                posX, blockPos.getY() + shiftY, posZ, yRot, delayTicks, damage, player);

        voidRuneEntity.setSilent(true);
        level.addFreshEntity(voidRuneEntity);

//        level.playSound(null, voidRuneEntity.blockPosition(),
//                ModSounds.VOID_RUNE_RISING.get(), SoundSource.MASTER,
//                1.0F, 1.0F);
    }

    // simple getters

    private static float getDamageStat(ItemStack stack) {
        return (float) ((VoidCloakItem) stack.getItem()).getStatValue(stack, "void_rune", "damage");
    }

    public static int getQuakesStat(ItemStack stack) {
        return (int) ((VoidCloakItem) stack.getItem()).getStatValue(stack, "seismic_zone", "quakes");
    }

    public static int getRadiusStat(ItemStack stack) {
        return (int) ((VoidCloakItem) stack.getItem()).getStatValue(stack, "seismic_zone", "radius");
    }
}
