package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModSounds;
import it.hurts.octostudios.reliquified_lenders_cataclysm.utils.relics.VoidMantleUtils;
import lombok.Getter;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Getter
public class ScreenShakeSoundedEntity extends ScreenShake_Entity {
    private int layersSpawned = 0;

    public ScreenShakeSoundedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public ScreenShakeSoundedEntity(Level level, Vec3 pos, int radius,
                                    int layersSpawned, int wavesNum, int fadeDuration) {
        super(level, pos, radius, 1.0F / wavesNum,
                wavesNum * VoidMantleUtils.getWaveTicks(), fadeDuration);

        this.layersSpawned = layersSpawned;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide || tickCount > getDuration()) {
            this.discard();
        }

        for (int i = 0; i < getLayersSpawned(); i++) {
            int fangLifespan = 30;
            int tickCountDecreased = tickCount - fangLifespan * i; // "pure" ticks count

            // set an interval to imitate the sound of each layer of fangs (3 ticks so far)
            if ((tickCount % 3 == 0 && tickCountDecreased >= 1 && tickCountDecreased <= (getRadius() - 1) * 3)
                    || tickCount == 1) {
                level().playSound(null, this.blockPosition(),
                        ModSounds.VOID_RUNE_RISING.get(), SoundSource.NEUTRAL, 1F, 1F);

                break;
            }
        }
    }
}
