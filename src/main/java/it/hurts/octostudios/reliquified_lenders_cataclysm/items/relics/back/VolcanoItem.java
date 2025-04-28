package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server.VolcanoEnergyPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathUtils;
import it.hurts.sskirillss.relics.api.events.common.ContainerSlotClickEvent;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.MathRandomUtils.*;

@EventBusSubscriber
public class VolcanoItem extends RECItem {
    public static final String ABILITY_ID = "jetpack";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder(ABILITY_ID)
                                .stat(StatData.builder("speed")
                                        .initialValue(2D, 2.5D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.18D)
                                        .formatValue(MathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("consumption")
                                        .initialValue(0.5D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.0875D)
                                        .formatValue(MathUtils::roundThousands)
                                        .build())
                                .stat(StatData.builder("capacity")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(MathUtils::roundThousands)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(100)
                        .step(100)
                        .maxLevel(15)
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
                                .build())
                        .beams(BeamsData.builder()
                                .build())
                        .build())
                .build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext tooltip,
                                @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, tooltip, components, flag);

        components.add(
                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(VolcanoItem.getEnergy(stack) + " mB").withStyle(ChatFormatting.YELLOW)));

        components.add(Component.empty());
    }

    @SubscribeEvent
    public static void onPlayerKeyUse(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.screen != null || event.getAction() != 2
                || event.getKey() != mc.options.keyJump.getKey().getValue()) {
            return;
        }

        List<ItemStack> stacksEquipped = EntityUtils.findEquippedCurios(player, ItemRegistry.VOLCANO.get());

        if (stacksEquipped.isEmpty()) {
            return;
        }

        ItemStack stack = stacksEquipped.getFirst();
        Iterator<ItemStack> stacksIterator = stacksEquipped.iterator();

        while (!isStackValid(stack) && stacksIterator.hasNext()) {
            stack = stacksIterator.next();

            if (isStackValid(stack)) {
                break;
            }
        }

        if (!isStackValid(stack)) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();

        player.setDeltaMovement(
                motion.x, ItemUtils.getSpeedStat(stack, ABILITY_ID) * stacksEquipped.size(), motion.z);

        // consume relic energy after each sec of flying
        if (player.tickCount % 20 == 0) {
            NetworkHandler.sendToServer(new VolcanoEnergyPacket(stacksEquipped.indexOf(stack)));

            player.playSound(SoundEvents.FIRECHARGE_USE);
        }

        if (player.tickCount % 100 == 0) {
            ((VolcanoItem) stack.getItem()).spreadRelicExperience(player, stack, 1);
        }

        // display amount of remaining lava above hotbar
//        player.displayClientMessage(
//                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano").withStyle(ChatFormatting.GOLD)
//                        .append(Component.literal(getEnergy(stack) + " mB").withStyle(ChatFormatting.YELLOW)), true);

        spawnVolcanoParticles(player);
    }

    private static void spawnVolcanoParticles(Player player) {
        RandomSource random = player.getRandom();
        Color color = new Color(255, 255 * (int) random.nextFloat(), 0); // somewhere between red & yellow

        player.getCommandSenderWorld()
                .addParticle(ParticleUtils.constructSimpleSpark(color,
                0.4F,
                20 + randomized(random, 10),
                0.8F + randomized(random, 0.1F)),
                        player.getX() + randomized(random, 0.5D),
                        player.getY() + 0.1D,
                        player.getZ() + randomized(random, 0.5D),
                        0, 0, 0);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.getCommandSenderWorld();
        ItemStack stack = event.getItemStack();

        if (level.isClientSide || !(stack.getItem() instanceof VolcanoItem) || !player.isCrouching()) {
            return;
        }

        HitResult hitResult = player.pick(8D, 0F, false);

        if (!hitResult.getType().equals(HitResult.Type.BLOCK)) {
            return;
        }

        BlockPos lavaPos = getLavaPos(level, (BlockHitResult) hitResult);

        if (lavaPos != null) {
            if (addEnergy(player, stack, 1000)) {
                Block blockNew = Blocks.AIR;

                if (level.getBlockState(lavaPos).getBlock() instanceof LavaCauldronBlock) {
                    blockNew = Blocks.CAULDRON;
                }

                level.setBlock(lavaPos, blockNew.defaultBlockState(), Block.UPDATE_ALL);

                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);

                level.playSound(null, player.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS);
            }
        }
    }

    @Nullable
    private static BlockPos getLavaPos(Level level, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        List<BlockPos> blockPoses =
                List.of(blockPos, blockPos.above(), blockPos.below(), blockPos.east(), blockPos.west());

        for (BlockPos blockPosCurrent : blockPoses) {
            if (isLavaContainerBlock(level, blockPosCurrent)) {
                return blockPosCurrent;
            }
        }

        return null;
    }

    private static boolean isLavaContainerBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.LAVA) || state.getFluidState().is(Fluids.LAVA)
                || state.getBlock() instanceof LavaCauldronBlock;
    }

    @SubscribeEvent
    public static void onContainerSlotClick(ContainerSlotClickEvent event) {
        if (!event.getAction().equals(ClickAction.SECONDARY)) {
            return;
        }

        Player player = event.getEntity();

        ItemStack slotStack = event.getSlotStack();
        ItemStack heldStack = event.getHeldStack();
        Item heldItem = heldStack.getItem();
        boolean isLavaBucket = heldItem.equals(Items.LAVA_BUCKET);

        if (!(slotStack.getItem() instanceof VolcanoItem)
                || !(isLavaBucket || (heldItem instanceof IFluidHandlerItem fluidHandlerItem
                && isItemStoringLava(fluidHandlerItem)))) {
            return;
        }

        if (isLavaBucket && addEnergy(player, slotStack, 1000)) {
            heldStack.shrink(1);

            ItemStack stackNew = new ItemStack(Items.BUCKET);

            if (!player.getInventory().add(stackNew)) {
                player.drop(stackNew, false);

                return;
            }

            event.setCanceled(true);

            return;
        }

        IFluidHandlerItem fluidHandlerItem = (IFluidHandlerItem) heldItem;

        for (int i = 0; i < fluidHandlerItem.getTanks(); i++) {
            FluidStack fluidStack = fluidHandlerItem.getFluidInTank(i);

            if (addEnergy(player, slotStack, fluidStack.getAmount())) {
                fluidStack.setAmount(0);
            }
        }
    }

    // getters & setters

    private static boolean isStackValid(ItemStack stack) {
        return !stack.isEmpty() && getEnergy(stack) > 0;
    }

    private static boolean isItemStoringLava(IFluidHandlerItem item) {
        for (int i = 0; i < item.getTanks(); i++) {
            if (item.getFluidInTank(i).getFluid().equals(Fluids.LAVA)) {
                return true;
            }
        }

        return false;
    }

    private static boolean addEnergy(Player player, ItemStack stack, int value) {
        int capacity = getCapacityForItem(stack), energy = getEnergy(stack);

        if (capacity <= energy) {
            return false;
        }

        stack.set(RECDataComponentRegistry.VOLCANO_ENERGY,
                Math.min(capacity, energy + value));

        player.playSound(SoundEvents.LAVA_POP);

        return true;
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.VOLCANO_ENERGY, getCapacityForItem(stack));
    }

    private static int getCapacityForItem(ItemStack stack) {
        return ((VolcanoItem) stack.getItem()).getCapacityStat(stack);
    }

    public int getConsumptionStat(ItemStack stack) {
        return (int) (getStatValue(stack, ABILITY_ID, "consumption") * 1000);
    }

    private int getCapacityStat(ItemStack stack) {
        return (int) (getStatValue(stack, ABILITY_ID, "capacity") * 1000);
    }
}
