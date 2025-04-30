package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import com.github.L_Ender.cataclysm.entity.projectile.Blazing_Bone_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.IgnitedShieldEntity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber
public class FirePlateItem extends RECItem {
    private static final String ABILITY_ID = "spawn_shield";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("health")
                                        .initialValue(4D, 6D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.233D)
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("regeneration_time")
                                        .initialValue(60D, 55D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.0722D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("projectiles")
                                        .initialValue(8D, 10D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.22D)
                                        .formatValue(RECMathUtils::roundInt)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.65D)
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
                                        .gem(GemShape.SQUARE, GemColor.ORANGE)
                                        .build())
                                .build())
                        .build())
                .loot(LootData.builder()
                        .entry(LootEntries.THE_NETHER)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .borderTop(0xFFFDF878)
                                .borderBottom(0xFFF0BA36)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFDD6732)
                                .endColor(0x00A4512C)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        int cooldownTicks = getCooldown(stack);

        if (cooldownTicks == 1) {
            ItemUtils.playCooldownSound(level, entity);
        }

        if (cooldownTicks > 0) {
            setCooldown(stack, cooldownTicks - 1);

            return;
        }

        IgnitedShieldEntity shield = getShield(level, stack);

        // shield spawn
        if (shield == null) {
            IgnitedShieldEntity shieldNew =
                    new IgnitedShieldEntity(level, entity, entity.position(), getHealthStat(stack));

            level.addFreshEntity(shieldNew);

            setShieldUUID(stack, shieldNew.getUUID().toString());
            setCooldown(stack, 0);
            setTime(stack, 0);

            return;
        }

        float health = shield.getHealth();
        int ticks = getTime(stack);

        // shield regen (once for N sec)
        if (ticks != 0 && ticks % getRegenTimeStat(stack) == 0) {
            if (health > 0F && health < getHealthStat(stack)) {
                shield.setHealth(getHealthStat(stack));

                playShieldSound(shield, SoundEvents.EXPERIENCE_ORB_PICKUP);
            }
        }

        // shield death
        if (shield.getHealth() <= 0F) {
            playShieldSound(shield, SoundEvents.DROWNED_SHOOT);

            int projectilesNum = getProjectilesStat(stack);

            // spawn projectiles
            for (int i = 0; i < projectilesNum; i++) {
                Blazing_Bone_Entity projectile = new Blazing_Bone_Entity(level, getDamageStat(stack), entity);

                float yawRadians = (float) Math.toRadians(shield.getYRot() + 90F);
                float throwAngle = (float) (yawRadians + i * Math.PI * 2 / projectilesNum);
                double motionX = shield.getX() + Mth.cos(throwAngle);
                double motionY = shield.getY() + shield.getBbHeight() * 0.62D;
                double motionZ = shield.getZ() + Mth.sin(throwAngle);

                projectile.moveTo(motionX, motionY, motionZ, i * 360F / projectilesNum, shield.getXRot());

                // speed = 0.5F
                projectile.shoot(Mth.cos(throwAngle), 0D, Mth.sin(throwAngle), 0.5F, 1.0F);

                level.addFreshEntity(projectile);
            }

            setCooldown(stack, 1200); // default cd = 60 sec
            setTime(stack, 0);
            setShieldUUID(stack, "");

            shield.remove(Entity.RemovalReason.DISCARDED);

            playShieldSound(shield, SoundEvents.ITEM_BREAK);

            return;
        }

        setTime(stack, getTime(stack) + 1);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.getCommandSenderWorld();

        if (stack.equals(newStack)) {
            IgnitedShieldEntity shield = getShield(level, stack);

            if (shield != null) {
                shield.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        if (level.isClientSide || newStack.getItem() == stack.getItem()) {
            return;
        }

        IgnitedShieldEntity shield = getShield(level, stack);

        if (shield != null) {
            shield.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        List<IgnitedShieldEntity> shieldsAround =
                level.getEntities(player, new AABB(player.blockPosition()).inflate(4D)).stream()
                        .map(entity -> entity instanceof IgnitedShieldEntity shield && shield.getOwner() != null
                                && shield.getOwner().equals(player) ? shield : null)
                        .filter(Objects::nonNull).toList();
        List<ItemStack> stacksEquipped = EntityUtils.findEquippedCurios(player, ItemRegistry.FIRE_PLATE.get());

        for (IgnitedShieldEntity shield : shieldsAround) {
            boolean isShieldBounded = false;

            for (ItemStack stack : stacksEquipped) {
                IgnitedShieldEntity shieldOther = ((FirePlateItem) stack.getItem()).getShield(level, stack);

                if (shieldOther != null && shieldOther.equals(shield)) {
                    isShieldBounded = true;

                    break;
                }
            }

            if (!isShieldBounded) {
                shield.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        for (ItemStack stack : EntityUtils.findEquippedCurios(entity, ItemRegistry.FIRE_PLATE.get())) {
            if (stack.isEmpty()) {
                continue;
            }

            FirePlateItem relic = ((FirePlateItem) stack.getItem());
            IgnitedShieldEntity shield = relic.getShield(level, stack);

            if (shield == null || relic.getCooldown(stack) > 0
                    || !(event.getSource().getDirectEntity() instanceof LivingEntity sourceEntity)) {
                continue;
            }

            Vec3 entityPos = entity.position();
            Vec3 directionVector = shield.position().subtract(entityPos).normalize();
            double dotProduct = sourceEntity.position().subtract(entityPos).normalize().dot(directionVector);

            // block the attack if it was from the shield side
            if (dotProduct > 0.0D) {
                shield.setHealth(shield.getHealth() - 1F);
                event.setCanceled(true);

                relic.playShieldSound(shield, SoundEvents.SHIELD_BLOCK);

                relic.spreadRelicExperience(entity, stack, 1);
            }
        }
    }

    private void playShieldSound(IgnitedShieldEntity shield, SoundEvent soundEvent) {
        shield.getCommandSenderWorld().playSound(null, shield.blockPosition(),
                soundEvent, SoundSource.NEUTRAL);
    }

    @Nullable
    private IgnitedShieldEntity getShield(Level level, ItemStack stack) {
        String shieldUUID = getShieldUUID(stack);

        if (!shieldUUID.isEmpty() && (((ServerLevel) level).getEntity(UUID.fromString(shieldUUID))
                instanceof IgnitedShieldEntity shield)) {
            return shield;
        }

        return null;
    }

    private String getShieldUUID(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.SHIELD_UUID, "");
    }

    private void setShieldUUID(ItemStack stack, String shieldUUID) {
        stack.set(RECDataComponentRegistry.SHIELD_UUID, shieldUUID);
    }

    private int getTime(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TIME, 0);
    }

    private void setTime(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.TIME, value);
    }

    private int getCooldown(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.COOLDOWN, 0);
    }

    private void setCooldown(ItemStack stack, int value) {
        stack.set(RECDataComponentRegistry.COOLDOWN, value);
    }

    private int getRegenTimeStat(ItemStack stack) {
        return ItemUtils.getIntStat(stack, ABILITY_ID, "regeneration_time") * 20;
    }

    private static float getHealthStat(ItemStack stack) {
        return (float) (((FirePlateItem) stack.getItem()).getStatValue(stack, ABILITY_ID, "health"));
    }

    private int getProjectilesStat(ItemStack stack) {
        return ItemUtils.getIntStat(stack, ABILITY_ID, "projectiles");
    }

    private static float getDamageStat(ItemStack stack) {
        return (float) (((FirePlateItem) stack.getItem()).getStatValue(stack, ABILITY_ID, "damage"));
    }
}
