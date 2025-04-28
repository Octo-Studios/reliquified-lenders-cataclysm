package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidVortexModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathUtils;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("height")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.075D)
                                        .formatValue(MathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(6.0D, 8.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(MathUtils::roundHP)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.06D)
                                        .formatValue(MathUtils::roundOneDigit)
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
                        .entry(RECLootEntries.FROSTED_PRISON, LootEntries.THE_END)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFF965DB0)
                                .borderBottom(0xFF77448E)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFF6D22AD)
                                .endColor(0x00A356C5)
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

        if (level.isClientSide || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        for (ItemStack stack : EntityUtils.findEquippedCurios(player, ItemRegistry.VOID_VORTEX_IN_BOTTLE.get())) {
            LivingEntity target = event.getEntity();

            if (!(stack.getItem() instanceof VoidVortexInBottleItem relic)
                    || relic.getVortexCooldown(stack) > 0) {
                continue;
            }

            VoidVortexModifiedEntity voidVortexEntity = new VoidVortexModifiedEntity(level,
                    target.getX(), target.getY(), target.getZ(), player.getYRot(), player, 100,
                    ItemUtils.getIntStat(stack, ABILITY_ID, "height"), relic.getDamageStat(stack));
            voidVortexEntity.setOwner(player);

            level.addFreshEntity(voidVortexEntity);

            relic.spreadRelicExperience(player, stack, 3);
            relic.setVortexCooldown(stack);
        }
    }

    private float getDamageStat(ItemStack stack) {
        return (float) (getStatValue(stack, ABILITY_ID, "damage"));
    }

    private int getVortexCooldown(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.COOLDOWN, 0);
    }

    private void reduceVortexCooldown(ItemStack stack) {
        stack.set(RECDataComponentRegistry.COOLDOWN, getVortexCooldown(stack) - 1);
    }

    private void setVortexCooldown(ItemStack stack) {
        stack.set(RECDataComponentRegistry.COOLDOWN, ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }
}
