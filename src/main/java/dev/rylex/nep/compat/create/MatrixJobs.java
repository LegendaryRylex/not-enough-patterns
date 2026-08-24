package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MatrixJobs {
    private MatrixJobs() {}

    record Outcome(@Nullable MatrixJob job, String reason) {

        static Outcome of(MatrixJob job) {
            return new Outcome(job, "");
        }

        static Outcome refused(String reason) {
            return new Outcome(null, reason);
        }
    }

    static Outcome resolve(IPatternDetails pattern, Level level, AEItemKey output, int outputCount) {
        if (!(pattern instanceof AndesiteCraftingPattern)
                && pattern.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return Outcome.refused("not a sequenced assembly, andesite crafting or processing pattern");
        }

        List<GenericStack> consumed = new ArrayList<>();
        List<GenericStack> retained = new ArrayList<>();
        if (!split(pattern, consumed, retained)) {
            return Outcome.refused("unreadable pattern inputs");
        }

        if (NepConfig.createFilling() && FillingRecipeResolver.resolve(pattern, level) != null) {
            if (!retained.isEmpty()) {
                return Outcome.refused("a filling pattern cannot keep an input");
            }
            return Outcome.of(
                    new MatrixJob(MatrixJob.Kind.FILLING, output, outputCount, List.copyOf(consumed), List.of()));
        }

        if (NepConfig.createDeploying()) {
            ApplicationRecipeResolver.Plan plan = ApplicationRecipeResolver.resolve(pattern, level);
            if (plan != null) {
                String refusal = deployingRefusal(pattern, level, plan);
                if (refusal != null) {
                    return Outcome.refused(refusal);
                }
                return Outcome.of(new MatrixJob(
                        MatrixJob.Kind.DEPLOYING, output, outputCount, List.copyOf(consumed), List.copyOf(retained)));
            }
        }
        return Outcome.refused("no enabled filling or deploying recipe matched the pattern");
    }

    @Nullable
    private static String deployingRefusal(IPatternDetails pattern, Level level, ApplicationRecipeResolver.Plan plan) {
        if (plan.keptTool() != null && plan.suppliedTool() == null) {
            return "this recipe keeps its tool and the pattern supplies none; the matrix holds nothing by hand";
        }
        if (SandPaperPolishing.wearsKeptTool(pattern, level)) {
            return "a deployer sands its paper down a point per polish, and no pattern can name a tool that comes "
                    + "back with different damage";
        }
        if (toolIsWornRatherThanConsumed(plan)) {
            return "a deployer would wear this recipe's tool down by a point rather than consume it, and no pattern "
                    + "can name a result that comes back with different damage";
        }
        return null;
    }

    private static boolean toolIsWornRatherThanConsumed(ApplicationRecipeResolver.Plan plan) {
        return plan.consumedTool() != null && plan.consumedTool().toStack().isDamageableItem();
    }

    private static boolean split(IPatternDetails pattern, List<GenericStack> consumed, List<GenericStack> retained) {
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack primary = input.getPossibleInputs()[0];
            AEKey what = primary.what();
            long amount = primary.amount() * input.getMultiplier();
            if (amount <= 0 || !(what instanceof AEItemKey || what instanceof AEFluidKey)) {
                return false;
            }
            (input.getRemainingKey(what) == null ? consumed : retained).add(new GenericStack(what, amount));
        }
        return !consumed.isEmpty();
    }
}
