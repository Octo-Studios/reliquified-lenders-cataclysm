package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECMathUtils;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VacuumGloveItem extends RECItem {
    private static final String ABILITY_ID = "vacuum_slowdown";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("slowdown")
                                        .initialValue(0.28D, 0.34D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(RECMathUtils::roundPercents)
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(6D, 6.5D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.35D)
                                        .formatValue(RECMathUtils::roundInt)
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
                                .build())
                        .beams(BeamsData.builder()
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (stack.isEmpty() || !(slotContext.entity() instanceof Player player)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        ArrayList<UUID> slowedEntities = getSlowedEntities(stack);

        for (int i = 0; i < slowedEntities.size(); i++) {
            if (!(((ServerLevel) level).getEntity(slowedEntities.get(i)) instanceof LivingEntity entity)) {
                continue;
            }

            if (player.distanceTo(entity) > getRadiusStat(stack) || entity.isDeadOrDying()) {
                ItemUtils.removeMovementAttribute(entity, stack);
                slowedEntities.remove(slowedEntities.get(i));
            }
        }

        // apply slowdown on mobs inside the area
        for (LivingEntity entity : getEntitiesInArea(level, player, getRadiusStat(stack))) {
            ItemUtils.resetMovementAttribute(entity, stack,
                    getModifierValue(stack, entity.getSpeed(), player.distanceTo(entity)));
            slowedEntities.add(entity.getUUID());
        }

        setSlowedEntities(stack, slowedEntities);

        if (player.isDeadOrDying()) {
            resetSlowedEntities(level, stack);
        }

        // leveling

        int ticks = stack.getOrDefault(DataComponentRegistry.TIME, 0);

        // +1 for each 10 s of slowdown, +1 for each 5 slowed mobs
        if (ticks % 200 == 0) {
            spreadRelicExperience(player, stack,
                    1 + (int) Math.floor(slowedEntities.size() / 5D));
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

        return slowdownSpeed - speed;
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
