package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.head;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Shard_Entity;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

import java.util.ArrayList;
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
                                        .initialValue(8D, 10D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.22D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(30D, 25D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.068D)
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
        int projectilesNum = getProjectilesStat(stack);

        List<Vec3> movementVecs = getShootVectors(player.getRandom(), projectilesNum);

        for (int i = 0; i < projectilesNum; i++) {
            Vec3 movementVec = movementVecs.get(i).scale(0.35D);

            Entity shardEntity = new Void_Shard_Entity(level, player,
                    player.getX() + movementVec.x,
                    player.getY() + movementVec.y + 0.25D,
                    player.getZ() + movementVec.z,
                    movementVec, player);
            level.addFreshEntity(shardEntity);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL);
    }

    public List<Vec3> getShootVectors(RandomSource random, float projectilesNum) {
        List<Vec3> vectors = new ArrayList<>();
        float turnFraction = (1.0F + Mth.sqrt(5.0F)) / 2.0F; // golden ratio

        for (int i = 1; i <= projectilesNum; i++) {
            float progress = i / projectilesNum;
            float inclination = (float) Math.acos(1.0F - 0.85F * progress); // vertical position on the sphere
            float azimuth = // horizontal rotation around the sphere
                    (float) ((turnFraction * i + random.nextFloat()) * Math.PI * 2);

            double x = Math.sin(inclination) * Math.cos(azimuth);
            double y = Math.cos(inclination);
            double z = Math.sin(inclination) * Math.sin(azimuth);

            Vec3 vec = new Vec3(x, y, z);

            if (i == 1) {
                vec = vec.add(0.0, 1.0, 0.0).scale(0.5);
            }

            vectors.add(vec);
        }

        return vectors;
    }

    // simple getters & setters

    private static int getAttackBlocks(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.ATTACK_BLOCKS, 0);
    }

    private int getAttackBlocksStat(ItemStack stack) {
        return (int) Math.round(getStatValue(stack, ABILITY_ID, "attack_blocks"));
    }

    private int getProjectilesStat(ItemStack stack) {
        return (int) getStatValue(stack, ABILITY_ID, "projectiles");
    }

    private static void setCooldown(IRelicItem relic, ItemStack stack) {
        relic.setAbilityCooldown(stack, ABILITY_ID, ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }
}
