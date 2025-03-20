package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.hands;

import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

public class VacuumGloveItem extends RelicItem {
    private static final String ABILITY_ID = "vacuum_slowdown";
    private static final float RADIUS = 6.0F;

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("slowdown")
                                        .initialValue(0.2D, 0.3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.05D)
                                        .formatValue(value -> MathUtils.round(value * 100, 1))
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
        if (stack.isEmpty() || !(slotContext.entity() instanceof Player player)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        List<Mob> entitiesInArea = getEntitiesInArea(level, player, RADIUS);

        if (entitiesInArea.isEmpty()) {
            return;
        }

        // apply slowdown on mobs inside the area
        for (Mob mob : entitiesInArea) {
            EntityUtils.resetAttribute(mob, stack, Attributes.MOVEMENT_SPEED,
                    getModifierValue(stack, mob.getSpeed(), player.distanceTo(mob)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            // debug
//            player.sendSystemMessage(mob.getDisplayName().copy().append(" ")
//                    .append(Component.literal(String.valueOf(mob.getSpeed()))));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (newStack.equals(stack) || !(slotContext.entity() instanceof Player player)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        List<Mob> entitiesInArea = getEntitiesInArea(level, player, RADIUS + 4.0F);

        if (entitiesInArea.isEmpty()) {
            return;
        }

        // remove slowdown
        for (Mob mob : entitiesInArea) {
            EntityUtils.removeAttribute(mob, stack, Attributes.MOVEMENT_SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    private List<Mob> getEntitiesInArea(Level level, Player player, float radius) {
        BlockPos playerPos =
                new BlockPos((int) player.position().x, (int) player.position().y, (int) player.position().z);
        AABB sphereArea = new AABB(playerPos).inflate(radius);

        return ItemUtils.getMobsInArea(level, sphereArea);
    }

    private float getModifierValue(ItemStack stack, float speed, float distance) {
        if (distance == 0.0F || speed == 0.0F || distance > RADIUS) {
            return 0.0F;
        }

        float minSpeed = getSlowdownStat(stack) * speed;
        float slowdownSpeed = minSpeed + (RADIUS - distance) * (speed - minSpeed) / RADIUS;

        return slowdownSpeed - speed;
    }

    private float getSlowdownStat(ItemStack stack) {
        return (float) (1.0F - getStatValue(stack, ABILITY_ID, "slowdown"));
    }
}
