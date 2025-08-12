package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.ScreenShakeSoundedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle.VoidRuneModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VoidMantleItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks.IRankModifier;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.http.annotation.Experimental;

import java.util.Arrays;

public class VoidMantleUtils {
    public static final String ABILITY_ID = "void_rune";

    @Getter
    private static final int waveTicks = 30;

    public static void spawnVoidRune(Level level, LivingEntity caster, LivingEntity target, ItemStack stack) {
        Vec3 mobMovement = target.getDeltaMovement();

        if (EntityUtils.isAlliedTo(caster, target)) {
            return;
        }

        spawnFang(level, caster, target, target.getX() + mobMovement.x, target.getZ() + mobMovement.z,
                0, -20, getRuneDamageStat(caster, stack));
    }

    public static void spawnSeismicZone(Level level, LivingEntity entity, LivingEntity dyingEntity, ItemStack stack) {
        int wavesNum = ItemUtils.getIntStat(entity, stack, ABILITY_ID, "waves");
        int layersSpawned = 0;

        for (int i = 0; i < wavesNum; i++) {
            layersSpawned = spawnQuakeWave(entity, dyingEntity, stack, i);
        }

        if (layersSpawned > 0) {
            level.addFreshEntity(new ScreenShakeSoundedEntity(level, dyingEntity.position(), getRadiusStat(entity, stack),
                    layersSpawned, wavesNum, 40));
        }
    }

    private static int spawnQuakeWave(LivingEntity entity, LivingEntity dyingEntity, ItemStack stack, int waveIndex) {
        Level level = entity.getCommandSenderWorld();
        Vec3 pos = dyingEntity.position();

        float shift = 1.25F; // fangs shift along circle
        float shiftMultiplier = 2.5F; // distance from center
        int delayTicks = 3;
        int layersSpawned = 0;

        for (int r = 0; r < getRadiusStat(entity, stack); r++) {
            int fangsNum = 6;
            boolean fangSpawned = false;

            for (int i = 0; i < fangsNum; i++) {
                float angle = (float) (i * Math.PI * 2.0F / fangsNum + shift);

                if (spawnFang(level, entity, dyingEntity,
                        pos.x + (double) Mth.cos(angle) * shiftMultiplier,
                        pos.z + (double) Mth.sin(angle) * shiftMultiplier,
                        i, waveIndex * getWaveTicks() + delayTicks, getZoneDamageStat(entity, stack))) {
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

    public static boolean spawnFang(Level level, LivingEntity caster, LivingEntity target,
                                    double posX, double posZ, float yRot, int delayTicks, float damage, boolean silent) {
        BlockPos pos = BlockPos.containing(posX, target.getY() + 1.0D, posZ);
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
        } while (pos.getY() >= Math.floor(target.getY() - 1));

        // spawn rune without sound (the sound is playing with modified screen shake entity)
        if (canFangSpawn) {
            VoidRuneModifiedEntity runeEntity = new VoidRuneModifiedEntity(level,
                    posX, pos.getY() + shiftY, posZ, yRot, delayTicks, damage, caster, target);

            runeEntity.setSilent(silent);
            runeEntity.setCaster(caster);

            level.addFreshEntity(runeEntity);
        }

        return canFangSpawn;
    }

    public static boolean spawnFang(Level level, LivingEntity caster, LivingEntity entity,
                                    double posX, double posZ, float yRot, int delayTicks, float damage) {
        return spawnFang(level, caster, entity, posX, posZ, yRot, delayTicks, damage, false);
    }

    // simple getters & stuff

    @Experimental
    public static boolean isRankModifierUnlocked(LivingEntity entity, ItemStack stack, int rank) {
        if (!(stack.getItem() instanceof VoidMantleItem relic)) {
            return false;
        }

        return relic.isAbilityRankModifierUnlocked(entity, stack, ABILITY_ID, RankModifier.getModifierByRank(rank));
    }

    @Experimental
    public static boolean isVoidRuneDisabled(LivingEntity entity, ItemStack stack) {
        return ((VoidMantleItem) stack.getItem()).getAbilityMode(entity, stack, ABILITY_ID).equals("disabled");
    }

    public static int getStunStatTicks(LivingEntity entity, ItemStack stack) {
        return ItemUtils.getTickStat(entity, stack, ABILITY_ID, "stun_time");
    }

    private static float getRuneDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((VoidMantleItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "damage");
    }

    private static float getZoneDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((VoidMantleItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "zone_damage");
    }

    public static int getRadiusStat(LivingEntity entity, ItemStack stack) {
        return ItemUtils.getIntStat(entity, stack, ABILITY_ID, "zone_radius");
    }

    @Getter
    public enum RankModifier implements IRankModifier {
        A(new RankModifierData("stun", 1)),
        B(new RankModifierData("attraction", 3)),
        C(new RankModifierData("seismic_zone", 5));

        private final RankModifierData data;

        RankModifier(RankModifierData data) {
            this.data = data;
        }

        public static String getModifierByRank(int rank) {
            return IRankModifier.getModifierByRank(RankModifier.class, rank);
        }
    }
}
