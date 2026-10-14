package dev.rylex.nep.compat.malum;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SpiritReclaimWrap {

    private SpiritReclaimWrap() {}

    public static boolean addFreshEntity(
            BlockEntity host, boolean keeping, ServerLevel level, Entity entity, Operation<Boolean> original) {
        if (entity instanceof ItemEntity item) {
            ItemStack remaining = keeping
                    ? SpiritReclaimer.reclaimKeeping(host, item.getItem())
                    : SpiritReclaimer.reclaim(host, item.getItem());
            if (remaining.isEmpty()) {
                return true;
            }
            item.setItem(remaining);
        }
        return original.call(level, entity);
    }
}
