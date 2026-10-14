package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.systems.blockentity.LodestoneBlockEntityInventory;

abstract class MemoizedPushMachine implements ICraftingMachine {
    private static final int MAX_MEMOIZED_REJECTS = 512;

    static final class Memo {
        private final Set<AEItemKey> unsatisfiable = new HashSet<>();
        private final RejectLog rejectLog = new RejectLog();

        void clear() {
            unsatisfiable.clear();
            rejectLog.clear();
        }
    }

    private final BlockEntity machine;
    private final Memo memo;
    private final String name;

    MemoizedPushMachine(BlockEntity machine, Memo memo, String name) {
        this.machine = machine;
        this.memo = memo;
        this.name = name;
    }

    abstract boolean enabled();

    abstract PushOutcome attempt(
            Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection);

    @Override
    public boolean acceptsPlans() {
        return enabled();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide() || !enabled()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (memo.unsatisfiable.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs, ejectionDirection);
        if (outcome.unsatisfiable() && memo.unsatisfiable.size() < MAX_MEMOIZED_REJECTS) {
            memo.unsatisfiable.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    @Nullable
    static Map<AEItemKey, Long> collectItems(KeyCounter[] inputs) {
        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return null;
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        return provided;
    }

    static boolean isEmpty(LodestoneBlockEntityInventory inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static ItemStack toStack(GenericStack stack) {
        return stack.what() instanceof AEItemKey key ? key.toStack((int) stack.amount()) : ItemStack.EMPTY;
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !memo.rejectLog.shouldLog(level, machine.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "{} {} {} push: {} (output={})",
                name,
                machine.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
