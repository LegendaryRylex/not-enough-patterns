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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DepotCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_depot";
    private static final String MEMO_BATCH = "nep_depot_memo";
    private static final String RETRY_BATCH = "nep_depot_retry";

    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);

    private DepotCraftingMachineGameTest() {}

    private static ICraftingMachine machine(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState());
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(DEPOT), null);
        helper.assertTrue(machine != null, "the depot exposed no crafting machine capability");
        return machine;
    }

    private static void placeDeployerAbove(GameTestHelper helper) {
        helper.setBlock(
                DEPOT.above(2),
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
    }

    private static IPatternDetails unmatchablePattern(GameTestHelper helper) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");
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

    @GameTest(template = TEMPLATE, batch = MEMO_BATCH)
    public static void aPatternNoRecipeCanSatisfyIsOnlyEvaluatedOnce(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = machine(helper);
        placeDeployerAbove(helper);

        IPatternDetails details = unmatchablePattern(helper);
        KeyCounter[] inputs = inputsOf(details);

        helper.assertTrue(
                !machine.pushPattern(details, inputs, Direction.NORTH), "an unmatchable pattern was accepted");
        helper.assertTrue(
                DepotCraftingMachine.memoisedRejectCount() == 1,
                "an unmatchable pattern was not memoised, so AE2 re-pushing it every tick would keep re-resolving "
                        + "recipes and re-logging forever");

        for (int push = 0; push < 20; push++) {
            helper.assertTrue(
                    !machine.pushPattern(details, inputs, Direction.NORTH),
                    "a memoised pattern was later accepted on push " + push);
        }
        helper.assertTrue(
                DepotCraftingMachine.memoisedRejectCount() == 1,
                "repeated pushes of the same dead pattern grew the memo");

        DepotCraftingMachine.clearCache();
        helper.assertTrue(DepotCraftingMachine.memoisedRejectCount() == 0, "a datapack reload did not clear the memo");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = RETRY_BATCH)
    public static void aDepotWithNoMachineAboveKeepsRetrying(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = machine(helper);

        IPatternDetails details = unmatchablePattern(helper);
        KeyCounter[] inputs = inputsOf(details);

        helper.assertTrue(
                !machine.pushPattern(details, inputs, Direction.NORTH),
                "a depot with nothing above it accepted a pattern");
        helper.assertTrue(
                DepotCraftingMachine.memoisedRejectCount() == 0,
                "a missing Deployer or Spout was memoised as permanent; the player can still build one");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anEmptyDepotAdvertisesItselfToPatternProviders(GameTestHelper helper) {
        ICraftingMachine machine = machine(helper);
        placeDeployerAbove(helper);

        helper.assertTrue(machine.acceptsPlans(), "a depot under a deployer refused to advertise for plans");
        helper.assertTrue(
                machine.getCraftingMachineInfo()
                        .name()
                        .equals(AllBlocks.DEPLOYER.get().getName()),
                "the depot did not report itself as a deployer to the pattern provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aBareDepotRefusesPlans(GameTestHelper helper) {
        ICraftingMachine machine = machine(helper);

        helper.assertTrue(!machine.acceptsPlans(), "a depot with nothing above it advertised for plans");
        helper.succeed();
    }
}
