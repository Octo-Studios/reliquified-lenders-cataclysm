package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponents;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VacuumGloveParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECEntityUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VacuumGloveItem extends RECItem {
    private static final String ABILITY_ID = "vacuum_slowdown";

    private final HashMap<UUID, Vec3> positionsPrev = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .stat(StatTemplate.builder("slowdown")
                                        .initialValue(0.28D, 0.34D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.15D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatTemplate.builder("radius")
                                        .initialValue(6D, 6.5D)
                                        .upgradeModifier(RelicsScalingModels.ADDITIVE.get(), 0.35D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
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
        for (LivingEntity entityOther : RECEntityUtils.getEntitiesInArea(entity, level, getRadiusStat(entity, stack))) {
            UUID id = entityOther.getUUID();
            Vec3 posPrev = positionsPrev.getOrDefault(id, entityOther.position());
            positionsPrev.put(id, entityOther.position());

            Vec3 motion = entityOther.position().subtract(posPrev);
            Vec3 directionVector = entityOther.position().subtract(entity.position()).normalize();

            double distanceCurrent = directionVector.length();
            double distanceNext = entity.position().subtract(entityOther.position().add(motion)).length();

            double dotProduct = motion.normalize().dot(directionVector);
            float entityBaseSpeed = (float) entityOther.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

            if (dotProduct > 0.0D && distanceNext > distanceCurrent) {
                float modifier = getModifierValue(entity, stack, entityBaseSpeed, entity.distanceTo(entityOther));

                RECEntityUtils.resetMovementAttribute(entityOther, stack, modifier);
                slowedEntities.add(entityOther.getUUID());

                // particles of circle segment
                NetworkHandler.sendToClientsTrackingEntityAndSelf(
                        new VacuumGloveParticlesPacket(getRadiusStat(entity, stack), entityOther.getId(),
                                entity.getX(), entity.getY(), entity.getZ()), entityOther);
            } else {
                removeSlowdown(slowedEntities, entityOther, stack);
            }
        }

        setSlowedEntities(stack, slowedEntities);

        if (entity.isDeadOrDying()) {
            resetSlowedEntities(level, stack);
        }

        // leveling

        int ticks = stack.getOrDefault(RelicsDataComponents.TIME, 0);

        // +1 for each 10 s of slowdown
        if (ticks % 200 == 0) {
            spreadRelicExperience(entity, stack, 1);
        }

        stack.set(RelicsDataComponents.TIME, ticks + 1);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (newStack.getItem() == stack.getItem()) {
            return;
        }

        resetSlowedEntities(slotContext.entity().level(), stack);
    }

    private static void removeSlowdown(List<UUID> slowedEntities, LivingEntity entity, ItemStack stack) {
        RECEntityUtils.removeMovementAttribute(entity, stack);
        slowedEntities.remove(entity.getUUID());
    }

    public float getModifierValue(LivingEntity entity, ItemStack stack, float speed, float distance) {
        float radius = getRadiusStat(entity, stack);

        if (distance == 0.0F || speed == 0.0F || distance > radius) {
            return 0.0F;
        }

        float minSpeed = getSlowdownStat(entity, stack) * speed;
        float slowdownSpeed = minSpeed + (radius - distance) * (speed - minSpeed) / radius;

        return (slowdownSpeed - speed);
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
}
