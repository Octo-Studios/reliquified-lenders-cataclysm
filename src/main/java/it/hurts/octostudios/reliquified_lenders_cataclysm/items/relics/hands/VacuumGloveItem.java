package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VacuumGloveParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathUtils;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("slowdown")
                                        .initialValue(0.28D, 0.34D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(MathUtils::roundPercents)
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(6D, 6.5D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.35D)
                                        .formatValue(MathUtils::roundInt)
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
                                        .gem(GemShape.SQUARE, GemColor.PURPLE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(RECLootEntries.CURSED_PYRAMID, LootEntries.THE_END)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFDE389)
                                .borderBottom(0xFFDBA461)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF550E5B)
                                .endColor(0x00CC4AD5)
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

            if (entity.distanceTo(entitySlowed) > getRadiusStat(stack) || entitySlowed.isDeadOrDying()) {
                removeSlowdown(slowedEntities, entitySlowed, stack);
            }
        }

        // apply slowdown on mobs inside the area
        for (LivingEntity entityOther : getEntitiesInArea(level, entity, getRadiusStat(stack))) {
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
                float modifier = getModifierValue(stack, entityBaseSpeed, entity.distanceTo(entityOther));

                ItemUtils.resetMovementAttribute(entityOther, stack, modifier);
                slowedEntities.add(entityOther.getUUID());

                // particles of circle segment
                NetworkHandler.sendToClientsTrackingEntityAndSelf(
                        new VacuumGloveParticlesPacket(getRadiusStat(stack), entityOther.getId(),
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

        int ticks = stack.getOrDefault(DataComponentRegistry.TIME, 0);

        // +1 for each 10 s of slowdown
        if (ticks % 200 == 0) {
            spreadRelicExperience(entity, stack, 1);
        }

        stack.set(DataComponentRegistry.TIME, ticks + 1);
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
        ItemUtils.removeMovementAttribute(entity, stack);
        slowedEntities.remove(entity.getUUID());
    }

    private static List<LivingEntity> getEntitiesInArea(Level level, LivingEntity entityCenter, float radius) {
        AABB sphereArea = new AABB(entityCenter.blockPosition()).inflate(radius);

        return ItemUtils.getEntitiesInArea(entityCenter, level, sphereArea);
    }

    public float getModifierValue(ItemStack stack, float speed, float distance) {
        float radius = getRadiusStat(stack);

        if (distance == 0.0F || speed == 0.0F || distance > radius) {
            return 0.0F;
        }

        float minSpeed = getSlowdownStat(stack) * speed;
        float slowdownSpeed = minSpeed + (radius - distance) * (speed - minSpeed) / radius;

        return (slowdownSpeed - speed);
    }

    private float getSlowdownStat(ItemStack stack) {
        return (float) (1.0F - getStatValue(stack, ABILITY_ID, "slowdown"));
    }

    public float getRadiusStat(ItemStack stack) {
        return (float) getStatValue(stack, ABILITY_ID, "radius");
    }

    public ArrayList<UUID> getSlowedEntities(ItemStack stack) {
        return new ArrayList<>(stack.getOrDefault(RECDataComponentRegistry.SLOWED_ENTITIES, new ArrayList<>()));
    }

    public void setSlowedEntities(ItemStack stack, List<UUID> entities) {
        stack.set(RECDataComponentRegistry.SLOWED_ENTITIES, entities);
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

            ItemUtils.removeMovementAttribute(target, stack);
        }

        stack.set(RECDataComponentRegistry.SLOWED_ENTITIES, new ArrayList<>());
    }
}
