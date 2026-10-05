package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public final class ArcaneLecterns {
    private ArcaneLecterns() {}

    private static final Map<Level, Set<ArcaneLecternBlockEntity>> LOADED = new WeakHashMap<>();

    static void add(ArcaneLecternBlockEntity lectern) {
        Level level = lectern.getLevel();
        if (level != null && !level.isClientSide()) {
            LOADED.computeIfAbsent(level, key -> Collections.newSetFromMap(new WeakHashMap<>()))
                    .add(lectern);
        }
    }

    static void remove(ArcaneLecternBlockEntity lectern) {
        Set<ArcaneLecternBlockEntity> lecterns = LOADED.get(lectern.getLevel());
        if (lecterns != null) {
            lecterns.remove(lectern);
        }
    }

    /** Returns true when the table is either already supplied or has nothing left to ask for. */
    public static boolean supply(ScribesTile table) {
        Level level = table.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        List<Ingredient> remaining = new ArrayList<>(table.getRemainingRequired());
        if (remaining.isEmpty()) {
            return true;
        }
        if (!NepConfig.arsArcaneLectern()) {
            return false;
        }
        Set<ArcaneLecternBlockEntity> lecterns = LOADED.get(level);
        if (lecterns == null || lecterns.isEmpty()) {
            return false;
        }
        BlockPos tablePos = table.getBlockPos();
        boolean delivered = false;
        for (ArcaneLecternBlockEntity lectern : List.copyOf(lecterns)) {
            if (lectern.isRemoved() || !within(lectern.getBlockPos(), tablePos)) {
                continue;
            }
            for (var it = remaining.iterator(); it.hasNext(); ) {
                ItemStack taken = lectern.takeOne(it.next(), table::canConsumeItemstack);
                if (taken.isEmpty()) {
                    continue;
                }
                table.consumedStacks.add(taken);
                level.addFreshEntity(new EntityFlyingItem(level, lectern.getBlockPos(), tablePos).setStack(taken));
                delivered = true;
                it.remove();
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        if (delivered) {
            table.updateBlock();
        }
        return delivered;
    }

    private static boolean within(BlockPos lectern, BlockPos table) {
        int range = NepConfig.arsArcaneLecternScribesRange();
        return Math.abs(lectern.getX() - table.getX()) <= range
                && Math.abs(lectern.getY() - table.getY()) <= range
                && Math.abs(lectern.getZ() - table.getZ()) <= range;
    }
}
