package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import com.github.L_Ender.cataclysm.init.ModItems;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.client.VolcanoParticlesPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server.VolcanoOperationPacket;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math.RECMathUtils;
import it.hurts.sskirillss.relics.api.events.common.ContainerSlotClickEvent;
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
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import top.theillusivec4.curios.api.SlotContext;

import java.util.Iterator;
import java.util.List;

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
                                        .formatValue(RECMathUtils::roundOneDigit)
                                        .build())
                                .stat(StatData.builder("consumption")
                                        .initialValue(4D, 3D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.0833D)
                                        .formatValue(RECMathUtils::roundHundreds)
                                        .build())
                                .stat(StatData.builder("capacity")
                                        .initialValue(3D, 4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4D)
                                        .formatValue(RECMathUtils::roundThousands)
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
                                .borderTop(0xFFFFDF52)
                                .borderBottom(0xFFD87F12)
                                .textured(true)
                                .build())
                        .beams(BeamsData.builder()
                                .startColor(0xFFCA7C00)
                                .endColor(0x00713A44)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext tooltip,
                                @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, tooltip, components, flag);

        int energy = VolcanoItem.getEnergy(stack);
        int capacity = VolcanoItem.getCapacityForItem(stack);
        ChatFormatting energyColor = ChatFormatting.YELLOW;

        if (energy == capacity) {
            energyColor = ChatFormatting.GREEN;
        } else if (energy <= capacity * 0.1) {
            energyColor = ChatFormatting.RED;
        }

        components.add(
                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(energy + " mB").withStyle(energyColor)));

        components.add(Component.empty());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        if (entity.getCommandSenderWorld().isClientSide) {
            return;
        }

        if (isToggled(stack)) {
            NetworkHandler.sendToClientsTrackingEntityAndSelf(new VolcanoParticlesPacket(entity.getId()), entity);
        }

        int energy = getEnergy(stack);
        int capacity = getCapacityForItem(stack);

        if (energy > capacity) {
            addEnergy(entity, stack, energy - capacity);
        }
    }

    @SubscribeEvent
    public static void onPlayerKeyUse(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) {
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

        if (mc.screen != null || event.getAction() != 2
                || event.getKey() != mc.options.keyJump.getKey().getValue()) {
            // toggled = false
            NetworkHandler.sendToServer(new VolcanoOperationPacket(stacksEquipped.indexOf(stack), 0, false));

            return;
        }

        Vec3 motion = player.getDeltaMovement();

        player.setDeltaMovement(
                motion.x, ItemUtils.getSpeedStat(stack, ABILITY_ID) * stacksEquipped.size(), motion.z);
        // toggled = true
        NetworkHandler.sendToServer(new VolcanoOperationPacket(stacksEquipped.indexOf(stack), 0, true));

        // consume relic energy after each sec of flying
        if (player.tickCount % 20 == 0) {
            NetworkHandler.sendToServer(new VolcanoOperationPacket(stacksEquipped.indexOf(stack), 1, true));

            player.playSound(SoundEvents.FIRECHARGE_USE);
        }

        if (player.tickCount % 100 == 0) {
            // spread exp
            NetworkHandler.sendToServer(new VolcanoOperationPacket(stacksEquipped.indexOf(stack), 2, true));
        }

        // display amount of remaining lava above hotbar
//        player.displayClientMessage(
//                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano").withStyle(ChatFormatting.GOLD)
//                        .append(Component.literal(getEnergy(stack) + " mB").withStyle(ChatFormatting.YELLOW)), true);
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
        boolean isPowerCell = heldItem.equals(ModItems.LAVA_POWER_CELL.get());

        if (!(slotStack.getItem() instanceof VolcanoItem)
                || !(isLavaBucket || isPowerCell || (heldItem instanceof IFluidHandlerItem fluidHandlerItem
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

        if (isPowerCell && addEnergy(player, slotStack, 5000)) {
            heldStack.shrink(1);
            event.setCanceled(true);

            return;
        }

        if (!(heldItem instanceof IFluidHandlerItem fluidHandlerItem)) {
            return;
        }

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

    private static boolean addEnergy(LivingEntity entity, ItemStack stack, int value) {
        int capacity = getCapacityForItem(stack), energy = getEnergy(stack);

        if (capacity <= energy) {
            return false;
        }

        stack.set(RECDataComponentRegistry.VOLCANO_ENERGY, Math.min(capacity, energy + value));

        entity.playSound(SoundEvents.LAVA_POP);

        return true;
    }

    public static void setToggled(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.TOGGLED, value);
    }

    public static boolean isToggled(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TOGGLED, false);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.VOLCANO_ENERGY, getCapacityForItem(stack));
    }

    private static int getCapacityForItem(ItemStack stack) {
        return ((VolcanoItem) stack.getItem()).getCapacityStat(stack);
    }

    public int getConsumptionStat(ItemStack stack) {
        return (int) (getStatValue(stack, ABILITY_ID, "consumption") * 100);
    }

    private int getCapacityStat(ItemStack stack) {
        return (int) (MathUtils.round(getStatValue(stack, ABILITY_ID, "capacity"), 0) * 1000);
    }
}
