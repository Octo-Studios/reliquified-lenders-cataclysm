package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class RECEntityUtils {
    public static void resetMovementAttribute(LivingEntity entity, ItemStack stack, float value) {
        EntityUtils.resetAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                value, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void removeMovementAttribute(LivingEntity entity, ItemStack stack) {
        EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED,
                AttributeModifier.Operation.ADD_VALUE);
    }

    public static List<LivingEntity> getEntitiesInArea(LivingEntity caster, Level level, AABB area) {
        return level.getEntities(null, area).stream()
                .map(entity -> entity instanceof LivingEntity livingEntity
                        && !entity.equals(caster) && !(EntityUtils.isAlliedTo(entity, caster))
                        && !(entity instanceof ArmorStand) ? livingEntity : null)
                .filter(Objects::nonNull).toList();
    }

    public static List<LivingEntity> getEntitiesInArea(LivingEntity caster, Level level, double radius) {
        return getEntitiesInArea(caster, level, getSphereArea(caster, radius));
    }

    public static AABB getSphereArea(LivingEntity entity, double radius) {
        return new AABB(entity.blockPosition()).inflate(radius);
    }
}
