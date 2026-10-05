package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ArsPatternEncoders {
    private ArsPatternEncoders() {}

    static void register() {
        PatternConverters.register(
                DecoderModule.ARS_NOUVEAU, EnchantingApparatusRecipe.class, ArsPatternEncoders::apparatus);
        PatternConverters.register(DecoderModule.ARS_NOUVEAU, ImbuementRecipe.class, ArsPatternEncoders::imbuement);
        PatternConverters.registerFallback(DecoderModule.ARS_NOUVEAU, ArsPatternEncoders::apparatusByResult);
        PatternConverters.registerFallback(DecoderModule.ARS_NOUVEAU, ArsPatternEncoders::imbuementByResult);
    }

    @Nullable
    private static ItemStack imbuement(IPatternDetails encoded, RecipeHolder<ImbuementRecipe> holder, Level level) {
        if (!NepConfig.arsImbuementChamber()) {
            return null;
        }
        if (holder.value().getType() != ArsRecipeResolver.imbuementType()) {
            return null;
        }
        return encodeImbuement(holder, level, encoded);
    }

    @Nullable
    private static PatternFallback.Result imbuementByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.arsImbuementChamber()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                ArsRecipeResolver.imbuementCandidates(level), candidate -> encodeImbuement(candidate, level, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encodeImbuement(
            RecipeHolder<ImbuementRecipe> holder, Level level, IPatternDetails encoded) {
        var inputs = PatternStacks.condensedInputs(encoded);
        var result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        ArsRecipeResolver.ImbuementPlan plan = ArsRecipeResolver.imbuementPlan(holder, level, inputs, result);
        if (plan == null) {
            return null;
        }
        return ImbuementPattern.encode(holder.id(), List.of(plan.reagent()), plan.result());
    }

    @Nullable
    private static ItemStack apparatus(
            IPatternDetails encoded, RecipeHolder<EnchantingApparatusRecipe> holder, Level level) {
        if (!NepConfig.arsEnchantingApparatus()) {
            return null;
        }
        if (!ArsRecipeResolver.runsOnApparatus(holder.value().getType())) {
            return null;
        }
        return encodeApparatus(holder, level, encoded);
    }

    @Nullable
    private static PatternFallback.Result apparatusByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.arsEnchantingApparatus()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                ArsRecipeResolver.apparatusCandidates(level), candidate -> encodeApparatus(candidate, level, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encodeApparatus(
            RecipeHolder<EnchantingApparatusRecipe> holder, Level level, IPatternDetails encoded) {
        var inputs = PatternStacks.condensedInputs(encoded);
        var result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        ArsRecipeResolver.ApparatusPlan plan = ArsRecipeResolver.plan(holder, level, inputs, result);
        if (plan == null) {
            return null;
        }
        List<GenericStack> ordered = new ArrayList<>(plan.pedestals().size() + 1);
        ordered.add(plan.reagent());
        ordered.addAll(plan.pedestals());
        return ApparatusPattern.encode(holder.id(), ordered, plan.result());
    }
}
