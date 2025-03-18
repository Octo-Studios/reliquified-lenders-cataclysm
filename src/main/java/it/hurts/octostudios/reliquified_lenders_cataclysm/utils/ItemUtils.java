package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class ItemUtils {
    public static List<Mob> getMobsInArea(Level level, AABB area) {
        return level.getEntities(null, area).stream()
                .map(entity -> entity instanceof Mob mob ? mob : null).filter(Objects::nonNull).toList();
    }
}
