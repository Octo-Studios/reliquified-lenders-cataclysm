package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class ItemUtils {
    public static int getTickStat(IRelicItem relic, ItemStack stack, String ability, String stat) {
        return (int) (relic.getStatValue(stack, ability, stat) * 20);
    }

    public static List<Mob> getMobsInArea(Level level, AABB area) {
        return level.getEntities(null, area).stream()
                .map(entity -> entity instanceof Mob mob ? mob : null).filter(Objects::nonNull).toList();
    }
}
