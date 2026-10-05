package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MalumPatternEncoders {
    private MalumPatternEncoders() {}

    static void register() {
        PatternConverters.register(
                DecoderModule.MALUM, SpiritInfusionRecipe.class, MalumPatternEncoders::spiritInfusion);
        PatternConverters.register(
                DecoderModule.MALUM, SpiritFocusingRecipe.class, MalumPatternEncoders::spiritFocusing);
        PatternConverters.register(DecoderModule.MALUM, RuneworkingRecipe.class, MalumPatternEncoders::runeworking);
        PatternConverters.registerFallback(DecoderModule.MALUM, MalumPatternEncoders::spiritInfusionByResult);
        PatternConverters.registerFallback(DecoderModule.MALUM, MalumPatternEncoders::spiritFocusingByResult);
        PatternConverters.registerFallback(DecoderModule.MALUM, MalumPatternEncoders::runeworkingByResult);
    }

    @Nullable
    private static ItemStack runeworking(IPatternDetails encoded, RecipeHolder<RuneworkingRecipe> holder, Level level) {
        return NepConfig.malumRuneworking() ? encodeRuneworking(holder, encoded) : null;
    }

    @Nullable
    private static PatternFallback.Result runeworkingByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.malumRuneworking()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                RuneworkingResolver.candidates(level), candidate -> encodeRuneworking(candidate, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encodeRuneworking(RecipeHolder<RuneworkingRecipe> holder, IPatternDetails encoded) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        RuneworkingResolver.Plan plan = RuneworkingResolver.plan(holder, inputs, result);
        return plan == null
                ? null
                : RuneworkingPattern.encode(holder.id(), List.of(plan.primary(), plan.secondary()), plan.result());
    }

    @Nullable
    private static ItemStack spiritFocusing(
            IPatternDetails encoded, RecipeHolder<SpiritFocusingRecipe> holder, Level level) {
        return NepConfig.malumSpiritFocusing() ? encodeFocusing(holder, encoded) : null;
    }

    @Nullable
    private static PatternFallback.Result spiritFocusingByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.malumSpiritFocusing()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                SpiritFocusingResolver.candidates(level), candidate -> encodeFocusing(candidate, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encodeFocusing(RecipeHolder<SpiritFocusingRecipe> holder, IPatternDetails encoded) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        SpiritFocusingResolver.Plan plan = SpiritFocusingResolver.plan(holder, inputs, result);
        return plan == null ? null : SpiritFocusingPattern.encode(holder.id(), plan.spirits(), plan.result());
    }

    @Nullable
    private static ItemStack spiritInfusion(
            IPatternDetails encoded, RecipeHolder<SpiritInfusionRecipe> holder, Level level) {
        return NepConfig.malumSpiritInfusion() ? encode(holder, encoded, level) : null;
    }

    @Nullable
    private static PatternFallback.Result spiritInfusionByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.malumSpiritInfusion()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                SpiritInfusionResolver.candidates(level), candidate -> encode(candidate, encoded, level));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encode(RecipeHolder<SpiritInfusionRecipe> holder, IPatternDetails encoded, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        SpiritInfusionResolver.Plan plan = SpiritInfusionResolver.plan(holder, inputs, result, level);
        if (plan == null) {
            return null;
        }
        List<GenericStack> chosen =
                new ArrayList<>(1 + plan.spirits().size() + plan.extras().size());
        chosen.add(plan.input());
        chosen.addAll(plan.spirits());
        chosen.addAll(plan.extras());
        return SpiritInfusionPattern.encode(holder.id(), chosen, plan.result());
    }
}
