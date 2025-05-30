package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.ring;

import com.github.L_Ender.cataclysm.entity.projectile.Flame_Jet_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.Random;

@EventBusSubscriber
public class FlameKindlerRingItem extends RECItem {
    public static final String ABILITY_ID = "flame_summon";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("jets")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("chance")
                                        .initialValue(0.25D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.167D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .step(100)
                        .maxLevel(10)
                        .sources(LevelingSourcesData.builder()
                                .source(LevelingSourceData
                                        .abilityBuilder(ABILITY_ID)
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.THE_NETHER)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFE432B)
                                .borderBottom(0xFFC50921)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFFF2500)
                                .endColor(0x00DF1008)
                                .build())
                        .build())
                .build();
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.getCommandSenderWorld();
        Entity entity = event.getTarget();
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.FLAME_KINDLER_RING.get());

        if (level.isClientSide || stack.isEmpty()
                || !(entity instanceof LivingEntity target) || EntityUtils.isAlliedTo(player, target)) {
            return;
        }

        if (player.getRandom().nextDouble() <= getChanceStat(stack)) {
            int jetsNum = MathUtils.randomBetween(new Random(), 1, getJetsStat(stack));
            double inaccuracy = 0.1D * jetsNum; // jets summon area depends on the num of jets
            Vec3 motion = target.getDeltaMovement();

            // summon N jets next to the target
            for (int i = 0; i < jetsNum; i++) {
                double x = target.getX() + motion.x + randomizedAround(inaccuracy);
                double y = target.getY() + motion.y;
                double z = target.getZ() + motion.z + randomizedAround(inaccuracy);

                level.addFreshEntity(new Flame_Jet_Entity(level,
                        x, y, z, target.getYRot(), i + 2, getDamageStat(stack), player));
            }

            ((FlameKindlerRingItem) stack.getItem()).spreadRelicExperience(player, stack, 1);
        }
    }

    private static double randomizedAround(double inaccuracy) {
        return MathUtils.randomBetween(new Random(), -1.0D - inaccuracy, 1.0D + inaccuracy);
    }

    private static int getJetsStat(ItemStack stack) {
        return (int) ((FlameKindlerRingItem) stack.getItem()).getStatValue(stack, ABILITY_ID, "jets");
    }

    private static float getDamageStat(ItemStack stack) {
        return (float) ((FlameKindlerRingItem) stack.getItem()).getStatValue(stack, ABILITY_ID, "damage");
    }

    private static double getChanceStat(ItemStack stack) {
        return ((FlameKindlerRingItem) stack.getItem()).getStatValue(stack, ABILITY_ID, "chance");
    }
}
