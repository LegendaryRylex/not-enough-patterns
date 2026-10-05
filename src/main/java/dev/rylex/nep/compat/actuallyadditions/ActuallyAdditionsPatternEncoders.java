package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.AtomicReconstructionPattern;
import dev.rylex.nep.pattern.EmpoweringPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Uniqueness;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ActuallyAdditionsPatternEncoders {
    private ActuallyAdditionsPatternEncoders() {}

    static void register() {
        PatternConverters.register(
                DecoderModule.ACTUALLY_ADDITIONS, EmpowererRecipe.class, ActuallyAdditionsPatternEncoders::empowering);
        PatternConverters.register(
                DecoderModule.ACTUALLY_ADDITIONS, LaserRecipe.class, ActuallyAdditionsPatternEncoders::laser);
        PatternConverters.registerFallback(
                DecoderModule.ACTUALLY_ADDITIONS, ActuallyAdditionsPatternEncoders::empoweringByResult);
    }

    @Nullable
    private static ItemStack empowering(IPatternDetails encoded, RecipeHolder<EmpowererRecipe> holder, Level level) {
        if (!NepConfig.actuallyAdditionsEmpowering() && !NepConfig.actuallyAdditionsMatrix()) {
            return null;
        }
        return ActuallyAdditionsRecipeResolver.planEmpowering(holder, encoded) == null
                ? null
                : encode(holder.id(), encoded, true);
    }

    @Nullable
    private static ItemStack laser(IPatternDetails encoded, RecipeHolder<LaserRecipe> holder, Level level) {
        if (!NepConfig.actuallyAdditionsAtomicReconstruction()) {
            return null;
        }
        return ActuallyAdditionsRecipeResolver.planLaser(holder, encoded, level) == null
                ? null
                : encode(holder.id(), encoded, false);
    }

    @Nullable
    private static PatternFallback.Result empoweringByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.actuallyAdditionsEmpowering() && !NepConfig.actuallyAdditionsMatrix()) {
            return null;
        }
        RecipeHolder<EmpowererRecipe> only = Uniqueness.onlyMatch(
                ActuallyAdditionsRecipeResolver.empoweringCandidates(level),
                candidate ->
                        ActuallyAdditionsRecipeResolver.planEmpowering(candidate, encoded) == null ? null : candidate);
        if (only == null) {
            return null;
        }
        ItemStack pattern = encode(only.id(), encoded, true);
        return pattern == null ? null : PatternFallback.Result.of(pattern);
    }

    @Nullable
    private static ItemStack encode(ResourceLocation recipe, IPatternDetails encoded, boolean empowering) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        return empowering
                ? EmpoweringPattern.encode(recipe, inputs, result)
                : AtomicReconstructionPattern.encode(recipe, inputs, result);
    }
}
