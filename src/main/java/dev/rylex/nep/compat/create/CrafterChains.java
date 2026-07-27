package dev.rylex.nep.compat.create;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import dev.rylex.nep.pattern.GridPos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.createmod.catnip.math.Pointing;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class CrafterChains {
    private CrafterChains() {}

    record Chain(
            List<MechanicalCrafterBlockEntity> crafters,
            Map<MechanicalCrafterBlockEntity, GridPos> positions,
            Map<MechanicalCrafterBlockEntity, Integer> depths,
            MechanicalCrafterBlockEntity output) {}

    private record CachedChain(long tick, @Nullable Chain chain) {}

    private static final Map<MechanicalCrafterBlockEntity, CachedChain> CACHE = new WeakHashMap<>();

    @Nullable
    static Chain of(MechanicalCrafterBlockEntity start) {
        Level level = start.getLevel();
        if (level == null) {
            return compute(start);
        }
        long now = level.getGameTime();
        CachedChain cached = CACHE.get(start);
        if (cached != null && cached.tick() == now) {
            return cached.chain();
        }
        Chain chain = compute(start);
        CACHE.put(start, new CachedChain(now, chain));
        return chain;
    }

    @Nullable
    private static Chain compute(MechanicalCrafterBlockEntity start) {
        List<MechanicalCrafterBlockEntity> crafters = RecipeGridHandler.getAllCraftersOfChainIf(start, c -> true);
        if (crafters == null || crafters.isEmpty()) {
            return null;
        }

        MechanicalCrafterBlockEntity output = null;
        Map<MechanicalCrafterBlockEntity, MechanicalCrafterBlockEntity> targets = new HashMap<>();
        for (MechanicalCrafterBlockEntity crafter : crafters) {
            MechanicalCrafterBlockEntity target = RecipeGridHandler.getTargetingCrafter(crafter);
            if (target == null) {
                if (output != null) {
                    return null;
                }
                output = crafter;
            } else {
                targets.put(crafter, target);
            }
        }
        if (output == null) {
            return null;
        }

        Map<MechanicalCrafterBlockEntity, GridPos> positions = new HashMap<>();
        Map<MechanicalCrafterBlockEntity, Integer> depths = new HashMap<>();
        positions.put(output, new GridPos(0, 0));
        depths.put(output, 0);
        boolean progress = true;
        while (positions.size() < crafters.size() && progress) {
            progress = false;
            for (MechanicalCrafterBlockEntity crafter : crafters) {
                if (positions.containsKey(crafter)) {
                    continue;
                }
                MechanicalCrafterBlockEntity target = targets.get(crafter);
                GridPos targetPos = positions.get(target);
                if (targetPos == null) {
                    continue;
                }
                Pointing pointing = pointing(crafter);
                positions.put(crafter, targetPos.offset(dx(pointing), dy(pointing)));
                depths.put(crafter, depths.get(target) + 1);
                progress = true;
            }
        }
        if (positions.size() != crafters.size()) {
            return null;
        }
        return new Chain(crafters, positions, depths, output);
    }

    static Pointing pointing(MechanicalCrafterBlockEntity crafter) {
        return crafter.getBlockState().getValue(MechanicalCrafterBlock.POINTING);
    }

    static int dx(Pointing pointing) {
        return pointing == Pointing.LEFT ? 1 : pointing == Pointing.RIGHT ? -1 : 0;
    }

    static int dy(Pointing pointing) {
        return pointing == Pointing.DOWN ? 1 : pointing == Pointing.UP ? -1 : 0;
    }

    static boolean isCovered(MechanicalCrafterBlockEntity crafter) {
        return crafter.craftingItemOrCoverPresent() && !crafter.craftingItemPresent();
    }
}
