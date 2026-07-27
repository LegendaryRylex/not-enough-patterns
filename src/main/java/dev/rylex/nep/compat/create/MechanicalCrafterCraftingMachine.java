package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.GridPlacement;
import dev.rylex.nep.pattern.GridPlan;
import dev.rylex.nep.pattern.GridPos;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MechanicalCrafterCraftingMachine implements ICraftingMachine {

    private final MechanicalCrafterBlockEntity be;

    public MechanicalCrafterCraftingMachine(MechanicalCrafterBlockEntity be) {
        this.be = be;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(AllBlocks.MECHANICAL_CRAFTER.asStack()),
                AllBlocks.MECHANICAL_CRAFTER.get().getName(),
                List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.createMechanicalCrafting();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.createMechanicalCrafting()) {
            return false;
        }
        if (be.getSpeed() == 0) {
            return false;
        }

        GridPlan plan = patternDetails instanceof MechanicalCraftingPattern pattern ? pattern.plan() : null;
        if (plan == null) {
            return false;
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return false;
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.multiset())) {
            return false;
        }

        CrafterChains.Chain chain = CrafterChains.of(be);
        if (chain == null) {
            return false;
        }

        Map<GridPos, MechanicalCrafterBlockEntity> open = new HashMap<>();
        for (MechanicalCrafterBlockEntity crafter : chain.crafters()) {
            if (crafter.craftingItemPresent()) {
                return false;
            }
            if (!CrafterChains.isCovered(crafter)) {
                open.put(chain.positions().get(crafter), crafter);
            }
        }

        List<Integer> filledCells = plan.filledIndices();
        GridPos[] placement = GridPlacement.find(
                plan.width(), filledCells, open.keySet(), chain.positions().get(chain.output()));
        if (placement == null) {
            return false;
        }

        MechanicalCrafterBlockEntity[] targets = new MechanicalCrafterBlockEntity[placement.length];
        ItemStack[] stacks = new ItemStack[placement.length];
        for (int i = 0; i < placement.length; i++) {
            targets[i] = open.get(placement[i]);
            stacks[i] = plan.cells().get(filledCells.get(i)).toStack();
        }

        for (int i = 0; i < targets.length; i++) {
            if (!targets[i].getInventory().insertItem(0, stacks[i], true).isEmpty()) {
                return false;
            }
        }
        for (int i = 0; i < targets.length; i++) {
            targets[i].getInventory().insertItem(0, stacks[i], false);
        }
        be.checkCompletedRecipe(true);
        return true;
    }
}
