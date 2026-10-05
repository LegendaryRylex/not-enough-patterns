package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionModifiedIngredientGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_modified_ingredients";

    private FusionModifiedIngredientGameTest() {}

    private record Subject(RecipeHolder<IFusionRecipe> holder, IPatternDetails pattern, AEItemKey catalyst) {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void patternsAcceptAnEnchantedCatalyst(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();
        Subject subject = firstDataCarryingRecipe(level);
        helper.assertTrue(subject != null, "no fusion recipe carries catalyst data onto its result");

        AEItemKey enchanted = enchant(level, subject.catalyst());
        helper.assertTrue(!enchanted.equals(subject.catalyst()), "enchanting the catalyst did not change its key");

        boolean accepted = false;
        boolean declared = false;
        for (IPatternDetails.IInput input : subject.pattern().getInputs()) {
            accepted |= input.isValid(enchanted, level);
            declared |= subject.catalyst().equals(input.getPossibleInputs()[0].what());
        }
        helper.assertTrue(accepted, "no pattern slot accepted the enchanted catalyst " + enchanted);
        helper.assertTrue(declared, "the pattern stopped asking to craft a fresh " + subject.catalyst());

        AEItemKey unrelated = AEItemKey.of(Items.DIRT);
        for (IPatternDetails.IInput input : subject.pattern().getInputs()) {
            helper.assertTrue(!input.isValid(unrelated, level), "a pattern slot accepted an unrelated item");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anEnchantedCatalystIsFusedAsItself(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();
        Subject subject = firstDataCarryingRecipe(level);
        helper.assertTrue(subject != null, "no fusion recipe carries catalyst data onto its result");

        AEItemKey enchanted = enchant(level, subject.catalyst());
        AEItemKey plainResult = FusionResults.resultFor(subject.holder().value(), level, Set.of(subject.catalyst()));
        AEItemKey craftedResult = FusionResults.resultFor(subject.holder().value(), level, Set.of(enchanted));
        helper.assertTrue(plainResult != null && craftedResult != null, "the recipe did not assemble a result");
        helper.assertTrue(
                !craftedResult.equals(plainResult),
                "fusing the enchanted catalyst produced the same key as a plain one");
        helper.assertTrue(
                craftedResult.toStack().isEnchanted(), "the enchantment did not ride along onto " + craftedResult);

        CraftedOutputs.expect(craftedResult, plainResult);
        helper.assertTrue(
                plainResult.equals(CraftedOutputs.declaredFor(craftedResult)),
                "a job waiting for " + plainResult + " would not settle on " + craftedResult);
        CraftedOutputs.forget(craftedResult);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theCraftingCoreLoadsTheCatalystItWasHanded(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();
        Subject subject = firstDataCarryingRecipe(level);
        helper.assertTrue(subject != null, "no fusion recipe carries catalyst data onto its result");

        FusionRecipeResolver.Plan plan = FusionRecipeResolver.resolve(subject.pattern(), level);
        helper.assertTrue(plan != null, "the pattern did not resolve to a plan");

        AEItemKey enchanted = enchant(level, subject.catalyst());
        Map<AEItemKey, Long> provided = new LinkedHashMap<>();
        for (Map.Entry<AEItemKey, Long> entry : plan.expectedItems().entrySet()) {
            provided.merge(
                    entry.getKey().equals(subject.catalyst()) ? enchanted : entry.getKey(),
                    entry.getValue(),
                    Long::sum);
        }

        FusionRecipeResolver.Plan reassigned = FusionRecipeResolver.planFromProvided(plan, provided, level);
        helper.assertTrue(reassigned != null, "the crafting core refused the enchanted catalyst");
        helper.assertTrue(
                enchanted.equals(reassigned.catalyst().what()),
                "the crafting core would have loaded " + reassigned.catalyst() + " instead of the item it was handed");
        helper.assertTrue(
                !reassigned.result().equals(plan.result()), "the plan still expects the plain result " + plan.result());
        helper.succeed();
    }

    static AEItemKey enchant(Level level, AEItemKey key) {
        Holder<Enchantment> unbreaking = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.UNBREAKING);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(unbreaking, 1);
        ItemStack stack = key.toStack();
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return AEItemKey.of(stack);
    }

    @Nullable
    private static Subject firstDataCarryingRecipe(Level level) {
        for (RecipeHolder<IFusionRecipe> holder : FusionRecipeResolver.candidates(level)) {
            if (!DraconicRecipeIngredients.carriesIngredientData(FusionResults.expectedResult(holder.value(), level))) {
                continue;
            }
            EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, level, true);
            if (encoded == null || encoded.inputs().isEmpty()) {
                continue;
            }
            if (!(encoded.inputs().get(0).get(0).what() instanceof AEItemKey catalyst)) {
                continue;
            }
            IPatternDetails pattern = decode(holder, level, encoded);
            if (pattern != null) {
                return new Subject(holder, pattern, catalyst);
            }
        }
        return null;
    }

    @Nullable
    private static IPatternDetails decode(RecipeHolder<IFusionRecipe> holder, Level level, EncodedIngredients encoded) {
        List<GenericStack> consumed = new ArrayList<>();
        List<GenericStack> kept = new ArrayList<>();
        for (int slot = 0; slot < encoded.inputs().size(); slot++) {
            (encoded.isRetained(slot) ? kept : consumed)
                    .add(encoded.inputs().get(slot).get(0));
        }
        GenericStack result = encoded.outputs().get(0);
        ItemStack pattern = FusionCraftingPattern.encode(holder.id(), consumed, kept, result);
        return PatternDetailsHelper.decodePattern(AEItemKey.of(pattern), level);
    }
}
