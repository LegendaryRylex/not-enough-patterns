package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.RitualPattern;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Recipes;
import dev.rylex.nep.util.Uniqueness;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class OccultismPatternEncoders {
    private OccultismPatternEncoders() {}

    static void register() {
        PatternConverters.register(DecoderModule.OCCULTISM, RitualRecipe.class, OccultismPatternEncoders::ritual);
        PatternConverters.registerFallback(DecoderModule.OCCULTISM, OccultismPatternEncoders::ritualByResult);
        RitualPattern.substituteInputsWith(OccultismRituals::substitution);
    }

    @Nullable
    private static ItemStack ritual(IPatternDetails encoded, RecipeHolder<RitualRecipe> holder, Level level) {
        return NepConfig.occultismRitualController() ? encode(holder, encoded, level) : null;
    }

    @Nullable
    private static PatternFallback.Result ritualByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.occultismRitualController()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                OccultismRituals.candidates(level), candidate -> encode(candidate, encoded, level));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encode(RecipeHolder<RitualRecipe> holder, IPatternDetails encoded, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        return OccultismRituals.plan(level, holder.value(), inputs, result) == null
                ? null
                : RitualPattern.encode(Recipes.idOf(holder), inputs, result);
    }
}
