package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.head;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidShardModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
                                .stat(StatTemplate.builder("attack_blocks")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(ScalingModelRegistry.ADDITIVE.get(), 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("projectiles")
                                        .initialValue(16D, 20D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.22D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(0.4D, 0.6D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.56D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), -0.068D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
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
        if (!(stack.getItem() instanceof VoidBubbleItem relic)) {
            return;
        }

        LivingEntity entity = slotContext.entity();

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        if (getAttackBlocks(stack) == getAttackBlocksStat(entity, stack)) {
            setCooldown(relic, entity, stack);
            stack.set(RECDataComponentRegistry.ATTACK_BLOCKS, 0);
        }

        if (getAbilityCooldown(entity, stack, ABILITY_ID) == 1) {
            ItemUtils.playCooldownSound(level, entity);
        }
    }

    /**
     * Ability {@code protective_bubble} <b>[1]</b>: remove the need for air
     */
    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();

        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.VOID_BUBBLE.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidBubbleItem)) {
            return;
        }

        event.setCanBreathe(true);
    }

    /**
     * Ability {@code protective_bubble} <b>[2]</b>: block N attacks, fire N projectiles, then set cooldown
     */
    @SubscribeEvent
    public static void onPlayerDamaged(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Entity)) {
            return;
        }

        LivingEntity entity = event.getEntity();

        Level level = entity.getCommandSenderWorld();
        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.VOID_BUBBLE.get());

        if (level.isClientSide || stack.isEmpty() || !(stack.getItem() instanceof VoidBubbleItem relic)
                || relic.isAbilityOnCooldown(entity, stack, ABILITY_ID)) {
            return;
        }

        int attackBlocks = getAttackBlocks(stack);
        int attackBlocksStat = relic.getAttackBlocksStat(entity, stack);

        if (attackBlocks < attackBlocksStat) {
            if (attackBlocks == attackBlocksStat - 1) {
                relic.spawnShards(entity, stack);
            }

            relic.spreadRelicExperience(entity, stack, 1);
            level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS);

            event.setCanceled(true);

            stack.set(RECDataComponentRegistry.ATTACK_BLOCKS, attackBlocks + 1);
        }
    }

    private void spawnShards(LivingEntity caster, ItemStack stack) {
        Level level = caster.getCommandSenderWorld();
        int projectilesNum = ItemUtils.getIntStat(caster, stack, ABILITY_ID, "projectiles");

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

    // simple getters & setters

    private static int getAttackBlocks(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.ATTACK_BLOCKS, 0);
    }

    private int getAttackBlocksStat(LivingEntity entity, ItemStack stack) {
        return (int) Math.round(getStatValue(entity, stack, ABILITY_ID, "attack_blocks"));
    }

    private float getDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) getStatValue(entity, stack, ABILITY_ID, "damage");
    }

    private static void setCooldown(RelicItem relic, LivingEntity entity, ItemStack stack) {
        relic.setAbilityCooldown(entity, stack, ABILITY_ID, ItemUtils.getCooldownStat(entity, stack, ABILITY_ID));
    }
}
