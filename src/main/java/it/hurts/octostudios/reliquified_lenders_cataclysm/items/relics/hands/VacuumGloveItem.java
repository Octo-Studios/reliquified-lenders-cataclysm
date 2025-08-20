package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VacuumGloveParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECEntityUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks.IRankModifier;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.apache.http.annotation.Experimental;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public class VacuumGloveItem extends RECItem {
    private static final String ABILITY_ID = "vacuum_slowdown";

    private final HashMap<UUID, Vec3> positionsPrev = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .rankModifier(1, RankModifier.getModifierByRank(1))
                                .rankModifier(3, RankModifier.getModifierByRank(3))
                                .rankModifier(5, RankModifier.getModifierByRank(5))
                                .stat(StatTemplate.builder("slowdown")
                                        .initialValue(0.1D, 0.15D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.01D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatTemplate.builder("radius")
                                        .initialValue(6D, 6.5D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.2714D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("attack_range")
                                        .initialValue(1D, 1.5D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.2429D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("slowdown_power")
                                        .initialValue(0.05D, 0.07D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0939D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatTemplate.builder("check_radius")
                                        .initialValue(10D, 9.5D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), -0.2143D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage_reduction")
                                        .initialValue(0.1D, 0.12D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.09D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .maxRank(5)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, LootEntries.THE_END)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFDE389)
                                .borderBottom(0xFFDBA461)
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

        ArrayList<UUID> slowedEntities = getSlowedEntities(stack);

        for (int i = 0; i < slowedEntities.size(); i++) {
            if (!(((ServerLevel) level).getEntity(slowedEntities.get(i)) instanceof LivingEntity entitySlowed)) {
                continue;
            }

            if (entity.distanceTo(entitySlowed) > getRadiusStat(entity, stack) || entitySlowed.isDeadOrDying()) {
                removeSlowdown(slowedEntities, entitySlowed, stack);
            }
        }

        // apply slowdown on mobs inside the area
        for (LivingEntity target : RECEntityUtils.getEntitiesInArea(entity, level, getRadiusStat(entity, stack))) {
            UUID id = target.getUUID();
            Vec3 posPrev = positionsPrev.getOrDefault(id, target.position());
            positionsPrev.put(id, target.position());

            Vec3 motion = target.position().subtract(posPrev);
            Vec3 direction = target.position().subtract(entity.position()).normalize();

            double distanceCurrent = direction.length();
            double distanceNext = entity.position().subtract(target.position().add(motion)).length();

            double dot = motion.normalize().dot(direction);
            float baseSpeed = (float) target.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

            if (dot > 0.0D && distanceNext > distanceCurrent) {
                float movementModifier = getMovementModifierValue(entity, stack, baseSpeed, entity.distanceTo(target));

                // rank 3 - slowdown power increases if no other entities in radius
                if (isRankModifierUnlocked(entity, stack, 3) && RECEntityUtils.getEntitiesInArea(target, level,
                        getStatValue(entity, stack, ABILITY_ID, "check_radius")).isEmpty()) {
                    movementModifier *= (float) (1F + getStatValue(entity, stack, ABILITY_ID, "slowdown_power"));
                }

                RECEntityUtils.resetMovementAttribute(target, stack, movementModifier);
                slowedEntities.add(target.getUUID());

                // particles of circle segment
                NetworkHandler.sendToClientsTrackingEntityAndSelf(
                        new VacuumGloveParticlesPacket(getRadiusStat(entity, stack), target.getId(),
                                entity.getX(), entity.getY(), entity.getZ()), target);
            } else {
                removeSlowdown(slowedEntities, target, stack);
            }
        }

        setSlowedEntities(stack, slowedEntities);

        if (entity.isDeadOrDying()) {
            resetSlowedEntities(level, stack);
        }

        // leveling

//        int ticks = stack.getOrDefault(RelicsDataComponents.TIME, 0);
//
//        // +1 for each 10 s of slowdown
//        if (ticks % 200 == 0) {
//            spreadRelicExperience(entity, stack, 1);
//        }
//
//        stack.set(RelicsDataComponents.TIME, ticks + 1);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (newStack.getItem() == stack.getItem()) {
            return;
        }

        resetSlowedEntities(slotContext.entity().level(), stack);
    }

    // rank 5 - decrease incoming damage
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.getCommandSenderWorld();
        ItemStack stack = EntityUtils.findEquippedCurio(entity, RECItems.VACUUM_GLOVE.get());

        if (level.isClientSide || stack.isEmpty() || !isRankModifierUnlocked(entity, stack, 5)
                || !(event.getSource().getEntity() instanceof LivingEntity source)) {
            return;
        }

        var relic = (VacuumGloveItem) stack.getItem();
        float radius = relic.getRadiusStat(entity, stack);
        float distance = entity.distanceTo(source);

        if (distance > radius) {
            return;
        }

        event.setAmount((float) (event.getAmount()
                * (1 - relic.getStatValue(entity, stack, ABILITY_ID, "damage_reduction")) * distance / radius));
    }

    private static void removeSlowdown(List<UUID> slowedEntities, LivingEntity entity, ItemStack stack) {
        RECEntityUtils.removeMovementAttribute(entity, stack);
        slowedEntities.remove(entity.getUUID());
    }

    public float getMovementModifierValue(LivingEntity entity, ItemStack stack, float speed, float distance) {
        float radius = getRadiusStat(entity, stack);

        if (distance == 0.0F || speed == 0.0F || distance > radius) {
            return 0.0F;
        }

        float minSpeed = getSlowdownStat(entity, stack) * speed;
        float slowdownSpeed = minSpeed + (radius - distance) * (speed - minSpeed) / radius;

        return slowdownSpeed - speed;
    }

    private float getSlowdownStat(LivingEntity entity, ItemStack stack) {
        return (float) (1.0F - getStatValue(entity, stack, ABILITY_ID, "slowdown"));
    }

    public float getRadiusStat(LivingEntity entity, ItemStack stack) {
        return (float) getStatValue(entity, stack, ABILITY_ID, "radius");
    }

    public ArrayList<UUID> getSlowedEntities(ItemStack stack) {
        return new ArrayList<>(stack.getOrDefault(RECDataComponents.SLOWED_ENTITIES, new ArrayList<>()));
    }

    public void setSlowedEntities(ItemStack stack, List<UUID> entities) {
        stack.set(RECDataComponents.SLOWED_ENTITIES, entities);
    }

    private void resetSlowedEntities(Level level, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }

        ArrayList<UUID> slowedEntities = getSlowedEntities(stack);

        for (UUID slowedEntity : slowedEntities) {
            if (!(((ServerLevel) level).getEntity(slowedEntity) instanceof LivingEntity target)) {
                continue;
            }

            RECEntityUtils.removeMovementAttribute(target, stack);
        }

        stack.set(RECDataComponents.SLOWED_ENTITIES, new ArrayList<>());
    }

    @Experimental
    public static boolean isRankModifierUnlocked(LivingEntity entity, ItemStack stack, int rank) {
        if (!(stack.getItem() instanceof VacuumGloveItem relic)) {
            return false;
        }

        return relic.isAbilityRankModifierUnlocked(entity, stack, ABILITY_ID, RankModifier.getModifierByRank(rank));
    }

    @Getter
    public enum RankModifier implements IRankModifier {
        A(new RankModifierData("attack_range", 1)),
        B(new RankModifierData("slowdown_power", 3)),
        C(new RankModifierData("border_damage", 5));

        private final RankModifierData data;

        RankModifier(RankModifierData data) {
            this.data = data;
        }

        public static String getModifierByRank(int rank) {
            return IRankModifier.getModifierByRank(RankModifier.class, rank);
        }
    }
}
