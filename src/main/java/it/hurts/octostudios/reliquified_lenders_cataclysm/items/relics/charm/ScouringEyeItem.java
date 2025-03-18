package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.GlowingEffectPacket;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

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
                                        .predicate("target", PredicateType.CAST, (player, stack) -> {
                                            int entityId = stack.getOrDefault(RECDataComponentRegistry.TARGET_ID.get(), -1);
                                            Entity entity = player.getCommandSenderWorld()
                                                    .getEntity(entityId);

                                            return entityId != -1 && entity instanceof LivingEntity;
                                        })
                                        .build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(15, 20)
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
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        if (ability.equals(ABILITY_ID)) {
            int entityId = stack.getOrDefault(RECDataComponentRegistry.TARGET_ID.get(), -1);
            Entity target = player.getCommandSenderWorld().getEntity(entityId);

            if (!(target instanceof LivingEntity livingTarget)) {
                return;
            }

            teleportToTarget(player, livingTarget);

            setAbilityCooldown(stack, ABILITY_ID, getCooldownStat(stack));
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());

        if (!(target instanceof LivingEntity livingTarget)
                || livingTarget.isDeadOrDying() || stack.isEmpty()) {
            return;
        }

        int livingTargetId = livingTarget.getId();

        stack.set(RECDataComponentRegistry.TARGET_ID.get(), livingTargetId);

        // no effect
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClient(new GlowingEffectPacket(livingTargetId), serverPlayer);
        }
    }

    private static void teleportToTarget(Player player, LivingEntity target) {
        double x = target.position().x;
        double y = target.position().y;
        double z = target.position().z;

        Direction targetDirection = target.getNearestViewDirection();

        // tp behind target's view direction
        if (targetDirection.getAxis().equals(Direction.Axis.X)) {
            x += getBlockBehindDirection(targetDirection);
        } else {
            z += getBlockBehindDirection(targetDirection);
        }

        player.teleportTo(x, y, z);
        player.getCommandSenderWorld().playSound(null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static float getBlockBehindDirection(Direction targetDirection) {
        return targetDirection.getAxisDirection().equals(Direction.AxisDirection.NEGATIVE)
                ? 2.0F : -2.0F;
    }

    private int getCooldownStat(ItemStack stack) {
        return (int) (getStatValue(stack, ABILITY_ID, "cooldown") * 20);
    }
}
