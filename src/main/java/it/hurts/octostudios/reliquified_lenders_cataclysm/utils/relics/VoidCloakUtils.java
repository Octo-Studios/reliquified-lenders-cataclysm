package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.ScreenShakeSoundedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VoidCloakItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoidCloakUtils {
    @Getter
    private static final int waveTicks = 30;

    public static void spawnVoidRune(Level level, Player player, LivingEntity entity, ItemStack stack) {
        Vec3 mobMovement = entity.getDeltaMovement();

        if (EntityUtils.isAlliedTo(player, entity)) {
            return;
        }

        spawnFang(level, player, entity, entity.getX() + mobMovement.x, entity.getZ() + mobMovement.z,
                0, -20, getRuneDamageStat(stack));
    }

    public static void spawnSeismicZone(Level level, Player player, LivingEntity dyingEntity, ItemStack stack) {
        int wavesNum = ItemUtils.getIntStat(stack, "seismic_zone", "waves");
        int layersSpawned = 0;

        for (int i = 0; i < wavesNum; i++) {
            layersSpawned = spawnQuakeWave(player, dyingEntity, stack, i);
        }

        if (layersSpawned > 0) {
            level.addFreshEntity(new ScreenShakeSoundedEntity(level, dyingEntity.position(), getRadiusStat(stack),
                    layersSpawned, wavesNum, 40));
        }
    }

    private static int spawnQuakeWave(Player player, LivingEntity dyingEntity, ItemStack stack, int waveIndex) {
        Level level = player.getCommandSenderWorld();
        Vec3 pos = dyingEntity.position();

        float shift = 1.25F; // fangs shift along circle
        float shiftMultiplier = 2.5F; // distance from center
        int delayTicks = 3;
        int layersSpawned = 0;

        for (int r = 0; r < getRadiusStat(stack); r++) {
            int fangsNum = 6;
            boolean fangSpawned = false;

            for (int i = 0; i < fangsNum; i++) {
                float angle = (float) (i * Math.PI * 2.0F / fangsNum + shift);

                if (spawnFang(level, player, dyingEntity,
                        pos.x + (double) Mth.cos(angle) * shiftMultiplier,
                        pos.z + (double) Mth.sin(angle) * shiftMultiplier,
                        i, waveIndex * getWaveTicks() + delayTicks, getZoneDamageStat(stack))) {
                    fangSpawned = true;
                }
            }

            shift -= shiftMultiplier;
            shiftMultiplier++;

            delayTicks += 2;

            if (fangSpawned) {
                layersSpawned++;
            }
        }

        return layersSpawned;
    }

    public static boolean spawnFang(Level level, LivingEntity caster, LivingEntity entity,
                                  double posX, double posZ, float yRot, int delayTicks, float damage, boolean silent) {
        BlockPos pos = BlockPos.containing(posX, entity.getY() + 1.0D, posZ);
        double shiftY = 0.0D;
        boolean canFangSpawn = false;

        do {
            BlockPos posBelow = pos.below();
            BlockState blockStateBelow = level.getBlockState(posBelow);

            if (blockStateBelow.isFaceSturdy(level, posBelow, Direction.UP)) {
                if (!level.isEmptyBlock(pos)) {
                    BlockState blockState = level.getBlockState(pos);
                    VoxelShape collisionShape = blockState.getCollisionShape(level, pos);

                    if (!collisionShape.isEmpty()) {
                        shiftY = collisionShape.max(Direction.Axis.Y);
                    }
                }

                canFangSpawn = true;

                break;
            }

            pos = pos.below();
        } while (pos.getY() >= Math.floor(entity.getY() - 1));

        // spawn rune without sound (the sound is playing with modified screen shake entity)
        if (canFangSpawn) {
            Void_Rune_Entity voidRuneEntity = new Void_Rune_Entity(level,
                    posX, pos.getY() + shiftY, posZ, yRot, delayTicks, damage, caster);
            voidRuneEntity.setSilent(silent);
            voidRuneEntity.setCaster(caster);

            level.addFreshEntity(voidRuneEntity);
        }

        return canFangSpawn;
    }

    public static boolean spawnFang(Level level, LivingEntity caster, LivingEntity entity,
                                    double posX, double posZ, float yRot, int delayTicks, float damage) {
        return spawnFang(level, caster, entity, posX, posZ, yRot, delayTicks, damage, false);
    }

    // simple getters

    private static float getRuneDamageStat(ItemStack stack) {
        return (float) ((VoidCloakItem) stack.getItem()).getStatValue(stack, "void_rune", "damage");
    }

    private static float getZoneDamageStat(ItemStack stack) {
        return (float) ((VoidCloakItem) stack.getItem()).getStatValue(stack, "seismic_zone", "damage");
    }

    public static int getRadiusStat(ItemStack stack) {
        return ItemUtils.getIntStat(stack, "seismic_zone", "radius");
    }
}
