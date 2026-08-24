package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblySharedOutputGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_shared_output";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos DEPLOYER = new BlockPos(1, 1, 2);
    private static final BlockPos PRESS = new BlockPos(3, 1, 2);
    private static final BlockPos FEED = new BlockPos(0, 1, 2);
    private static final BlockPos DEPOT = new BlockPos(4, 1, 2);

    private static final ResourceLocation IRON_RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "shared_output_assembly_a");
    private static final ResourceLocation GOLD_RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "shared_output_assembly_b");

    private SequencedAssemblySharedOutputGameTest() {}

    private static SequencedAssemblyControllerBlockEntity line(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, NepCreateContent.CONTROLLER.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(
                be instanceof SequencedAssemblyControllerBlockEntity, "the controller did not create its block entity");
        helper.setBlock(
                DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(PRESS, AllBlocks.MECHANICAL_PRESS.getDefaultState());
        helper.setBlock(FEED, AllBlocks.DEPOT.getDefaultState());
        helper.setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState());

        SequencedAssemblyControllerBlockEntity controller = (SequencedAssemblyControllerBlockEntity) be;
        controller.applyPlan(
                helper.absolutePos(FEED),
                helper.absolutePos(DEPOT),
                List.of(helper.absolutePos(DEPLOYER), helper.absolutePos(PRESS)));
        return controller;
    }

    private static void push(GameTestHelper helper, ResourceLocation recipe, Item base) {
        helper.assertTrue(
                SequencedAssemblyResolver.resolveById(helper.getLevel(), recipe) != null,
                "the shared-output test recipe " + recipe + " did not load");

        ItemStack encoded = SequencedAssemblyPattern.encode(
                recipe,
                List.of(new GenericStack(AEItemKey.of(base), 1), new GenericStack(AEItemKey.of(Items.FLINT), 1)),
                new GenericStack(AEItemKey.of(Items.NETHER_STAR), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the shared-output pattern did not decode");

        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CONTROLLER), null);
        helper.assertTrue(machine != null, "the controller exposed no crafting machine capability");

        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        helper.assertTrue(
                machine.pushPattern(details, inputs, Direction.UP), "the controller refused the pattern for " + recipe);
    }

    private static boolean fedWith(GameTestHelper helper, Item base) {
        IItemHandler feed =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(FEED), null);
        if (feed == null) {
            return false;
        }
        for (int slot = 0; slot < feed.getSlots(); slot++) {
            if (feed.getStackInSlot(slot).is(base)) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aPatternFeedsTheBaseItemOfItsOwnRecipe(GameTestHelper helper) {
        line(helper);
        push(helper, GOLD_RECIPE, Items.GOLD_INGOT);

        helper.succeedWhen(() -> helper.assertTrue(
                fedWith(helper, Items.GOLD_INGOT),
                "the controller never fed the base item of the recipe the pattern names; a rival recipe for the same "
                        + "output used to win, leaving the pattern's own base item stranded in the buffer"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aRivalRecipeForTheSameOutputDoesNotTakeOver(GameTestHelper helper) {
        line(helper);
        push(helper, IRON_RECIPE, Items.IRON_INGOT);

        helper.succeedWhen(() -> helper.assertTrue(
                fedWith(helper, Items.IRON_INGOT),
                "the controller never fed the base item of the recipe the pattern names; a rival recipe for the same "
                        + "output used to win, leaving the pattern's own base item stranded in the buffer"));
    }
}
