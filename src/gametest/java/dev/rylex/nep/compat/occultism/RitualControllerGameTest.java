package dev.rylex.nep.compat.occultism;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import com.klikli_dev.occultism.common.blockentity.GoldenSacrificialBowlBlockEntity;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import com.klikli_dev.occultism.registry.OccultismBlocks;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.util.ItemNBTUtil;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.RitualPattern;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Rotation;

public final class RitualControllerGameTest {

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final BlockPos CONTROLLER = new BlockPos(4, 4, 4);
    private static final BlockPos ME_CONTROLLER = CONTROLLER.east();
    private static final BlockPos ENERGY = ME_CONTROLLER.above();
    private static final List<BlockPos> BOWLS = List.of(
            new BlockPos(0, 1, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(7, 1, 0),
            new BlockPos(8, 1, 0),
            new BlockPos(0, 1, 8),
            new BlockPos(8, 1, 8));

    private static final Identifier CRUSHER_BOOK = Nep.id("ritual/infuse_foliot_crusher");
    private static final Identifier MATRIX = Nep.id("ritual/bound_spirit_matrix");
    private static final Identifier BUDDING_AMETHYST =
            Identifier.fromNamespaceAndPath("occultism", "ritual/misc_budding_amethyst");
    private static final TagKey<EntityType<?>> PIGS =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "pigs"));

    private static final int BOOT_TICKS = 60;

    private RitualControllerGameTest() {}

    public static void register(NepGameTests.Batch batch0) {
        batch0.add(
                        "controller_runs_a_ritual_pattern_on_the_real_pentacle",
                        1600,
                        RitualControllerGameTest::controllerRunsARitualPatternOnTheRealPentacle)
                .add(
                        "a_ritual_above_the_bound_spirits_tier_is_refused",
                        RitualControllerGameTest::aRitualAboveTheBoundSpiritsTierIsRefused)
                .add(
                        "a_controller_without_a_book_takes_no_patterns",
                        RitualControllerGameTest::aControllerWithoutABookTakesNoPatterns)
                .add(
                        "stand_ins_answer_only_for_their_own_sacrifice",
                        RitualControllerGameTest::standInsAnswerOnlyForTheirOwnSacrifice)
                .add(
                        "a_sacrifice_ritual_plans_only_with_a_stand_in",
                        RitualControllerGameTest::aSacrificeRitualPlansOnlyWithAStandIn)
                .add(
                        "a_ritual_pattern_takes_a_book_under_any_name",
                        RitualControllerGameTest::aRitualPatternTakesABookUnderAnyName);
    }

    private static RitualControllerBlockEntity controller(GameTestHelper helper, BoundSpirit.Tier tier) {
        helper.setBlock(CONTROLLER, NepOccultismContent.CONTROLLER.get());
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
        RitualControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, RitualControllerBlockEntity.class);
        if (tier != null) {
            controller.book().put(0, SpiritInfusedBookItem.of(new BoundSpirit(tier, BoundSpirit.Work.CRUSHER)));
        }
        return controller;
    }

    private static void pentacle(GameTestHelper helper, Identifier ritual) {
        RecipeHolder<RitualRecipe> holder = OccultismRituals.byId(helper.getLevel(), ritual);
        helper.assertTrue(holder != null, ritual + " is not loaded");
        Multiblock pentacle = holder.value().getPentacle();
        pentacle.place(helper.getLevel(), helper.absolutePos(CENTRE), Rotation.NONE);
        for (BlockPos bowl : BOWLS) {
            helper.setBlock(bowl, OccultismBlocks.SACRIFICIAL_BOWL.get().defaultBlockState());
        }
    }

    private static List<ItemStack> crusherBookInputs() {
        return List.of(
                new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get()),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.COPPER_INGOT),
                new ItemStack(OccultismItems.SILVER_INGOT.get()),
                new ItemStack(OccultismItems.BOOK_OF_BINDING_EMPTY.get()));
    }

    private static IPatternDetails ritualPattern(
            GameTestHelper helper, Identifier ritual, List<GenericStack> template, ItemStack result) {
        ItemStack encoded = RitualPattern.encode(ritual, template, GenericStack.fromItemStack(result));
        IPatternDetails pattern = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(pattern instanceof RitualPattern, "the test ritual pattern did not decode");
        return pattern;
    }

    private static boolean push(GameTestHelper helper, Identifier ritual, List<ItemStack> stacks, ItemStack result) {
        List<GenericStack> template = new ArrayList<>();
        for (ItemStack stack : stacks) {
            template.add(new GenericStack(AEItemKey.of(stack), stack.getCount()));
        }
        IPatternDetails pattern = ritualPattern(helper, ritual, template, result);
        ICraftingMachine machine = helper.getLevel()
                .getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CONTROLLER), Direction.NORTH);
        helper.assertTrue(machine != null, "the Ritual Controller offers no crafting machine");
        KeyCounter inputs = new KeyCounter();
        for (ItemStack stack : stacks) {
            inputs.add(AEItemKey.of(stack.copy()), stack.getCount());
        }
        return machine.pushPattern(pattern, new KeyCounter[] {inputs}, Direction.NORTH);
    }

    private static ItemStack crusherBook(BoundSpirit.Tier tier) {
        return SpiritInfusedBookItem.of(new BoundSpirit(tier, BoundSpirit.Work.CRUSHER));
    }

    private static boolean outputHolds(RitualControllerBlockEntity controller, BoundSpirit expected) {
        for (int slot = 0; slot < controller.getOutputBuffer().size(); slot++) {
            if (expected.equals(
                    controller.getOutputBuffer().stackAt(slot).get(NepOccultismContent.BOUND_SPIRIT.get()))) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack firstOf(
            net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.world.level.Level level) {
        return dev.rylex.nep.pattern.encoding.IngredientMatching.itemOptions(ingredient, level)
                .get(0)
                .toStack(1);
    }

    private static ItemStack soulGem(EntityType<?> held) {
        ItemStack gem = new ItemStack(OccultismItems.SOUL_GEM_ITEM.get());
        gem.set(DataComponents.ENTITY_DATA, TypedEntityData.of(held, new CompoundTag()));
        return gem;
    }

    public static void controllerRunsARitualPatternOnTheRealPentacle(GameTestHelper helper) {
        pentacle(helper, CRUSHER_BOOK);
        RitualControllerBlockEntity controller = controller(helper, BoundSpirit.Tier.MARID);
        BoundSpirit expected = new BoundSpirit(BoundSpirit.Tier.FOLIOT, BoundSpirit.Work.CRUSHER);
        helper.startSequence()
                .thenIdle(BOOT_TICKS)
                .thenExecute(() -> helper.assertTrue(
                        push(helper, CRUSHER_BOOK, crusherBookInputs(), crusherBook(BoundSpirit.Tier.FOLIOT)),
                        "the Controller refused a foliot ritual with a marid book bound"))
                .thenWaitUntil(() -> helper.assertTrue(
                        outputHolds(controller, expected),
                        "the Controller has not collected the foliot crusher book yet; stall " + controller.stall()
                                + ", refusal " + controller.refusal()))
                .thenExecute(() -> {
                    helper.assertTrue(controller.pendingJobs() == 0, "the finished ritual is still owed");
                    helper.assertTrue(
                            helper.getBlockEntity(CENTRE, GoldenSacrificialBowlBlockEntity.class)
                                            .getCurrentRitualRecipe()
                                    == null,
                            "the golden bowl is still running a ritual");
                })
                .thenSucceed();
    }

    public static void aRitualAboveTheBoundSpiritsTierIsRefused(GameTestHelper helper) {
        RitualControllerBlockEntity controller = controller(helper, BoundSpirit.Tier.FOLIOT);
        RecipeHolder<RitualRecipe> matrix = OccultismRituals.byId(helper.getLevel(), MATRIX);
        helper.assertTrue(matrix != null, "the Bound Spirit Matrix ritual is not loaded");
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_AFRIT.get()));
        for (Ingredient ingredient : matrix.value().getIngredients()) {
            inputs.add(firstOf(ingredient, helper.getLevel()));
        }
        helper.assertFalse(
                push(helper, MATRIX, inputs, new ItemStack(NepOccultismContent.MATRIX_ITEM.get())),
                "a foliot book let the Controller take an afrit ritual");
        helper.assertTrue(
                controller.refusal() == RitualControllerBlockEntity.Refusal.TIER_TOO_LOW,
                "the refusal was " + controller.refusal() + " rather than the spirit's tier");
        helper.succeed();
    }

    public static void aControllerWithoutABookTakesNoPatterns(GameTestHelper helper) {
        RitualControllerBlockEntity controller = controller(helper, null);
        helper.assertFalse(controller.readyForPatterns(), "a Controller with no book offered to take patterns");
        helper.assertFalse(
                push(helper, CRUSHER_BOOK, crusherBookInputs(), crusherBook(BoundSpirit.Tier.FOLIOT)),
                "a Controller with no book took a ritual");
        helper.succeed();
    }

    public static void standInsAnswerOnlyForTheirOwnSacrifice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(
                SacrificeStandIns.accepts(level, PIGS, new ItemStack(Items.PIG_SPAWN_EGG)),
                "a pig spawn egg did not stand in for a pig");
        helper.assertTrue(
                SacrificeStandIns.accepts(level, PIGS, soulGem(EntityType.PIG)),
                "a soul gem holding a pig was refused");
        helper.assertFalse(
                SacrificeStandIns.accepts(level, PIGS, soulGem(EntityType.COW)), "a soul gem holding a cow stood in");
        helper.assertFalse(
                SacrificeStandIns.accepts(level, PIGS, new ItemStack(OccultismItems.SOUL_GEM_ITEM.get())),
                "an empty soul gem stood in");
        helper.assertFalse(
                SacrificeStandIns.accepts(level, PIGS, new ItemStack(Items.COW_SPAWN_EGG)),
                "a cow spawn egg stood in for a pig");
        ItemStack leftover = SacrificeStandIns.leftover(soulGem(EntityType.PIG));
        helper.assertTrue(
                leftover.is(OccultismItems.SOUL_GEM_ITEM.get()) && !leftover.has(DataComponents.ENTITY_DATA),
                "a spent soul gem did not come back empty");
        helper.assertTrue(
                SacrificeStandIns.leftover(new ItemStack(Items.PIG_SPAWN_EGG)).isEmpty(),
                "a spent spawn egg came back");
        helper.succeed();
    }

    public static void aSacrificeRitualPlansOnlyWithAStandIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeHolder<RitualRecipe> amethyst = OccultismRituals.byId(level, BUDDING_AMETHYST);
        helper.assertTrue(amethyst != null, "Occultism's budding amethyst ritual is not loaded");
        helper.assertTrue(amethyst.value().requiresSacrifice(), "the budding amethyst ritual no longer wants a pig");
        List<GenericStack> inputs = new ArrayList<>();
        inputs.add(GenericStack.fromItemStack(firstOf(amethyst.value().getActivationItem(), helper.getLevel())));
        for (Ingredient ingredient : amethyst.value().getIngredients()) {
            inputs.add(GenericStack.fromItemStack(firstOf(ingredient, helper.getLevel())));
        }
        GenericStack result = GenericStack.fromItemStack(amethyst.value().getResult());
        helper.assertTrue(
                OccultismRituals.plan(level, amethyst.value(), inputs, result) == null,
                "the ritual planned without anything standing in for its pig");
        List<GenericStack> withEgg = new ArrayList<>(inputs);
        withEgg.add(GenericStack.fromItemStack(new ItemStack(Items.PIG_SPAWN_EGG)));
        helper.assertTrue(
                OccultismRituals.plan(level, amethyst.value(), withEgg, result) != null,
                "a pig spawn egg did not complete the ritual's plan");
        helper.assertTrue(
                OccultismRituals.ingredients(level, amethyst.value()) != null,
                "the recipe viewer could not encode the sacrifice ritual");
        helper.succeed();
    }

    public static void aRitualPatternTakesABookUnderAnyName(GameTestHelper helper) {
        List<GenericStack> template = new ArrayList<>();
        for (ItemStack stack : crusherBookInputs()) {
            template.add(GenericStack.fromItemStack(stack));
        }
        IPatternDetails pattern = ritualPattern(helper, CRUSHER_BOOK, template, crusherBook(BoundSpirit.Tier.FOLIOT));
        ItemStack otherBook = new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get());
        ItemNBTUtil.generateBoundSpiritName(otherBook);
        AEItemKey otherName = AEItemKey.of(otherBook);
        boolean accepted = false;
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            if (input.getPossibleInputs()[0].what() instanceof AEItemKey key
                    && key.getItem() == OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get()) {
                helper.assertFalse(key.equals(otherName), "two bound books drew the same spirit name");
                accepted = input.isValid(otherName, helper.getLevel());
                helper.assertFalse(
                        input.isValid(AEItemKey.of(Items.BOOK), helper.getLevel()),
                        "a plain book passed for a bound one");
            }
        }
        helper.assertTrue(accepted, "the pattern turned away a bound book carrying another spirit's name");
        helper.succeed();
    }
}
