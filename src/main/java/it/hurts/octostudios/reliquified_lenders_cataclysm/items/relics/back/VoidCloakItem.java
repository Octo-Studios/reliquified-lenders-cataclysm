package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

@EventBusSubscriber
public class VoidCloakItem extends RelicItem {
    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("void_invulnerability")
                                .build())
                        .ability(AbilityData.builder("void_rune")
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(12, 15)
                                        .upgradeModifier(UpgradeOperation.ADD, -1)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(0.75D, 1.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.45D)
                                        .formatValue(value -> MathUtils.round(value * 2, 1))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("seismic_zone")
                                .stat(StatData.builder("radius")
                                        .initialValue(2, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("quakes")
                                        .initialValue(2, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .step(100)
                        .maxLevel(5)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData
                                        .abilityBuilder("void_rune")
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .source(LevelingSourceData
                                        .abilityBuilder("seismic_zone")
                                        .gem(GemShape.SQUARE, GemColor.CYAN)
                                        .build())
                                .build())
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .build())
                        .beams(BeamsData.builder()
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (stack.isEmpty() || !(slotContext.entity() instanceof Player player)
                || !(stack.getItem() instanceof VoidCloakItem relic) || !isAbilityTicking(stack, "void_rune")) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int voidRuneTime = stack.getOrDefault(RECDataComponentRegistry.VOID_RUNE_TIME, 0);

        if (voidRuneTime > 0) {
            voidRuneTime--;
        } else {
            PathfinderMob mob = level.getNearestEntity(PathfinderMob.class, TargetingConditions.DEFAULT,
                    player, player.getX(), player.getY(), player.getZ(),
                    player.getBoundingBox().inflate(10.0D));

            if (mob == null) {
                return;
            }

            LivingEntity targetEntity = mob.getTarget();

            if (targetEntity != null && targetEntity.is(player)) {
                // todo: increase the effectiveness and accuracy
                spawnVoidRuneRow(level, player, mob, stack);
                voidRuneTime = ItemUtils.getTickStat(relic, stack, "void_rune", "cooldown");
            }
        }

        stack.set(RECDataComponentRegistry.VOID_RUNE_TIME, voidRuneTime);
    }

    private void spawnVoidRuneRow(Level level, Player player, PathfinderMob mob, ItemStack stack) {
        float shiftRad = (float) (Math.toRadians(mob.yHeadRot - 90));
        float damage = getDamageStat(stack);

        if (player.getXRot() > 70) {
            for (int i = 0; i < 5; i++) {
                float yRot = shiftRad + i * (float) Math.PI * 0.4F;
                double shift = (double) Mth.cos(yRot) * 1.5D;

                spawnFang(level, player,
                        player.getX() + shift, player.getZ() + shift,
                        yRot, 0, damage);
            }

            for (int i = 0; i < 8; i++) {
                float yRot = shiftRad + i * (float) Math.PI / 4.0F + 1.2566371F;
                double shift = (double) Mth.cos(yRot) * 2.5D;

                spawnFang(level, player,
                        player.getX() + shift, player.getZ() + shift,
                        yRot, 3, damage);
            }
        } else {
            for (int i = 0; i < 10; i++) {
                double shiftForIndex = 1.25D * (i + 1);

                spawnFang(level, player,
                        player.getX() + Mth.cos(shiftRad) * shiftForIndex,
                        player.getZ() + Mth.sin(shiftRad) * shiftForIndex,
                        shiftRad, i, damage);
            }
        }
    }

    /**
     * Ability {@code void_invulnerability}: complete suppression of void damage
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.getCommandSenderWorld().isClientSide) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem)) {
            return;
        }

        if (player.getY() < player.getCommandSenderWorld().getMinBuildHeight()) {
            event.setCanceled(true);
        }
    }

    /**
     * Ability {@code seismic_zone}: spawn seismic zone of void rune if player or entity owned by player dies
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = null;

        if (entity instanceof Player) {
            player = (Player) entity;
        } else if (entity instanceof OwnableEntity ownableEntity) {
            LivingEntity entityOwner = ownableEntity.getOwner();

            if (!(entityOwner instanceof Player entityOwnerPlayer)) {
                return;
            }

            player = entityOwnerPlayer;
        }

        if (player == null) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_CLOAK.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidCloakItem relic)
                || !relic.isAbilityUnlocked(stack, "seismic_zone")) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(),
                1.0F, false, Level.ExplosionInteraction.NONE);

        int quakesNum = relic.getQuakesStat(stack);

        for (int i = 0; i < quakesNum; i++) {
            relic.spawnSeismicZone(stack, player, entity, i);
        }

        ScreenShake_Entity.ScreenShake(level, entity.position(), relic.getRadiusStat(stack),
                1.0F / quakesNum, quakesNum * 50, 10);
    }

    private void spawnSeismicZone(ItemStack stack, Player player, LivingEntity dyingEntity, int quakeIndex) {
        Level level = player.getCommandSenderWorld();
        Vec3 pos = dyingEntity.position();
        float damage = getDamageStat(stack);

        // initial 4 fangs (for minimal radius = 1)
        for (int i = 0; i < 4; i++) {
            double shift = 0.5D;

            spawnFang(level, player, dyingEntity,
                    pos.x + (i < 2 ? 1 : -1) * shift, pos.z + Math.pow(-1, i) * shift,
                    i, quakeIndex * 50, damage);
        }

        double shift = 0.5D;
        int delayTicks = 3;

        for (int r = 0; r < getRadiusStat(stack); r++) {
            // xp & xn
            for (int i = 0; i < 4; i++) {
                spawnFang(level, player, dyingEntity,
                        pos.x + getAxisShift(r, i, shift), pos.z + Math.pow(-1, i) * shift,
                        i, quakeIndex * 50 + delayTicks, damage);
            }

            // zp & zn
            for (int i = 0; i < 4; i++) {
                spawnFang(level, player, dyingEntity,
                        pos.x + Math.pow(-1, i) * shift, pos.z + getAxisShift(r, i, shift),
                        i, quakeIndex * 50 + delayTicks, damage);
            }

            delayTicks += 2;
        }

        /*
        scheme of fangs spawn [so far]:
                zp zp
              xn # # xp
              xn # # xp
                zn zn
        # - initial fang;
        xp - fang in positive X direction, xn - negative X;
        zp - positive Z direction, zn - negative Z;
        for each r, num of fangs pair on each side increases by 1,
        so fangs form '+'-shape
        */
    }

    private double getAxisShift(int radiusIndex, int fangIndex, double shift) {
        double axisShift = shift * (radiusIndex + 2) + 1.0F * (radiusIndex + 1);

        return fangIndex < 2 ? axisShift : -axisShift;
    }

    private static void spawnFang(Level level, Player player, LivingEntity entity,
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

        level.addFreshEntity(new Void_Rune_Entity(level,
                posX, blockPos.getY() + shiftY, posZ, yRot, delayTicks, damage, player));
    }

    private static void spawnFang(Level level, Player player,
                                  double posX, double posZ, float yRot, int delayTicks, float damage) {
        spawnFang(level, player, player, posX, posZ, yRot, delayTicks, damage);
    }

    private float getDamageStat(ItemStack stack) {
        return (float) getStatValue(stack, "void_rune", "damage");
    }

    private int getQuakesStat(ItemStack stack) {
        return (int) getStatValue(stack, "seismic_zone", "quakes");
    }

    private int getRadiusStat(ItemStack stack) {
        return (int) getStatValue(stack, "seismic_zone", "radius");
    }
}
