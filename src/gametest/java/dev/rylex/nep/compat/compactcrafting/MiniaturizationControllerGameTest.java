package dev.rylex.nep.compat.compactcrafting;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import dev.compactmods.crafting.api.field.IMiniaturizationField;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.compactmods.crafting.core.CCBlocks;
import dev.compactmods.crafting.projector.FieldProjectorBlock;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MiniaturizationControllerGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_miniaturization_controller";

    /** A small field spans nine blocks projector to projector, which is exactly the template's footprint. */
    private static final BlockPos FIELD_CENTER = new BlockPos(4, 2, 4);

    private static final BlockPos WEST_PROJECTOR = new BlockPos(0, 2, 4);
    private static final BlockPos EAST_PROJECTOR = new BlockPos(8, 2, 4);
    private static final BlockPos NORTH_PROJECTOR = new BlockPos(4, 2, 0);
    private static final BlockPos SOUTH_PROJECTOR = new BlockPos(4, 2, 8);

    private static final BlockPos CONTROLLER = new BlockPos(0, 3, 4);
    private static final BlockPos ENERGY_CELL = new BlockPos(0, 4, 4);

    private MiniaturizationControllerGameTest() {}

    private static void placeProjector(
            GameTestHelper helper, BlockPos pos, Direction facing, MiniaturizationFieldSize size) {
        helper.setBlock(
                pos,
                CCBlocks.FIELD_PROJECTOR_BLOCK
                        .get()
                        .defaultBlockState()
                        .setValue(FieldProjectorBlock.FACING, facing)
                        .setValue(FieldProjectorBlock.SIZE, size));
    }

    /**
     * A projector only forms its field when it is placed already carrying a size, so the first three go in inactive and
     * the last one activates the set.
     */
    private static IMiniaturizationField<MiniaturizationRecipe> formField(GameTestHelper helper) {
        placeProjector(helper, WEST_PROJECTOR, Direction.EAST, MiniaturizationFieldSize.INACTIVE);
        placeProjector(helper, NORTH_PROJECTOR, Direction.SOUTH, MiniaturizationFieldSize.INACTIVE);
        placeProjector(helper, SOUTH_PROJECTOR, Direction.NORTH, MiniaturizationFieldSize.INACTIVE);
        placeProjector(helper, EAST_PROJECTOR, Direction.WEST, MiniaturizationFieldSize.SMALL);

        IMiniaturizationField<MiniaturizationRecipe> field =
                FieldBinding.fieldAt(helper.getLevel(), helper.absolutePos(FIELD_CENTER));
        helper.assertTrue(
                field != null,
                "the four projectors did not register a field at " + FIELD_CENTER + "; the rest of this test is moot");
        return field;
    }

    private static MiniaturizationControllerBlockEntity place(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, NepCompactCraftingContent.CONTROLLER.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(
                be instanceof MiniaturizationControllerBlockEntity, "the controller did not create its block entity");
        return (MiniaturizationControllerBlockEntity) be;
    }

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static RecipeHolder<MiniaturizationRecipe> smallFieldRecipe(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> best = null;
        for (RecipeHolder<MiniaturizationRecipe> candidate :
                MiniaturizationRecipeResolver.candidates(helper.getLevel())) {
            if (!candidate.value().fitsInFieldSize(MiniaturizationFieldSize.SMALL)
                    || MiniaturizationRecipeIngredients.miniaturization(candidate, helper.getLevel()) == null
                    || MiniaturizationLayout.plan(candidate.value(), BlockPos.ZERO) == null) {
                continue;
            }
            if (best == null
                    || candidate.value().getCraftingTime() < best.value().getCraftingTime()) {
                best = candidate;
            }
        }
        helper.assertTrue(best != null, "no miniaturization recipe fits a small field, so this test proves nothing");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<MiniaturizationRecipe> holder) {
        EncodedIngredients expected = MiniaturizationRecipeIngredients.miniaturization(holder, helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            inputs.add(slot.get(0));
        }
        ItemStack encoded = MiniaturizationPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the miniaturization pattern did not decode");
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

    private static List<MiniaturizationLayout.Placement> layoutFor(
            GameTestHelper helper,
            RecipeHolder<MiniaturizationRecipe> holder,
            IMiniaturizationField<MiniaturizationRecipe> field) {
        BlockPos anchor = MiniaturizationLayout.anchorFor(holder.value(), field.getCenter());
        List<MiniaturizationLayout.Placement> plan = MiniaturizationLayout.plan(holder.value(), anchor);
        helper.assertTrue(plan != null, "the recipe " + holder.id() + " produced no layout");
        return plan;
    }

    private static int blocksInField(GameTestHelper helper, IMiniaturizationField<MiniaturizationRecipe> field) {
        int total = 0;
        for (BlockPos pos : MiniaturizationLayout.blocksIn(field.getBounds())) {
            if (!helper.getLevel().isEmptyBlock(pos)) {
                total++;
            }
        }
        return total;
    }

    private static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.is(item)) {
                total += held.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theControllerIsBuiltByARealMiniaturizationCraft(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder =
                MiniaturizationRecipeResolver.resolveById(helper.getLevel(), Nep.id("miniaturization_controller"));
        helper.assertTrue(holder != null, "nep:miniaturization_controller did not load as a miniaturization recipe");
        ItemStack[] outputs = holder.value().getOutputs();
        helper.assertTrue(
                outputs.length == 1
                        && ItemStack.isSameItem(
                                outputs[0], new ItemStack(NepCompactCraftingContent.CONTROLLER_ITEM.get())),
                "the nep:miniaturization_controller recipe does not produce a Miniaturization Controller");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void exposesItsMachineCapabilities(GameTestHelper helper) {
        place(helper);
        BlockPos pos = helper.absolutePos(CONTROLLER);

        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, pos, null) != null,
                "the controller exposed no crafting machine capability");
        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null,
                "the controller exposed no in-world grid node host, so it can never join a network");
        helper.assertTrue(
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null,
                "the controller exposed no item handler, so nothing can restock it by hand");
        helper.succeed();
    }

    /**
     * A signal on any projector switches the whole field off, and a full block is a redstone conductor unless it says
     * otherwise, so a conductive controller would let a repeater beside it cancel every craft.
     */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void doesNotConductRedstoneIntoTheProjectorItTouches(GameTestHelper helper) {
        place(helper);
        BlockPos pos = helper.absolutePos(CONTROLLER);
        helper.assertTrue(
                !helper.getBlockState(CONTROLLER).isRedstoneConductor(helper.getLevel(), pos),
                "the controller conducts redstone, so strongly powering it would disable the field it drives");
        helper.assertTrue(
                helper.getBlockState(CONTROLLER).getSignal(helper.getLevel(), pos, Direction.DOWN) == 0,
                "the controller emits a redstone signal, which would disable the field it drives");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void refusesPatternsWithNoFieldToDrive(GameTestHelper helper) {
        MiniaturizationControllerBlockEntity controller = place(helper);
        helper.assertTrue(
                !controller.readyForPatterns(),
                "a controller touching no projector still accepted plans, so jobs would be routed into a machine that "
                        + "cannot run them");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void bindsToTheFieldOfTheProjectorItTouches(GameTestHelper helper) {
        formField(helper);
        MiniaturizationControllerBlockEntity controller = place(helper);

        helper.assertTrue(
                controller.boundFieldSize() == MiniaturizationFieldSize.SMALL,
                "the controller read the field size as " + controller.boundFieldSize() + " instead of SMALL");
        helper.assertTrue(controller.readyForPatterns(), "a controller bound to a live field refused to accept plans");
        helper.succeed();
    }

    /** Adjacency is what keeps the controller from being counted as a filled block and cleared with the recipe. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void sitsOutsideTheFieldItDrives(GameTestHelper helper) {
        IMiniaturizationField<MiniaturizationRecipe> field = formField(helper);
        place(helper);

        AABB bounds = field.getBounds();
        helper.assertTrue(
                !bounds.contains(Vec3.atCenterOf(helper.absolutePos(CONTROLLER))),
                "the controller stands inside the field bounds " + bounds + ", where the field would consume it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theLayoutItBuildsFitsInsideTheField(GameTestHelper helper) {
        IMiniaturizationField<MiniaturizationRecipe> field = formField(helper);
        RecipeHolder<MiniaturizationRecipe> holder = smallFieldRecipe(helper);

        AABB bounds = field.getBounds();
        for (MiniaturizationLayout.Placement placement : layoutFor(helper, holder, field)) {
            helper.assertTrue(
                    bounds.contains(Vec3.atCenterOf(placement.pos())),
                    "the layout for " + holder.id() + " puts a block at " + placement.pos() + ", outside the field "
                            + bounds);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUnnetworkedControllerNeverBuildsIntoTheField(GameTestHelper helper) {
        IMiniaturizationField<MiniaturizationRecipe> field = formField(helper);
        MiniaturizationControllerBlockEntity controller = place(helper);
        IPatternDetails details = patternFor(helper, smallFieldRecipe(helper));

        helper.assertTrue(
                controller.pushControllerPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected by an idle controller");

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    MiniaturizationLayout.isClear(helper.getLevel(), field.getBounds()),
                    "a controller with no ME network built into the field; it must stay idle until it is on a powered "
                            + "grid");
            helper.succeed();
        });
    }

    /**
     * A layout that snapped into place in a single tick would never be seen going up, and a field the size of a house
     * would fill faster than the projectors could show it.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void buildsItsLayoutOneBlockAtATime(GameTestHelper helper) {
        IMiniaturizationField<MiniaturizationRecipe> field = formField(helper);
        RecipeHolder<MiniaturizationRecipe> holder = smallFieldRecipe(helper);
        int size = layoutFor(helper, holder, field).size();
        helper.assertTrue(
                size > 2,
                "the layout for " + holder.id() + " is only " + size
                        + " block(s), too few to tell a gradual build from an instant one");

        MiniaturizationControllerBlockEntity controller = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                controller.pushControllerPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected");

        helper.succeedWhen(() -> {
            int placed = blocksInField(helper, field);
            helper.assertTrue(
                    placed > 0 && placed < size,
                    "the field never held a partial layout, so the controller placed all " + size
                            + " blocks in one tick (phase " + controller.phase() + ", " + controller.builtBlocks()
                            + " built, " + placed + " standing)");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void aPoweredControllerDrivesTheFieldToCompletion(GameTestHelper helper) {
        IMiniaturizationField<MiniaturizationRecipe> field = formField(helper);
        RecipeHolder<MiniaturizationRecipe> holder = smallFieldRecipe(helper);

        MiniaturizationControllerBlockEntity controller = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = holder.value().getOutputs()[0];

        helper.assertTrue(
                controller.pushControllerPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected");

        int ticks = holder.value().getCraftingTime()
                + layoutFor(helper, holder, field).size()
                + 200;
        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(
                    countIn(controller.getOutputBuffer(), wanted.getItem()) >= wanted.getCount(),
                    "a powered controller produced nothing after " + ticks + " ticks for " + holder.id() + " (phase "
                            + controller.phase() + ", stall " + controller.stall() + ", refusal " + controller.refusal()
                            + ")");
            helper.assertTrue(!controller.hasPending(), "the controller still owes a craft it was only asked for once");
            helper.assertTrue(
                    MiniaturizationLayout.isClear(helper.getLevel(), field.getBounds()),
                    "the field still holds blocks after the craft finished");
            helper.succeed();
        });
    }

    /** A layout the field cannot match costs nothing: the blocks come back out and the ingredients are re-buffered. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aFieldWithBlocksAlreadyInItIsLeftAlone(GameTestHelper helper) {
        formField(helper);
        MiniaturizationControllerBlockEntity controller = place(helper);
        powerUp(helper);
        helper.setBlock(FIELD_CENTER, Blocks.BEDROCK.defaultBlockState());
        IPatternDetails details = patternFor(helper, smallFieldRecipe(helper));

        helper.assertTrue(
                controller.pushControllerPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected");

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    controller.stall() == MiniaturizationControllerBlockEntity.Stall.FIELD_OCCUPIED,
                    "the controller reported " + controller.stall()
                            + " instead of FIELD_OCCUPIED for a field with a block standing in it");
            helper.assertTrue(
                    helper.getBlockState(FIELD_CENTER).is(Blocks.BEDROCK),
                    "the controller built over a block that was already in the field");
            helper.succeed();
        });
    }
}
