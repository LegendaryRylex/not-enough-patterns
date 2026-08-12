package dev.rylex.nep.compat.apothic;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.RavenTableStats;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public final class InfusionCraftingMachineGameTest {

    private static final BlockPos TABLE = new BlockPos(4, 2, 4);
    private static final BlockPos RETURN = TABLE.above();
    private static final BlockPos SIDE = TABLE.north();

    private InfusionCraftingMachineGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "every_infusion_recipe_gets_a_whole_number_stat_triple",
                        InfusionCraftingMachineGameTest::everyInfusionRecipeGetsAWholeNumberStatTripleInsideItsWindow)
                .add(
                        "an_infusion_pattern_is_run_and_its_result_returned",
                        InfusionCraftingMachineGameTest::anInfusionPatternIsRunAndItsResultReturned)
                .add(
                        "a_result_goes_to_another_inventory_when_the_provider_side_has_none",
                        InfusionCraftingMachineGameTest::aResultGoesToAnotherInventoryWhenTheProviderSideHasNone)
                .add(
                        "a_result_with_nowhere_to_go_is_dropped_above_the_table",
                        InfusionCraftingMachineGameTest::aResultWithNowhereToGoIsDroppedAboveTheTable)
                .add(
                        "an_automated_infusion_puts_the_players_own_stats_back",
                        InfusionCraftingMachineGameTest::anAutomatedInfusionPutsThePlayersOwnStatsBack)
                .add(
                        "a_pattern_that_skips_the_lapis_and_experience_cost_is_refused",
                        InfusionCraftingMachineGameTest::aPatternThatSkipsTheLapisAndExperienceCostIsRefused)
                .add(
                        "the_experience_payment_is_the_fluid_where_one_carries_the_tag",
                        InfusionCraftingMachineGameTest
                                ::theExperiencePaymentIsTheFluidWhereverOneCarriesTheExperienceTag)
                .add(
                        "a_pattern_paying_a_fluid_that_is_not_experience_is_refused",
                        InfusionCraftingMachineGameTest::aPatternPayingAFluidThatIsNotExperienceIsRefused)
                .add(
                        "an_encoded_infusion_pattern_is_accepted_by_the_table",
                        InfusionCraftingMachineGameTest::anEncodedInfusionPatternIsAcceptedByTheTable)
                .add(
                        "a_processing_pattern_carrying_an_infusion_is_upgraded",
                        InfusionCraftingMachineGameTest
                                ::aProcessingPatternCarryingAnInfusionIsUpgradedToAnEnchantingPattern)
                .add(
                        "a_recipe_viewer_transfer_is_left_as_a_processing_pattern",
                        InfusionCraftingMachineGameTest::aRecipeViewerTransferIsLeftAsAProcessingPattern)
                .add(
                        "a_vanilla_enchanting_table_takes_no_patterns",
                        InfusionCraftingMachineGameTest::aVanillaEnchantingTableTakesNoPatterns);
    }

    private static EnchantingTableBlockEntity placeTable(GameTestHelper helper) {
        InfusionCraftingMachine.clearCache();
        InfusionRecipeResolver.clearCache();
        helper.setBlock(TABLE, Ench.Blocks.RAVEN_ENCHANTING_TABLE.value().defaultBlockState());
        helper.setBlock(RETURN, Blocks.BARREL.defaultBlockState());
        return helper.getBlockEntity(TABLE, EnchantingTableBlockEntity.class);
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(TABLE), null);
        helper.assertTrue(machine != null, "the Enchanting Table of the Raven exposed no crafting machine capability");
        return machine;
    }

    private static List<ItemStack> accepted(InfusionRecipe recipe, Level level) {
        return IngredientMatching.itemOptions(recipe.getInput(), level).stream()
                .map(AEItemKey::toStack)
                .toList();
    }

    private static RecipeHolder<InfusionRecipe> simpleRecipe(GameTestHelper helper) {
        RecipeHolder<InfusionRecipe> best = null;
        for (RecipeHolder<InfusionRecipe> holder : InfusionRecipeResolver.candidates(helper.getLevel())) {
            InfusionRecipe recipe = holder.value();
            if (accepted(recipe, helper.getLevel()).size() != 1
                    || recipe.getOutput().create().isEmpty()) {
                continue;
            }
            if (InfusionRecipeResolver.statsFor(recipe) == null) {
                continue;
            }
            ItemStack input = accepted(recipe, helper.getLevel()).get(0);
            if (input.is(Items.LAPIS_LAZULI) || input.is(Items.EXPERIENCE_BOTTLE)) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no infusion recipe with a single input item was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<InfusionRecipe> holder) {
        return patternFor(helper, holder, InfusionPayments.preferred(holder.value(), InfusionCosts.Rates.fromConfig()));
    }

    private static IPatternDetails patternFor(
            GameTestHelper helper, RecipeHolder<InfusionRecipe> holder, @Nullable GenericStack experience) {
        InfusionRecipe recipe = holder.value();
        List<GenericStack> inputs = new ArrayList<>();
        inputs.add(new GenericStack(
                AEItemKey.of(accepted(recipe, helper.getLevel()).get(0)), 1));
        inputs.add(new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), InfusionCosts.LAPIS));
        if (experience != null) {
            inputs.add(experience);
        }
        return decode(
                helper,
                PatternDetailsHelper.encodeProcessingPattern(
                        inputs,
                        List.of(GenericStack.fromItemStack(recipe.getOutput().create()))));
    }

    private static IPatternDetails decode(GameTestHelper helper, ItemStack encoded) {
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the infusion pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    private static int countReturned(GameTestHelper helper, ItemStack wanted) {
        return countIn(helper, RETURN, Direction.DOWN, wanted);
    }

    private static int countIn(GameTestHelper helper, BlockPos pos, Direction face, ItemStack wanted) {
        ResourceHandler<ItemResource> inventory =
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), face);
        helper.assertTrue(inventory != null, "the inventory at " + pos + " exposed no item handler");
        int found = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack held = inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
            if (ItemStack.isSameItemSameComponents(held, wanted)) {
                found += held.getCount();
            }
        }
        return found;
    }

    public static void everyInfusionRecipeGetsAWholeNumberStatTripleInsideItsWindow(GameTestHelper helper) {
        InfusionRecipeResolver.clearCache();
        List<String> unreachable = new ArrayList<>();
        int checked = 0;
        for (RecipeHolder<InfusionRecipe> holder : InfusionRecipeResolver.candidates(helper.getLevel())) {
            InfusionRecipe recipe = holder.value();
            InfusionRecipeResolver.Stats stats = InfusionRecipeResolver.statsFor(recipe);
            if (stats == null) {
                unreachable.add(holder.id() + " (no whole number fits its stat window)");
                continue;
            }
            List<ItemStack> inputs = accepted(recipe, helper.getLevel());
            if (inputs.isEmpty()) {
                continue;
            }
            checked++;
            if (!recipe.matches(inputs.get(0), stats.eterna(), stats.quanta(), stats.arcana())) {
                unreachable.add(holder.id() + " rejected the stats picked for it: eterna " + stats.eterna()
                        + ", quanta " + stats.quanta() + ", arcana " + stats.arcana());
            }
        }
        helper.assertTrue(checked > 0, "no infusion recipes were loaded, so this test proves nothing");
        helper.assertTrue(
                unreachable.isEmpty(),
                "the table could not adapt to every infusion recipe:\n  " + String.join("\n  ", unreachable));
        helper.succeed();
    }

    public static void anInfusionPatternIsRunAndItsResultReturned(GameTestHelper helper) {
        placeTable(helper);
        helper.setBlock(SIDE, Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack expected = holder.value().getOutput().create();

        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "an infusion pattern the table can satisfy was rejected");

        int returned = countReturned(helper, expected);
        helper.assertTrue(
                returned == expected.getCount(),
                "the table returned " + returned + " of " + expected + " rather than " + expected.getCount()
                        + ", so the crafting job would wait forever");
        helper.assertTrue(
                countIn(helper, SIDE, Direction.SOUTH, expected) == 0,
                "the result went to the barrel beside the table rather than to the provider that asked for it");
        helper.succeed();
    }

    public static void aResultGoesToAnotherInventoryWhenTheProviderSideHasNone(GameTestHelper helper) {
        placeTable(helper);
        helper.setBlock(RETURN, Blocks.AIR);
        helper.setBlock(SIDE, Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack expected = holder.value().getOutput().create();

        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "an infusion pattern was refused with an inventory beside the table to take its result");

        int found = countIn(helper, SIDE, Direction.SOUTH, expected);
        helper.assertTrue(
                found == expected.getCount(),
                "the barrel beside the table holds " + found + " of " + expected + " rather than "
                        + expected.getCount());
        helper.assertItemEntityNotPresent(expected.getItem());
        helper.succeed();
    }

    public static void aResultWithNowhereToGoIsDroppedAboveTheTable(GameTestHelper helper) {
        placeTable(helper);
        helper.setBlock(RETURN, Blocks.AIR);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack expected = holder.value().getOutput().create();

        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "an infusion pattern was refused rather than dropping its result with nowhere to put it");
        helper.assertItemEntityCountIs(expected.getItem(), RETURN, 1.5, expected.getCount());
        helper.succeed();
    }

    public static void anAutomatedInfusionPutsThePlayersOwnStatsBack(GameTestHelper helper) {
        EnchantingTableBlockEntity table = placeTable(helper);
        ICraftingMachine machine = machine(helper);

        RavenTableStats stats = table.getData(RavenTableStats.TYPE);
        stats.set(15, 4, 9);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP), "the infusion pattern was rejected");

        RavenTableStats after = table.getData(RavenTableStats.TYPE);
        helper.assertTrue(
                after.eterna() == 15 && after.quanta() == 4 && after.arcana() == 9,
                "the craft left the table at eterna " + after.eterna() + ", quanta " + after.quanta() + ", arcana "
                        + after.arcana() + " instead of the player's own 15/4/9");
        helper.succeed();
    }

    public static void aPatternThatSkipsTheLapisAndExperienceCostIsRefused(GameTestHelper helper) {
        placeTable(helper);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(
                        AEItemKey.of(accepted(holder.value(), helper.getLevel()).get(0)), 1)),
                List.of(GenericStack.fromItemStack(holder.value().getOutput().create())));
        IPatternDetails details = decode(helper, encoded);

        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a pattern that pays neither lapis nor experience ran an infusion for free");
        helper.assertTrue(
                countReturned(helper, holder.value().getOutput().create()) == 0,
                "a refused push still produced its result");
        helper.succeed();
    }

    public static void theExperiencePaymentIsTheFluidWhereverOneCarriesTheExperienceTag(GameTestHelper helper) {
        placeTable(helper);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        InfusionCosts.Rates rates = InfusionCosts.Rates.fromConfig();
        GenericStack payment = InfusionPayments.preferred(holder.value(), rates);
        helper.assertTrue(payment != null, "an infusion recipe asked for no experience at all");

        AEFluidKey fluid = ExperienceFluids.preferred();
        if (fluid == null) {
            helper.assertTrue(
                    payment.what() instanceof AEItemKey key && key.matches(new ItemStack(Items.EXPERIENCE_BOTTLE)),
                    "with no fluid carrying the experience tag the payment should be bottles, but it was " + payment);
            helper.assertTrue(
                    payment.amount() == InfusionCosts.bottleCost(holder.value(), rates),
                    "the pattern asked for " + payment.amount() + " bottles rather than the "
                            + InfusionCosts.bottleCost(holder.value(), rates) + " the recipe costs");
        } else {
            helper.assertTrue(
                    fluid.equals(payment.what()),
                    "an experience fluid is loaded but the payment was " + payment + " rather than " + fluid);
            helper.assertTrue(
                    payment.amount() == InfusionCosts.fluidCost(holder.value(), rates),
                    "the pattern asked for " + payment.amount() + "mB rather than the "
                            + InfusionCosts.fluidCost(holder.value(), rates) + " the recipe costs");
        }

        IPatternDetails details = patternFor(helper, holder, payment);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "the table refused a pattern paying the experience it asks for");
        helper.succeed();
    }

    public static void aPatternPayingAFluidThatIsNotExperienceIsRefused(GameTestHelper helper) {
        placeTable(helper);
        ICraftingMachine machine = machine(helper);

        AEFluidKey water = AEFluidKey.of(Fluids.WATER);
        helper.assertTrue(
                !ExperienceFluids.isExperience(water), "water carries the experience tag, so this test proves nothing");

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails details = patternFor(
                helper,
                holder,
                new GenericStack(water, InfusionCosts.fluidCost(holder.value(), InfusionCosts.Rates.fromConfig())));

        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a pattern paying its experience in water ran an infusion");
        helper.assertTrue(
                countReturned(helper, holder.value().getOutput().create()) == 0,
                "a refused push still produced its result");
        helper.succeed();
    }

    public static void anEncodedInfusionPatternIsAcceptedByTheTable(GameTestHelper helper) {
        placeTable(helper);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        ItemStack bare = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(
                        AEItemKey.of(accepted(holder.value(), helper.getLevel()).get(0)), 1)),
                List.of(GenericStack.fromItemStack(holder.value().getOutput().create())));
        ItemStack converted = PatternConverters.convertQuietly(
                PatternOrigin.ofRecipe(holder.id().identifier()), bare, helper.getLevel());
        helper.assertTrue(converted != null, "the infusion recipe encoded no pattern");
        helper.assertTrue(
                converted.is(NepItems.ENCHANTING_PATTERN.get()),
                "the infusion recipe encoded a " + converted + " rather than an Enchanting Pattern");

        IPatternDetails details = decode(helper, converted);
        helper.assertTrue(details instanceof EnchantingPattern, "the encoded pattern decoded as " + details);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "the table refused the pattern its own encoder produced");
        helper.succeed();
    }

    public static void aProcessingPatternCarryingAnInfusionIsUpgradedToAnEnchantingPattern(GameTestHelper helper) {
        placeTable(helper);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails processing = patternFor(helper, holder);
        ItemStack converted = PatternConverters.convertQuietly(
                PatternOrigin.MANUAL, processing.getDefinition().toStack(), helper.getLevel());
        helper.assertTrue(
                converted != null && converted.is(NepItems.ENCHANTING_PATTERN.get()),
                "a processing pattern paying a whole infusion was not recognised as one: " + converted);

        IPatternDetails details = decode(helper, converted);
        helper.assertTrue(
                ((EnchantingPattern) details).recipe().equals(holder.id().identifier()),
                "the upgraded pattern named " + ((EnchantingPattern) details).recipe() + " rather than "
                        + holder.id().identifier());
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "the table refused the pattern it upgraded the processing pattern into");
        helper.succeed();
    }

    public static void aRecipeViewerTransferIsLeftAsAProcessingPattern(GameTestHelper helper) {
        placeTable(helper);

        RecipeHolder<InfusionRecipe> holder = simpleRecipe(helper);
        IPatternDetails processing = patternFor(helper, holder);
        ItemStack converted = PatternConverters.convertQuietly(
                PatternOrigin.RECIPE_VIEWER, processing.getDefinition().toStack(), helper.getLevel());
        helper.assertTrue(
                converted == null,
                "a recipe viewer filled these slots for a machine NEP does not drive, and NEP still claimed them as "
                        + converted + "; the pattern belongs to whichever machine the open category was for");
        helper.succeed();
    }

    public static void aVanillaEnchantingTableTakesNoPatterns(GameTestHelper helper) {
        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE.defaultBlockState());
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(TABLE), null);
        helper.assertTrue(
                machine == null,
                "a vanilla enchanting table exposed the crafting machine capability; only the Raven table adapts its"
                        + " stats, so only it can run a pattern");
        helper.succeed();
    }
}
