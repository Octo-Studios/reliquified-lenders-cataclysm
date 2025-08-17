package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.head;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.void_bubble.VoidShardModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks.IRankModifier;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.Getter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.apache.http.annotation.Experimental;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class VoidBubbleItem extends RECItem {
    private static final String ABILITY_ID = "protective_bubble";

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .rankModifier(1, RankModifier.getModifierByRank(1))
                                .rankModifier(3, RankModifier.getModifierByRank(3))
                                .rankModifier(5, RankModifier.getModifierByRank(5))
                                .stat(StatTemplate.builder("attack_blocks")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.4857D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("projectiles")
                                        .initialValue(16D, 20D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0629D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(0.5D, 0.6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2095D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(15D, 12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0238D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("damage_reduce")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatTemplate.builder("tremor_time")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .maxRank(5)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_END)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFF9FC1)
                                .borderBottom(0xFFE7598B)
                                .textured(true)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(stack.getItem() instanceof VoidBubbleItem)) {
            return;
        }

        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int attackBlocks = getAttackBlocks(stack);
        int cooldown = getCooldown(stack);

        if (cooldown > 1) {
            reduceCooldown(stack);
        } else if (cooldown == 1) {
            setAttackBlocks(stack, attackBlocks - 1);
            reduceCooldown(stack);

            RECItemUtils.playCooldownSound(level, entity);
        } else if (cooldown == 0 && attackBlocks > 0) {
            setCooldown(entity, stack);
        }
    }

    // rank 1 - remove the need for air
    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, RECItems.VOID_BUBBLE.get());

        if (stack.isEmpty() || !isRankModifierUnlocked(entity, stack, 1)) {
            return;
        }

        event.setCanBreathe(true);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.getCommandSenderWorld();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, RECItems.VOID_BUBBLE.get());

        if (level.isClientSide || stack.isEmpty() || !(stack.getItem() instanceof VoidBubbleItem relic)) {
            return;
        }

        // rank 5 - tremor effect on void shard hit
        if (event.getSource().getDirectEntity() instanceof VoidShardModifiedEntity
                && event.getSource().getEntity() instanceof LivingEntity caster
                && isRankModifierUnlocked(entity, stack, 5)) {
            entity.addEffect(new MobEffectInstance(RelicsMobEffects.TREMOR,
                    relic.getTremorTicksStat(caster, stack)), caster);
        }

        // rank 3 - reduce damage on bubble cooldown
        if (isRankModifierUnlocked(entity, stack, 3) && getAttackBlocks(stack) == 0) {
            event.setAmount(event.getAmount() * relic.getDamageReductionStat(entity, stack));
        }

        int attackBlocks = getAttackBlocks(stack);
        int attackBlocksStat = relic.getAttackBlocksStat(entity, stack);

        if (attackBlocks < attackBlocksStat) {
            if (attackBlocks == attackBlocksStat - 1) {
                relic.spawnShards(entity, stack);
                relic.setBubbleCooldown(entity, stack);
            }

            level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS);

            event.setCanceled(true);

            relic.setAttackBlocks(stack, attackBlocks + 1);
        }
    }

    private void spawnShards(LivingEntity caster, ItemStack stack) {
        Level level = caster.getCommandSenderWorld();
        int projectilesNum = RECItemUtils.getIntStat(caster, stack, ABILITY_ID, "projectiles");

        List<Vec3> movementVecs = getShootVectors(caster.getRandom(), projectilesNum);

        for (int i = 0; i < projectilesNum; i++) {
            Vec3 movementVec = movementVecs.get(i).scale(0.35D);

            Entity shardEntity = new VoidShardModifiedEntity(level, caster,
                    caster.getX() + movementVec.x,
                    caster.getY() + movementVec.y + 1.2D,
                    caster.getZ() + movementVec.z,
                    movementVec, caster, getDamageStat(caster, stack));

            level.addFreshEntity(shardEntity);
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL);
    }

    public List<Vec3> getShootVectors(RandomSource random, float projectilesNum) {
        List<Vec3> vectors = new ArrayList<>();
        float turnFraction = (1.0F + Mth.sqrt(5.0F)) / 2.0F; // golden ratio

        for (int i = 1; i <= projectilesNum; i++) {
            float progress = i / projectilesNum;
            float inclination = (float) Math.acos(1.0F - 2.0F * progress); // vertical position on the sphere
            float azimuth = // horizontal rotation around the sphere
                    (float) ((turnFraction * i + random.nextFloat()) * Math.PI * 2);

            double x = Math.sin(inclination) * Math.cos(azimuth);
            double y = Math.cos(inclination);
            double z = Math.sin(inclination) * Math.sin(azimuth);

            Vec3 vec = new Vec3(x, y, z);

            if (i == 1) {
                vec = vec.add(0.0, 1.0, 0.0).scale(0.5);
            }

            vectors.add(vec);
        }

        return vectors;
    }

    // simple getters, setters & stuff

    @Experimental
    public static boolean isRankModifierUnlocked(LivingEntity entity, ItemStack stack, int rank) {
        if (!(stack.getItem() instanceof VoidBubbleItem relic)) {
            return false;
        }

        return relic.isAbilityRankModifierUnlocked(entity, stack, ABILITY_ID, RankModifier.getModifierByRank(rank));
    }

    private static int getAttackBlocks(ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.ATTACK_BLOCKS, 0);
    }

    private int getAttackBlocksStat(LivingEntity entity, ItemStack stack) {
        return (int) Math.round(getStatValue(entity, stack, ABILITY_ID, "attack_blocks"));
    }

    private float getDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) getStatValue(entity, stack, ABILITY_ID, "damage");
    }

    private float getDamageReductionStat(LivingEntity entity, ItemStack stack) {
        return (float) (1.0F - getStatValue(entity, stack, ABILITY_ID, "damage_reduce"));
    }

    private int getTremorTicksStat(LivingEntity entity, ItemStack stack) {
        return RECItemUtils.getTickStat(entity, stack, ABILITY_ID, "tremor_time");
    }

    private int getCooldown(ItemStack stack) {
        return stack.getOrDefault(RECDataComponents.COOLDOWN, 0);
    }

    private void reduceCooldown(ItemStack stack) {
        stack.set(RECDataComponents.COOLDOWN, getCooldown(stack) - 1);
    }

    private void setCooldown(LivingEntity entity, ItemStack stack) {
        stack.set(RECDataComponents.COOLDOWN, RECItemUtils.getCooldownStat(entity, stack, ABILITY_ID));
    }

    private void setBubbleCooldown(LivingEntity entity, ItemStack stack) {
        stack.set(RECDataComponents.COOLDOWN,
                getTremorTicksStat(entity, stack) + RECItemUtils.getCooldownStat(entity, stack, ABILITY_ID));
    }

    private void setAttackBlocks(ItemStack stack, int attackBlocks) {
        stack.set(RECDataComponents.ATTACK_BLOCKS, attackBlocks);
    }

    @Getter
    enum RankModifier implements IRankModifier {
        A(new RankModifierData("infinite_breath", 1)),
        B(new RankModifierData("cooldown_protection", 3)),
        C(new RankModifierData("tremor", 5));

        private final RankModifierData data;

        RankModifier(RankModifierData data) {
            this.data = data;
        }

        public static String getModifierByRank(int rank) {
            return IRankModifier.getModifierByRank(RankModifier.class, rank);
        }
    }
}
