package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import com.github.L_Ender.cataclysm.entity.effect.Void_Vortex_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

@EventBusSubscriber
public class VoidVortexInBottleItem extends RelicItem {
    private static final String ABILITY_ID = "spawn_vortex";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("lifespan")
                                        .initialValue(5, 7)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1F)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(0.2D, 0.35D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> MathUtils.round(value * 2, 1))
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(50, 60)
                                        .upgradeModifier(UpgradeOperation.ADD, -1)
                                        .formatValue(value -> MathUtils.round(value, 1))
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

        int vortexId = stack.getOrDefault(RECDataComponentRegistry.VORTEX_ID.get() ,0);
        Entity voidVortexEntity = level.getEntity(vortexId);

        if (voidVortexEntity != null) {
            damageMobsInVortex(level, voidVortexEntity, stack);
        }
    }

    private void damageMobsInVortex(Level level, Entity vortex, ItemStack stack) {
        // variable from the code of void vortex entity
        AABB vortexArea =
                new AABB(vortex.getX() - 3.0, vortex.getY(), vortex.getZ() - 3.0,
                        vortex.getX() + 3.0, vortex.getY() + 15.0, vortex.getZ() + 3.0);
        List<Mob> mobsInArea = ItemUtils.getMobsInArea(level, vortexArea);

        for (Mob mob : mobsInArea) {
            mob.hurt(level.damageSources().magic(), getDamageStat(stack));
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_VORTEX_IN_BOTTLE.get());

        if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        Level level = player.getCommandSenderWorld();
        LivingEntity target = event.getEntity();
        int lifespanTicks = ItemUtils.getTickStat(stack, ABILITY_ID, "lifespan");
        Entity voidVortexEntity = new Void_Vortex_Entity(level,
                target.getX(), target.getY(), target.getZ(), target.getYRot(), player, lifespanTicks);

        level.addFreshEntity(voidVortexEntity);
        stack.set(RECDataComponentRegistry.VORTEX_ID.get(), voidVortexEntity.getId());

        player.getCooldowns().addCooldown(stack.getItem(), ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }

    private float getDamageStat(ItemStack stack) {
        return (float) (getStatValue(stack, ABILITY_ID, "damage"));
    }
}
