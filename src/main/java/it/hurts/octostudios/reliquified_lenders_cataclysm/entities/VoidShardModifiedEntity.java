package it.hurts.octostudios.reliquified_lenders_cataclysm.entities;

import com.github.L_Ender.cataclysm.entity.projectile.Void_Shard_Entity;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VoidShardModifiedEntity extends Void_Shard_Entity {
    private float damage = 1.0F;

    public VoidShardModifiedEntity(EntityType<? extends Void_Shard_Entity> type, Level level) {
        super(type, level);
    }

    public VoidShardModifiedEntity(Level level, LivingEntity caster, double x, double y, double z,
                                   Vec3 movement, @Nullable Entity ignore, float damage) {
        super(level, caster, x, y, z, movement, ignore);

        this.damage = damage;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity shooter = this.getOwner();
        Entity entity = result.getEntity();

        if (shooter == null) {
            entity.hurt(this.damageSources().magic(), damage);
            entity.invulnerableTime = 0;
        } else if (entity != shooter && !EntityUtils.isAlliedTo(shooter, entity)) {
            entity.hurt(this.damageSources().indirectMagic(this, this.getOwner()), damage);
            entity.invulnerableTime = 0;
        }
    }
}
