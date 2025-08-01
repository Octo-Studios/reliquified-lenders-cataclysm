package it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.ring_of_the_flame_kindler;

import com.github.L_Ender.cataclysm.entity.projectile.Flame_Jet_Entity;
import it.hurts.octostudios.reliquified_lenders_cataclysm.network.packets.server.FlameJetSpawnPacket;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class FlameJetModifiedEntity extends Flame_Jet_Entity {
    private int lifeTicks;
    private boolean clientSideAttackStarted = false;

    public FlameJetModifiedEntity(EntityType<? extends Flame_Jet_Entity> type, Level level) {
        super(type, level);
    }

    public FlameJetModifiedEntity(Level level, double x, double y, double z, float yRot,
                                  int warmupDelayTicks, float damage, LivingEntity caster) {
        super(level, x, y, z, yRot, warmupDelayTicks, damage, caster);

        this.lifeTicks = 22;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            if (clientSideAttackStarted) {
                --lifeTicks;

                if (lifeTicks == 14) {
                    NetworkHandler.sendToServer(new FlameJetSpawnPacket(getX(), getY(), getZ()));
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);

        if (id == 4) {
            this.clientSideAttackStarted = true;
        }
    }
}
