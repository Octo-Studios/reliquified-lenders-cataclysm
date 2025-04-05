package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class ItemUtils {
    public static void resetMovementAttribute(LivingEntity entity, ItemStack stack, float value) {
        EntityUtils.resetAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                value, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void removeMovementAttribute(LivingEntity entity, ItemStack stack) {
        EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                AttributeModifier.Operation.ADD_VALUE);
    }

    public static void playCooldownSound(Level level, LivingEntity entity) {
        level.playSound(null, entity.blockPosition(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.0F, 1F);
    }

    public static List<LivingEntity> getEntitiesInArea(LivingEntity centerEntity, Level level, AABB area) {
        return level.getEntities(null, area).stream()
                .map(entity -> entity instanceof LivingEntity livingEntity
                        && !entity.equals(centerEntity) && !(entity instanceof ArmorStand)
                        ? livingEntity : null)
                .filter(Objects::nonNull).toList();
    }

    public static int getIntStat(ItemStack stack, String ability, String stat) {
        return (int) Math.round(((IRelicItem) stack.getItem()).getStatValue(stack, ability, stat));
    }

    private static int getTickStat(IRelicItem relic, ItemStack stack, String ability, String stat) {
        return (int) Math.floor(relic.getStatValue(stack, ability, stat) * 20);
    }

    public static int getTickStat(ItemStack stack, String ability, String stat) {
        return getTickStat((IRelicItem) stack.getItem(), stack, ability, stat);
    }

    public static int getCooldownStat(ItemStack stack, String ability) {
        return getTickStat((IRelicItem) stack.getItem(), stack, ability, "cooldown");
    }
}
