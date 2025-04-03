package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import com.github.L_Ender.cataclysm.entity.effect.Void_Vortex_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

@EventBusSubscriber
public class VoidVortexInBottleItem extends RECItem {
    private static final String ABILITY_ID = "spawn_vortex";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("lifespan")
                                        .initialValue(5D, 7D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.12D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(0.8D, 1.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(RECMathUtils::roundDamage)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(60D, 56D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.0375D)
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
                        .entry(RECLootEntries.FROSTED_PRISON,
                                LootEntries.END_LIKE, LootEntries.THE_END)
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
            damageEntitiesInVortex(level, player, voidVortexEntity, stack);
        }

        float cooldownPercent = player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F);

        // play sound on cooldown ending
        if (cooldownPercent > 0.0F && cooldownPercent <= (float) 1 / ItemUtils.getCooldownStat(stack, ABILITY_ID)) {
            ItemUtils.playCooldownSound(level, player);
        }
    }

    private void damageEntitiesInVortex(Level level, Player player, Entity vortex, ItemStack stack) {
        double shift = 4.0D; // pull radius

        // variable based on the code of void vortex entity
        AABB vortexArea =
                new AABB(vortex.getX() - shift, vortex.getY(), vortex.getZ() - shift,
                        vortex.getX() + shift, vortex.getY() + 15.0, vortex.getZ() + shift);
        List<LivingEntity> entitiesInArea = ItemUtils.getEntitiesInArea(player, level, vortexArea);

        for (LivingEntity entity : entitiesInArea) {
            // increase pull force
            ItemUtils.resetMovementAttribute(entity, stack, 0.2F);

            Vec3 deltaMovement = entity.position().subtract(vortex.position()).normalize().scale(0.075);

            // pull & hurt entities in custom radius
            entity.setDeltaMovement(entity.getDeltaMovement()
                    .add(0.0, -2.0, 0.0).subtract(deltaMovement));
            entity.hurt(level.damageSources().magic(), getDamageStat(stack));

            ItemUtils.removeMovementAttribute(entity, stack);
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
        int lifespanTicks = ItemUtils.getTickStat(stack, ABILITY_ID, "lifespan");
        Entity voidVortexEntity = new Void_Vortex_Entity(level,
                target.getX(), target.getY(), target.getZ(), target.getYRot(), player, lifespanTicks);

        level.addFreshEntity(voidVortexEntity);
        stack.set(RECDataComponentRegistry.VORTEX_ID.get(), voidVortexEntity.getId());

        relic.spreadRelicExperience(player, stack, 3);

        player.getCooldowns().addCooldown(stack.getItem(), ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }

    private float getDamageStat(ItemStack stack) {
        return (float) (getStatValue(stack, ABILITY_ID, "damage"));
    }
}
