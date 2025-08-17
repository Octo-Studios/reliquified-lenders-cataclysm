package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.ScreenShakeSoundedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_mantle.VoidRuneModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back.VoidMantleItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks.IRankModifier;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.http.annotation.Experimental;

import java.awt.*;

public class VoidMantleUtils {
    public static final String ABILITY_ID = "void_rune";

    private static final Color CIRCLE_START_COLOR = new Color(200, 0, 255);
    private static final Color CIRCLE_END_COLOR = new Color(102, 36, 180);

    @Getter
    private static final int waveTicks = 30;

    public static void spawnVoidRune(Level level, LivingEntity caster, LivingEntity target, ItemStack stack) {
        Vec3 movement = target.getDeltaMovement();

        spawnFang(level, caster, target, target.getX() + movement.x, target.getZ() + movement.z,
                0, -20, getRuneDamageStat(caster, stack), false);
    }

    public static void spawnSeismicZone(Level level, LivingEntity entity, LivingEntity dyingEntity, ItemStack stack) {
        int wavesNum = RECItemUtils.getIntStat(entity, stack, ABILITY_ID, "waves");
        int layersSpawned = 0;

        for (int i = 0; i < wavesNum; i++) {
            layersSpawned = spawnWave(entity, dyingEntity, stack, i);
        }

        if (layersSpawned > 0) {
            level.addFreshEntity(new ScreenShakeSoundedEntity(level, dyingEntity.position(), getZoneRadiusStat(entity, stack),
                    layersSpawned, wavesNum, 40));
        }
    }

    private static int spawnWave(LivingEntity entity, LivingEntity dyingEntity, ItemStack stack, int waveIndex) {
        Level level = entity.getCommandSenderWorld();
        Vec3 pos = dyingEntity.position();

        float shift = 1.25F; // fangs shift along circle
        float shiftMultiplier = 2.5F; // distance from center
        int delayTicks = 3;
        int layersSpawned = 0;

        for (int r = 0; r < getZoneRadiusStat(entity, stack); r++) {
            int fangsNum = 6;
            boolean fangSpawned = false;

            for (int i = 0; i < fangsNum; i++) {
                float angle = (float) (i * Math.PI * 2.0F / fangsNum + shift);

                if (spawnFang(level, entity, dyingEntity,
                        pos.x + (double) Mth.cos(angle) * shiftMultiplier,
                        pos.z + (double) Mth.sin(angle) * shiftMultiplier,
                        i, waveIndex * getWaveTicks() + delayTicks, getZoneDamageStat(entity, stack),
                        true)) {
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
                                    double posX, double posZ, float yRot, int delayTicks, float damage,
                                    boolean summonedWithZone) {
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

            runeEntity.setCaster(caster);
            runeEntity.setSilent(summonedWithZone);
            runeEntity.setSummonedWithZone(summonedWithZone);

            if (!summonedWithZone) {
                ItemStack stack = EntityUtils.findEquippedCurio(caster, RECItems.VOID_MANTLE.get());

                if (stack.isEmpty()) {
                    return false;
                }

                // rank 3 - activate rune attraction
                if (isRankModifierUnlocked(caster, stack, 3)) {
                    runeEntity.setAttractionRadius(getRadiusStat(caster, stack));
                    runeEntity.setAttractionForce(RECItemUtils.getIntStat(caster, stack, ABILITY_ID, "attraction_force"));
                }

                drawRuneCircle((ServerLevel) level, target, 0);
            }

            level.addFreshEntity(runeEntity);
        }

        return canFangSpawn;
    }

    public static void drawRuneCircle(ServerLevel level, LivingEntity runeTarget, int cooldown) {
        int particlesTotalNum = 16;
        double step = (2 * Math.PI) / particlesTotalNum;

        double progress = (20 - cooldown) / 20D;
        int particlesNum = Math.max(1, (int) (progress * particlesTotalNum));

        double px = runeTarget.getX() + runeTarget.getDeltaMovement().x;
        double py = runeTarget.getY() + 0.1D;
        double pz = runeTarget.getZ() + runeTarget.getDeltaMovement().z;

        int r = (int) Math.abs(CIRCLE_START_COLOR.getRed() - CIRCLE_END_COLOR.getRed() * progress);
        int g = (int) Math.abs(CIRCLE_START_COLOR.getGreen() - CIRCLE_END_COLOR.getGreen() * progress);
        int b = (int) Math.abs(CIRCLE_START_COLOR.getBlue() - CIRCLE_END_COLOR.getBlue() * progress);

        for (int i = 0; i < particlesNum; i++) {
            double angle = i * step;

            level.sendParticles(ParticleUtils.constructSimpleSpark(
                            new Color(r, g, b),
                            0.15F, progress < 1D ? 10 : 35, 0.85F),
                    px + 0.5D * Math.cos(angle), py, pz + 0.5D * Math.sin(angle),
                    1, 0, 0, 0, 0);
        }
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
    public static boolean isRuneDisabled(LivingEntity entity, ItemStack stack) {
        return ((VoidMantleItem) stack.getItem()).getAbilityMode(entity, stack, ABILITY_ID).equals("disabled");
    }

    public static Color getRuneColor() {
        return CIRCLE_END_COLOR;
    }

    public static int getRadiusStat(LivingEntity entity, ItemStack stack) {
        return RECItemUtils.getIntStat(entity, stack, ABILITY_ID, "radius");
    }

    public static int getStunStatTicks(LivingEntity entity, ItemStack stack) {
        return RECItemUtils.getTickStat(entity, stack, ABILITY_ID, "stun_time");
    }

    private static float getRuneDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((VoidMantleItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "damage");
    }

    private static float getZoneDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((VoidMantleItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "zone_damage");
    }

    public static int getZoneRadiusStat(LivingEntity entity, ItemStack stack) {
        return RECItemUtils.getIntStat(entity, stack, ABILITY_ID, "zone_radius");
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
