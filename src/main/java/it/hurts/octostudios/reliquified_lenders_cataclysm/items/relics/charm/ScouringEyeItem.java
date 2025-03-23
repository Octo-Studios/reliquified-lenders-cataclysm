package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.PredicateType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.*;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemColor;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.GemShape;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.style.BeamsData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@EventBusSubscriber
public class ScouringEyeItem extends RelicItem {
    private static final String ABILITY_ID = "glowing_scour";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .active(CastData.builder()
                                        .type(CastType.INSTANTANEOUS)
                                        .predicate("target", PredicateType.CAST,
                                                (player, stack) -> !getTargetUUID(stack).isEmpty())
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(20D, 15D)
                                        .upgradeModifier(UpgradeOperation.ADD, -1D)
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

    /**
     * Ability {@code glowing_scour} <b>[active]</b>: tp player to the attacked entity, then set the cooldown
     */
    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (!ability.equals(ABILITY_ID)) {
            return;
        }

        Level level = player.getCommandSenderWorld();

        if (level.isClientSide) {
            return;
        }

        LivingEntity target = getEntityFromStack(level, stack);

        if (target == null) {
            setTargetUUID(stack, ""); // if entity's not found, clear data
            return;
        }

        teleportToTarget(player, target);
        setAbilityCooldown(stack, ABILITY_ID, ItemUtils.getCooldownStat(stack, ABILITY_ID));
    }

    private static void teleportToTarget(Player player, LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        Direction targetDirection = target.getNearestViewDirection();

        // tp behind target's view direction
        if (targetDirection.getAxis().equals(Direction.Axis.X)) {
            x += getBlocksBehindDirection(targetDirection);
        } else {
            z += getBlocksBehindDirection(targetDirection);
        }

        player.teleportTo(x, y, z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        player.getCommandSenderWorld().playSound(null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static float getBlocksBehindDirection(Direction targetDirection) {
        return targetDirection.getAxisDirection().equals(Direction.AxisDirection.NEGATIVE)
                ? 2.0F : -2.0F;
    }

    /**
     * Ability {@code glowing_scour} <b>[active]</b>:
     * write data of the attacked entity to the relic, then start glowing the entity on client
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        Level level = player.getCommandSenderWorld();

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());

        if (stack.isEmpty()
                || !(target instanceof LivingEntity livingTarget) || livingTarget.isDeadOrDying()
                || level.isClientSide) {
            return;
        }

        setTargetUUID(stack, livingTarget.getUUID().toString());
    }

    @Nullable
    private LivingEntity getEntityFromStack(Level level, ItemStack stack) {
        String uuid = getTargetUUID(stack);

        if (uuid.isEmpty() || level.isClientSide) {
            return null;
        }

        Entity entity = ((ServerLevel) level).getEntity(UUID.fromString(uuid));

        if (entity instanceof LivingEntity livingEntity && !(livingEntity.isDeadOrDying())) {
            return livingEntity;
        }

        return null;
    }

    public String getTargetUUID(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.TARGET_UUID, "");
    }

    private static void setTargetUUID(ItemStack stack, String value) {
        stack.set(RECDataComponentRegistry.TARGET_UUID, value);
    }
}
