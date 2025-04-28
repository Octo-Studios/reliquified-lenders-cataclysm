package it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.back;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.init.RECDataComponentRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.base.RECItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ItemUtils;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.RECMathUtils;
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
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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
                                        .initialValue(0.5D, 0.4D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, -0.0875D)
                                        .formatValue(RECMathUtils::roundThousands)
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
                                .build())
                        .beams(BeamsData.builder()
                                .build())
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == stack.getItem()) {
            return;
        }
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

        System.out.println(stacksEquipped);

        ItemStack stack = stacksEquipped.getFirst();
        Iterator<ItemStack> stacksItr = stacksEquipped.iterator();
        boolean isStackInvalid = stack.isEmpty() || getEnergy(stack) <= 0;

        while (isStackInvalid && stacksItr.hasNext()) {
            stack = stacksItr.next();
        }

        if (isStackInvalid) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();

        player.setDeltaMovement(
                motion.x, ItemUtils.getSpeedStat(stack, ABILITY_ID) * stacksEquipped.size(), motion.z);

        // consume relic energy after each sec of flying
        if (player.tickCount % 20 == 0) {
            consumeEnergy(stack);
        }

        player.displayClientMessage(
                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(getEnergy(stack) + " mB")
                                .withStyle(ChatFormatting.YELLOW)), true);

        if (player.tickCount % 10 == 0) {
            player.playSound(SoundEvents.FIRECHARGE_USE);
        }

        // todo: spawn particles under the player
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.getCommandSenderWorld();
        ItemStack stack = event.getItemStack();

        if (level.isClientSide || !(stack.getItem() instanceof VolcanoItem relic)) {
            return;
        }

        int energy = 0;
        BlockState state = level.getBlockState(event.getPos());

        if (state.is(Blocks.LAVA) || state.getBlock() instanceof LavaCauldronBlock) {
            energy = 1000;
        }

        addEnergy(player, stack, energy);
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

        if (isLavaBucket) {
            addEnergy(player, slotStack, 1000);

            return;
        }

        IFluidHandlerItem fluidHandlerItem = (IFluidHandlerItem) heldItem;

        for (int i = 0; i < fluidHandlerItem.getTanks(); i++) {
            FluidStack fluidStack = fluidHandlerItem.getFluidInTank(i);

            addEnergy(player, slotStack, fluidStack.getAmount());
            fluidStack.setAmount(0);
        }
    }

    private static boolean isItemStoringLava(IFluidHandlerItem item) {
        for (int i = 0; i < item.getTanks(); i++) {
            if (item.getFluidInTank(i).getFluid().equals(Fluids.LAVA)) {
                return true;
            }
        }

        return false;
    }

    private static void consumeEnergy(ItemStack stack) {
        stack.set(RECDataComponentRegistry.ENERGY,
                Math.max(0, getEnergy(stack) - ((VolcanoItem) stack.getItem()).getConsumptionStat(stack)));
    }

    private static void addEnergy(Player player, ItemStack stack, int value) {
        stack.set(RECDataComponentRegistry.ENERGY, Math.min(getCapacityForItem(stack), getEnergy(stack) + value));

        player.playSound(SoundEvents.LAVA_POP);
    }

    private static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(RECDataComponentRegistry.ENERGY, getCapacityForItem(stack));
    }

    private static int getCapacityForItem(ItemStack stack) {
        return ((VolcanoItem) stack.getItem()).getCapacityStat(stack);
    }

    private int getConsumptionStat(ItemStack stack) {
        return (int) getStatValue(stack, ABILITY_ID, "consumption") * 1000;
    }

    private int getCapacityStat(ItemStack stack) {
        return (int) getStatValue(stack, ABILITY_ID, "capacity") * 1000;
    }

//    @Override
//    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
//        return Optional.of(new VolcanoTooltip(getEnergyRounded(stack)));
//    }
//
//    public static class VolcanoTooltip implements TooltipComponent {
//        public final MutableComponent tooltipFirst =
//                Component.translatable("tooltip.reliquified_lenders_cataclysm.volcano");
//        public final MutableComponent tooltipSecond;
//
//        public VolcanoTooltip(String tooltipInput) {
//            this.tooltipSecond = Component.literal(tooltipInput + " mB").withStyle(ChatFormatting.YELLOW);
//        }
//
//        public MutableComponent get() {
//            return tooltipFirst.withStyle(ChatFormatting.GOLD).append(tooltipSecond);
//        }
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    public static class VolcanoClientTooltip implements ClientTooltipComponent {
//        private final VolcanoTooltip tooltip;
//
//        public VolcanoClientTooltip(VolcanoTooltip tooltip) {
//            this.tooltip = tooltip;
//        }
//
//        @Override
//        public int getHeight() {
//            return 10;
//        }
//
//        @Override
//        public int getWidth(@NotNull Font font) {
//            return 100;
//        }
//
//        @Override
//        public void renderText(@NotNull Font font, int x, int y,
//                               @NotNull Matrix4f mx, MultiBufferSource.@NotNull BufferSource buffer) {
//            font.drawInBatch(tooltip.get(), x, y, 0xFFFFFF, false, mx, buffer,
//                    Font.DisplayMode.NORMAL, 0, (int) (LightTexture.pack(15, 15)));
//        }
//    }
}
