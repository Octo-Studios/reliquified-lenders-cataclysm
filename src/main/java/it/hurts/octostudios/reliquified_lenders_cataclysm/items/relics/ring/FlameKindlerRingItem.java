package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.ring;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.FlameJetModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

@EventBusSubscriber
public class FlameKindlerRingItem extends RECItem {
    public static final String ABILITY_ID = "flame_summon";

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .stat(StatTemplate.builder("radius")
                                        .initialValue(5D, 6D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.363D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("chance")
                                        .initialValue(0.25D, 0.3D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.167D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatTemplate.builder("jets")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.3D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.1D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_NETHER)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFE432B)
                                .borderBottom(0xFFC50921)
                                .textured(true)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        List<LivingEntity> entitiesAround = ItemUtils.getEntitiesInArea(entity, level, getRadiusStat(entity, stack));

        for (LivingEntity target : entitiesAround) {
            if (!target.wasOnFire && target.isOnFire()) {
                spawnJets(level, entity, target, stack, 0F); // jets spawn is guaranteed
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.getCommandSenderWorld();

        if (level.isClientSide || !(event.getSource().getDirectEntity() instanceof LivingEntity entity)
                || EntityUtils.isAlliedTo(entity, target)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.FLAME_KINDLER_RING.get());

        if (stack.isEmpty()) {
            return;
        }

        spawnJets(level, entity, target, stack, entity.getRandom().nextDouble());
    }

    private static void spawnJets(Level level, LivingEntity sourceEntity, LivingEntity targetEntity, ItemStack stack, double chance) {
        if (chance > getChanceStat(sourceEntity, stack)) {
            return;
        }

        int jetsNum = MathUtils.randomBetween(sourceEntity.getRandom(), 1, getJetsStat(sourceEntity, stack));
        double inaccuracy = 0.1D * jetsNum; // summon area of jets depends on the num of jets
        Vec3 motion = targetEntity.getDeltaMovement();

        // summon N jets next to the target
        for (int i = 0; i < jetsNum; i++) {
            double x = targetEntity.getX() + motion.x + randomizedAround(sourceEntity, inaccuracy);
            double y = targetEntity.getY() + motion.y;
            double z = targetEntity.getZ() + motion.z + randomizedAround(sourceEntity, inaccuracy);

            // ensure that potential spawn pos of jet is valid
            BlockPos spawnPos = ItemUtils.getValidSpawnPos(level, BlockPos.containing(x, y, z));

            if (spawnPos == null) {
                continue;
            }

            FlameJetModifiedEntity flameJet = new FlameJetModifiedEntity(level,
                    spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                    targetEntity.getYRot(), i + 2, getDamageStat(targetEntity, stack), sourceEntity);

            level.addFreshEntity(flameJet);
        }

        ((FlameKindlerRingItem) stack.getItem()).spreadRelicExperience(sourceEntity, stack, 1);
    }

    private static double randomizedAround(LivingEntity entity, double inaccuracy) {
        return MathUtils.randomBetween(entity.getRandom(), -1.0D - inaccuracy, 1.0D + inaccuracy);
    }

    private static double getChanceStat(LivingEntity entity, ItemStack stack) {
        return ((FlameKindlerRingItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "chance");
    }

    private static double getRadiusStat(LivingEntity entity, ItemStack stack) {
        return ((FlameKindlerRingItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "radius");
    }

    private static int getJetsStat(LivingEntity entity, ItemStack stack) {
        return (int) ((FlameKindlerRingItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "jets");
    }

    private static float getDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) ((FlameKindlerRingItem) stack.getItem()).getStatValue(entity, stack, ABILITY_ID, "damage");
    }
}
