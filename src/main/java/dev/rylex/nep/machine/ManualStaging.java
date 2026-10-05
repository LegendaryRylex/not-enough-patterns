package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ManualStaging {
    private ManualStaging() {}

    public record Result(ManualCraftResult status, int batches, Map<AEItemKey, Long> perCraft) {}

    public static Result pull(
            Player player,
            List<ManualRequirement> requirements,
            int wanted,
            Predicate<Map<AEItemKey, Long>> stageIntoMachine) {
        List<ItemStack> inventory = ManualPull.snapshot(player.getInventory().items);
        int preferredSlot = player.getInventory().selected;
        int affordable = ManualPull.affordableBatches(inventory, requirements, wanted, preferredSlot);
        if (affordable <= 0) {
            return new Result(ManualCraftResult.MISSING_ITEMS, 0, Map.of());
        }
        List<ManualPull.Take> takes = null;
        while (affordable > 0) {
            takes = ManualPull.plan(inventory, requirements, affordable, preferredSlot);
            if (takes != null && stageIntoMachine.test(ManualPull.totals(inventory, takes))) {
                break;
            }
            takes = null;
            affordable--;
        }
        if (takes == null) {
            return new Result(ManualCraftResult.BUFFER_FULL, 0, Map.of());
        }
        for (ManualPull.Take take : takes) {
            player.getInventory().removeItem(take.slot(), take.count());
        }
        player.getInventory().setChanged();
        List<ManualPull.Take> perCraft = ManualPull.plan(inventory, requirements, 1, preferredSlot);
        return new Result(
                ManualCraftResult.STARTED,
                affordable,
                perCraft == null ? Map.of() : ManualPull.consumedTotals(inventory, perCraft, requirements));
    }
}
