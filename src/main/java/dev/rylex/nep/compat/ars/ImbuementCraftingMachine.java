package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.ImbuementPattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ImbuementCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;
    private static final int SOURCE_RANGE = 2;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final ImbuementTile chamber;

    public ImbuementCraftingMachine(ImbuementTile chamber) {
        this.chamber = chamber;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(BlockRegistry.IMBUEMENT_BLOCK.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), BlockRegistry.IMBUEMENT_BLOCK.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.arsImbuementChamber();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.arsImbuementChamber()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs) {
        if (!(patternDetails instanceof ImbuementPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an imbuement or processing pattern");
        }

        ArsRecipeResolver.ImbuementPlan plan = ArsRecipeResolver.resolveImbuement(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no imbuement recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("the imbuement chamber takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }
        if (plan.catalystsInPattern()) {
            return PushOutcome.unsatisfiable(
                    "the pattern carries the pedestal items, which stay on the pedestals; encode the reagent alone");
        }

        if (!chamber.getItem(0).isEmpty()) {
            return PushOutcome.retry("the chamber is already holding an item");
        }
        if (!EnchantingApparatusRecipe.doItemsMatch(
                chamber.getPedestalItems(), plan.holder().value().getPedestalItems())) {
            return PushOutcome.retry("the pedestals touching the chamber do not hold this recipe's items");
        }

        if (plan.sourceCost() > 0
                && !SourceUtil.hasSourceNearby(chamber.getBlockPos(), level, SOURCE_RANGE, plan.sourceCost())) {
            return PushOutcome.retry("not enough source nearby for " + plan.sourceCost());
        }

        chamber.setItem(0, ArsRecipeResolver.toStack(plan.reagent()));
        chamber.setChanged();
        return PushOutcome.accepted("imbuing " + plan.result());
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, chamber.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Imbuement Chamber {} {} push: {} (output={})",
                chamber.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
