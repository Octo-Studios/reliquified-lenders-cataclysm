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
                                        .initialValue(15D, 12D)
                                        .upgradeModifier(UpgradeOperation.ADD, -0.5D)
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
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("quakes")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5D)
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
                || !isAbilityTicking(stack, "void_rune")) {
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
                spawnVoidRune(level, player, mob, stack);
                voidRuneTime = ItemUtils.getCooldownStat(stack, "void_rune");
            }
        }

        stack.set(RECDataComponentRegistry.VOID_RUNE_TIME, voidRuneTime);
    }

    private void spawnVoidRune(Level level, Player player, PathfinderMob mob, ItemStack stack) {
        Vec3 mobMovement = mob.getDeltaMovement();

        spawnFang(level, player, mob, mob.getX() + mobMovement.x, mob.getZ() + mobMovement.z * 3,
                0, -20, getDamageStat(stack));
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

        level.explode(player, entity.getX(), entity.getY(), entity.getZ(),
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
