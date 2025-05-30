package it.hurts.octostudios.reliquified_lenders_cataclysm.mixin;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm.ScouringEyeItem;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import static it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.ScouringEyeUtils.*;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof ScouringEyeItem)
                || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (livingEntity.getUUID().toString().equals(getTargetUUID(stack))
                && ScouringEyeUtils.isGlowingTimeTicking(stack, livingEntity.level())) {
            cir.setReturnValue(true);
        }
    }
}
