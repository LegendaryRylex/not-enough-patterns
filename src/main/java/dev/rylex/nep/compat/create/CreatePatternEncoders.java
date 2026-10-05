package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.pattern.GridPlan;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.pattern.encoding.SyntheticRecipes;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class CreatePatternEncoders {
    private CreatePatternEncoders() {}

    static void register() {
        SyntheticRecipes.register((recipe, level) -> LogStripping.byId(recipe, level) != null);
        PatternConverters.register(
                DecoderModule.CREATE, SequencedAssemblyRecipe.class, CreatePatternEncoders::sequencedAssembly);
        PatternConverters.register(
                DecoderModule.CREATE, ItemApplicationRecipe.class, CreatePatternEncoders::itemApplication);
        PatternConverters.register(
                DecoderModule.CREATE, SandPaperPolishingRecipe.class, CreatePatternEncoders::sandPaperPolishing);
        PatternConverters.register(DecoderModule.CREATE, FillingRecipe.class, CreatePatternEncoders::filling);
        PatternConverters.register(
                DecoderModule.CREATE, CraftingRecipe.class, CreatePatternEncoders::mechanicalCrafting);
        PatternConverters.registerFallback(DecoderModule.CREATE, CreatePatternEncoders::mechanicalCraftingByResult);
        PatternConverters.registerFallback(DecoderModule.CREATE, CreatePatternEncoders::andesiteCraftingByResult);
        PatternConverters.registerFallback(DecoderModule.CREATE, CreatePatternEncoders::sequencedAssemblyByResult);
    }

    @Nullable
    private static ItemStack mechanicalCrafting(
            IPatternDetails encoded, RecipeHolder<CraftingRecipe> holder, Level level) {
        if (!NepConfig.createMechanicalCrafting()) {
            return null;
        }
        GridPlan plan = MechanicalRecipeResolver.resolveRecipe(encoded, holder.id(), level);
        return plan == null ? null : encodeGrid(plan, encoded);
    }

    @Nullable
    private static ItemStack sequencedAssembly(
            IPatternDetails encoded, RecipeHolder<SequencedAssemblyRecipe> holder, Level level) {
        if (!NepConfig.createSequencedAssembly() && !NepConfig.createSequencedAssemblyMatrix()) {
            return null;
        }
        List<GenericStack> inputs = condensedInputs(encoded);
        GenericStack result = singleResult(encoded);
        if (inputs == null || result == null || !(result.what() instanceof AEItemKey outputKey)) {
            return null;
        }
        if (!SequencedAssemblyResolver.produces(holder.value(), outputKey.toStack())) {
            return null;
        }
        EncodedIngredients expected = CreateRecipeIngredients.sequencedAssembly(holder, level);
        if (IngredientMatching.matchAssign(expected, inputs, result) == null) {
            return null;
        }
        return SequencedAssemblyPattern.encode(holder.id(), inputs, result);
    }

    @Nullable
    private static ItemStack itemApplication(
            IPatternDetails encoded, RecipeHolder<ItemApplicationRecipe> holder, Level level) {
        if (!NepConfig.createDeploying()) {
            return null;
        }
        return andesiteCrafting(encoded, holder, CreateRecipeIngredients.itemApplication(holder, level));
    }

    @Nullable
    private static ItemStack sandPaperPolishing(
            IPatternDetails encoded, RecipeHolder<SandPaperPolishingRecipe> holder, Level level) {
        if (!NepConfig.createDeploying()) {
            return null;
        }
        RecipeHolder<DeployerApplicationRecipe> application = SandPaperPolishing.substitute(holder, level);
        return application == null
                ? null
                : andesiteCrafting(encoded, application, CreateRecipeIngredients.itemApplication(application, level));
    }

    @Nullable
    private static ItemStack filling(IPatternDetails encoded, RecipeHolder<FillingRecipe> holder, Level level) {
        if (!NepConfig.createFilling()) {
            return null;
        }
        return andesiteCrafting(encoded, holder, CreateRecipeIngredients.spoutFilling(holder, level));
    }

    @Nullable
    private static ItemStack andesiteCrafting(
            IPatternDetails encoded, RecipeHolder<?> holder, @Nullable EncodedIngredients expected) {
        List<GenericStack> inputs = condensedInputs(encoded);
        GenericStack result = singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        Split split = splitAndesite(expected, holder, inputs, result);
        return split == null
                ? null
                : AndesiteCraftingPattern.encode(holder.id(), split.consumed(), split.kept(), split.worn(), result);
    }

    private record Split(List<GenericStack> consumed, List<GenericStack> kept, List<GenericStack> worn) {}

    @Nullable
    private static Split splitAndesite(
            @Nullable EncodedIngredients expected,
            RecipeHolder<?> holder,
            List<GenericStack> inputs,
            GenericStack result) {
        if (expected == null || !AllRecipeTypes.CAN_BE_AUTOMATED.test(holder)) {
            return null;
        }
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null) {
            return null;
        }
        List<GenericStack> consumed = new ArrayList<>();
        List<GenericStack> kept = new ArrayList<>();
        List<GenericStack> worn = new ArrayList<>();
        for (int slot = 0; slot < chosen.length; slot++) {
            if (expected.isWorn(slot)) {
                worn.add(chosen[slot]);
            } else if (expected.isRetained(slot)) {
                kept.add(chosen[slot]);
            } else {
                consumed.add(chosen[slot]);
            }
        }
        return consumed.isEmpty() ? null : new Split(List.copyOf(consumed), List.copyOf(kept), List.copyOf(worn));
    }

    @Nullable
    private static PatternFallback.Result andesiteCraftingByResult(IPatternDetails encoded, Level level) {
        List<GenericStack> inputs = condensedInputs(encoded);
        GenericStack result = singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }

        RecipeHolder<?> only = null;
        Split split = null;

        if (NepConfig.createDeploying()) {
            for (RecipeHolder<? extends ItemApplicationRecipe> holder : ApplicationRecipeResolver.candidates(level)) {
                Split candidate =
                        splitAndesite(CreateRecipeIngredients.itemApplication(holder, level), holder, inputs, result);
                if (candidate != null) {
                    if (only != null) {
                        return null;
                    }
                    only = holder;
                    split = candidate;
                }
            }
        }

        if (NepConfig.createFilling()) {
            for (RecipeHolder<FillingRecipe> holder : FillingRecipeResolver.candidates(level)) {
                Split candidate =
                        splitAndesite(CreateRecipeIngredients.spoutFilling(holder, level), holder, inputs, result);
                if (candidate != null) {
                    if (only != null) {
                        return null;
                    }
                    only = holder;
                    split = candidate;
                }
            }
        }

        return only == null
                ? null
                : PatternFallback.Result.of(AndesiteCraftingPattern.encode(
                        only.id(), split.consumed(), split.kept(), split.worn(), result));
    }

    @Nullable
    private static PatternFallback.Result mechanicalCraftingByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.createMechanicalCrafting()) {
            return null;
        }
        GridPlan plan = MechanicalRecipeResolver.resolve(encoded, level);
        return plan == null ? null : PatternFallback.Result.of(encodeGrid(plan, encoded));
    }

    @Nullable
    private static PatternFallback.Result sequencedAssemblyByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.createSequencedAssembly() && !NepConfig.createSequencedAssemblyMatrix()) {
            return null;
        }
        GenericStack result = singleResult(encoded);
        if (result == null || !(result.what() instanceof AEItemKey outputKey)) {
            return null;
        }
        List<RecipeHolder<SequencedAssemblyRecipe>> candidates =
                SequencedAssemblyResolver.candidatesFor(level, outputKey.toStack());
        if (candidates.isEmpty()) {
            return null;
        }

        List<GenericStack> inputs = condensedInputs(encoded);
        if (inputs != null) {
            RecipeHolder<SequencedAssemblyRecipe> only = Uniqueness.onlyMatch(
                    candidates,
                    candidate -> IngredientMatching.matchAssign(
                                            CreateRecipeIngredients.sequencedAssembly(candidate, level), inputs, result)
                                    == null
                            ? null
                            : candidate);
            if (only != null) {
                return PatternFallback.Result.of(SequencedAssemblyPattern.encode(only.id(), inputs, result));
            }
        }

        return PatternFallback.Result.feedback(Component.translatable("nep.encoding.assembly_unresolved"));
    }

    private static ItemStack encodeGrid(GridPlan plan, IPatternDetails encoded) {
        List<GenericStack> cells = new ArrayList<>(plan.cells().size());
        for (AEItemKey key : plan.cells()) {
            cells.add(key == null ? null : new GenericStack(key, 1));
        }
        return MechanicalCraftingPattern.encode(
                plan.width(), plan.height(), cells, encoded.getOutputs().get(0));
    }

    @Nullable
    private static GenericStack singleResult(IPatternDetails encoded) {
        return PatternStacks.singleResult(encoded);
    }

    @Nullable
    private static List<GenericStack> condensedInputs(IPatternDetails encoded) {
        return PatternStacks.condensedInputs(encoded);
    }
}
