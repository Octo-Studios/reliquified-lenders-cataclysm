package it.hurts.octostudios.reliquified_lenders_cataclysm.mixin;

import it.hurts.octostudios.reliquified_lenders_cataclysm.init.ItemRegistry;
import it.hurts.octostudios.reliquified_lenders_cataclysm.items.relics.charm.ScouringEyeItem;
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

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.SCOURING_EYE.get());

        if (stack.isEmpty() || !(stack.getItem() instanceof ScouringEyeItem relic)) {
            return;
        }

        if (entity instanceof LivingEntity livingEntity
                && livingEntity.getUUID().toString().equals(relic.getTargetUUID(stack))) {
            cir.setReturnValue(true);
        }
    }
}
