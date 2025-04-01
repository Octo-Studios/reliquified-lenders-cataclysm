package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.head;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Scatter_Arrow_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Void_Shard_Entity;
import com.github.L_Ender.cataclysm.init.ModEntities;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECMathUtils;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

@EventBusSubscriber
public class VoidBubbleItem extends RECItem {
    private static final String ABILITY_ID = "protective_bubble";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("attack_blocks")
                                        .initialValue(2D, 3D)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("projectiles")
                                        .initialValue(4D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.3D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.07D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(50)
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
                        .entry(LootEntries.END_LIKE, LootEntries.THE_END)
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
        if (!(slotContext.entity() instanceof Player player)
                || !(stack.getItem() instanceof VoidBubbleItem relic)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        if (getAttackBlocks(stack) == getAttackBlocksStat(stack)) {
            setCooldown(relic, stack);
            stack.set(RECDataComponentRegistry.ATTACK_BLOCKS, 0);
        }

        if (getAbilityCooldown(stack, ABILITY_ID) == 1) {
            ItemUtils.playCooldownSound(level, player);
        }
    }

    /**
     * Ability {@code protective_bubble} <b>[1]</b>: remove the need for air
     */
    @SubscribeEvent
    public static void onPlayerBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_BUBBLE.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof VoidBubbleItem)) {
            return;
        }

        event.setCanBreathe(true);
    }

    /**
     * Ability {@code protective_bubble} <b>[2]</b>: block N attacks, fire N projectiles, then set cooldown
     */
    @SubscribeEvent
    public static void onPlayerDamaged(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getSource().getEntity() instanceof Entity)) {
            return;
        }

        Level level = player.getCommandSenderWorld();
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.VOID_BUBBLE.get());

        if (level.isClientSide || stack.isEmpty() || !(stack.getItem() instanceof VoidBubbleItem relic)
                || relic.isAbilityOnCooldown(stack, ABILITY_ID)) {
            return;
        }

        int attackBlocks = getAttackBlocks(stack);
        int attackBlocksStat = relic.getAttackBlocksStat(stack);

        if (attackBlocks < attackBlocksStat) {
            if (attackBlocks == attackBlocksStat - 1) {
                relic.spawnShards(player, stack);
                relic.spreadRelicExperience(player, stack, 1);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS);

            event.setCanceled(true);

            stack.set(RECDataComponentRegistry.ATTACK_BLOCKS, attackBlocks + 1);
        }
    }

    private void spawnShards(Player player, ItemStack stack) {
        Level level = player.getCommandSenderWorld();

        Void_Scatter_Arrow_Entity arrowEntity =
                new Void_Scatter_Arrow_Entity(ModEntities.VOID_SCATTER_ARROW.get(), level);
        List<Vec3> movementVecs = arrowEntity.getShootVectors(player.getRandom(), 0.0F);
        Vec3 movementVec;

        for (int i = 0; i < (int) getStatValue(stack, ABILITY_ID, "projectiles"); i++) {
            movementVec = movementVecs.get(i);
            movementVec = movementVec.scale(0.35D);

            Entity shardEntity = new Void_Shard_Entity(level, player,
                    player.getX() + movementVec.x,
                    player.getY() + movementVec.y + 0.25D,
                    player.getZ() + movementVec.z,
                    movementVec, player);
            level.addFreshEntity(shardEntity);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL);
    }

    private static int getAttackBlocks(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.ATTACK_BLOCKS, 0);
    }

    private int getAttackBlocksStat(ItemStack stack) {
        return (int) getStatValue(stack, ABILITY_ID, "attack_blocks");
    }

    private static void setCooldown(IRelicItem relic, ItemStack stack) {
        relic.setAbilityCooldown(stack, ABILITY_ID, ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }
}
