package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidVortexModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.StatTemplate;
import it.hurts.sskirillss.relics.init.ScalingModelRegistry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.SlotContext;

@EventBusSubscriber
public class VoidVortexInBottleItem extends RECItem {
    private static final String ABILITY_ID = "spawn_vortex";

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder(ABILITY_ID)
                                .stat(StatTemplate.builder("height")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.075D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatTemplate.builder("damage")
                                        .initialValue(6.0D, 8.0D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), 0.15D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatTemplate.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(ScalingModelRegistry.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(RECLootEntries.FROSTED_PRISON, LootEntries.THE_END)
                        .build())
                .style(StyleTemplate.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFF965DB0)
                                .borderBottom(0xFF77448E)
                                .textured(true)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        if (stack.isEmpty()) {
            return;
        }

        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int cooldownCurrent = getVortexCooldown(stack);

        if (cooldownCurrent > 0) {
            // play sound on cooldown ending
            if (cooldownCurrent == 1) {
                ItemUtils.playCooldownSound(level, entity);
            }

            reduceVortexCooldown(stack);
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();

        if (level.isClientSide || !(event.getSource().getEntity() instanceof LivingEntity entity)) {
            return;
        }

        for (ItemStack stack : EntityUtils.findEquippedCurios(entity, ItemRegistry.VOID_VORTEX_IN_BOTTLE.get())) {
            LivingEntity target = event.getEntity();

            if (!(stack.getItem() instanceof VoidVortexInBottleItem relic)
                    || relic.getVortexCooldown(stack) > 0) {
                continue;
            }

            VoidVortexModifiedEntity voidVortexEntity = new VoidVortexModifiedEntity(level,
                    target.getX(), target.getY(), target.getZ(), entity.getYRot(), entity, 100,
                    ItemUtils.getIntStat(entity, stack, ABILITY_ID, "height"), relic.getDamageStat(entity, stack));
            voidVortexEntity.setOwner(entity);

            level.addFreshEntity(voidVortexEntity);

            relic.spreadRelicExperience(entity, stack, 3);
            relic.setVortexCooldown(entity, stack);
        }
    }

    private float getDamageStat(LivingEntity entity, ItemStack stack) {
        return (float) (getStatValue(entity, stack, ABILITY_ID, "damage"));
    }

    private int getVortexCooldown(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.COOLDOWN, 0);
    }

    private void reduceVortexCooldown(ItemStack stack) {
        stack.set(RECDataComponentRegistry.COOLDOWN, getVortexCooldown(stack) - 1);
    }

    private void setVortexCooldown(LivingEntity entity, ItemStack stack) {
        stack.set(RECDataComponentRegistry.COOLDOWN, ItemUtils.getCooldownStat(entity, stack, ABILITY_ID));
    }
}
