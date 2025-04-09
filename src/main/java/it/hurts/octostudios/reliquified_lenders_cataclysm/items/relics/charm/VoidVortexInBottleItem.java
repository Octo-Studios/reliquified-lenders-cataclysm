package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.VoidVortexModifiedEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.data.RECLootEntries;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECMathUtils;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.Objects;

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
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(6.0D, 8.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(RECMathUtils::roundDamage)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.06D)
                                        .formatValue(RECMathUtils::roundOneDigit)
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
        if (stack.isEmpty() || !(slotContext.entity() instanceof Player player)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        float cooldownPercent = player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F);

        // play sound on cooldown ending
        if (cooldownPercent > 0.0F && cooldownPercent <= (float) 1 / ItemUtils.getCooldownStat(stack, ABILITY_ID)) {
            ItemUtils.playCooldownSound(level, player);
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();

        if (level.isClientSide || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_VORTEX_IN_BOTTLE.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidVortexInBottleItem relic)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        LivingEntity target = event.getEntity();
        VoidVortexModifiedEntity voidVortexEntity = new VoidVortexModifiedEntity(level,
                target.getX(), target.getY(), target.getZ(), player.getYRot(), player, 100,
                ItemUtils.getIntStat(stack, ABILITY_ID, "height"), relic.getDamageStat(stack));

        // get vortices colliding with target
        AABB targetBox = target.getBoundingBox();
        List<Entity> vorticesIntersecting = level.getEntities(target, targetBox, entityOther ->
                targetBox.intersects(entityOther.getBoundingBox())).stream()
                .map(entity -> entity instanceof VoidVortexModifiedEntity ? entity : null).filter(Objects::nonNull)
                .toList();

        // if target is in vortex, don't spawn vortex (it's a dup)
        if (!vorticesIntersecting.isEmpty()) {
            return;
        }

        level.addFreshEntity(voidVortexEntity);

        relic.spreadRelicExperience(player, stack, 3);
        player.getCooldowns().addCooldown(stack.getItem(), ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }

    private float getDamageStat(ItemStack stack) {
        return (float) (getStatValue(stack, ABILITY_ID, "damage"));
    }
}
