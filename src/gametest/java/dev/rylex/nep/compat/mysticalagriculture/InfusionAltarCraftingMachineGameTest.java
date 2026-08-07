package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.init.ModBlocks;
import com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfusionAltarCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_infusion_altar";

    private static final BlockPos ALTAR = new BlockPos(4, 1, 4);

    private static final List<BlockPos> PEDESTAL_OFFSETS = List.of(
            new BlockPos(3, 0, 0),
            new BlockPos(0, 0, 3),
            new BlockPos(-3, 0, 0),
            new BlockPos(0, 0, -3),
            new BlockPos(2, 0, 2),
            new BlockPos(2, 0, -2),
            new BlockPos(-2, 0, 2),
            new BlockPos(-2, 0, -2));

    private InfusionAltarCraftingMachineGameTest() {}

    private static InfusionAltarTileEntity placeAltar(GameTestHelper helper) {
        InfusionAltarCraftingMachine.clearCache();
        MysticalRecipeResolver.clearCache();
        helper.setBlock(ALTAR, ModBlocks.INFUSION_ALTAR.get().defaultBlockState());
        return (InfusionAltarTileEntity) helper.getBlockEntity(ALTAR);
    }

    private static List<InfusionPedestalTileEntity> placePedestals(GameTestHelper helper, int count) {
        List<InfusionPedestalTileEntity> pedestals = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BlockPos pos = ALTAR.offset(PEDESTAL_OFFSETS.get(index));
            helper.setBlock(pos, ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
            pedestals.add((InfusionPedestalTileEntity) helper.getBlockEntity(pos));
        }
        return pedestals;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(ALTAR), null);
        helper.assertTrue(machine != null, "the Infusion Altar exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<IInfusionRecipe> eightPedestalRecipe(GameTestHelper helper) {
        RecipeHolder<IInfusionRecipe> best = null;
        for (RecipeHolder<IInfusionRecipe> holder : MysticalRecipeResolver.infusionCandidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("mysticalagriculture")) {
                continue;
            }
            EncodedIngredients expected = MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel());
            if (expected == null || expected.inputs().size() != MysticalRecipeIngredients.INFUSION_PEDESTALS + 1) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no infusion recipe filling all eight pedestals was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<IInfusionRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded =
                InfusionPattern.encode(holder.id(), inputs, expected.outputs().get(0));
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

    private static void assertNothingStranded(
            GameTestHelper helper, InfusionAltarTileEntity altar, List<InfusionPedestalTileEntity> pedestals) {
        helper.assertTrue(
                altar.getInventory().getStackInSlot(0).isEmpty(), "a refused push left an item in the Infusion Altar");
        for (InfusionPedestalTileEntity pedestal : pedestals) {
            helper.assertTrue(
                    pedestal.getInventory().getStackInSlot(0).isEmpty(),
                    "a refused push left an item on an Infusion Pedestal");
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aFullPedestalArrayStagesTheBaseAndEveryIngredient(GameTestHelper helper) {
        InfusionAltarTileEntity altar = placeAltar(helper);
        List<InfusionPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());

        RecipeHolder<IInfusionRecipe> holder = eightPedestalRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an infusion pattern with all eight pedestals in place was rejected");
        helper.assertTrue(!altar.getInventory().getStackInSlot(0).isEmpty(), "the altar item was not staged");
        for (InfusionPedestalTileEntity pedestal : pedestals) {
            helper.assertTrue(
                    !pedestal.getInventory().getStackInSlot(0).isEmpty(), "an Infusion Pedestal was left empty");
        }
        helper.assertTrue(altar.isActive(), "the altar was loaded but never activated, so the craft cannot start");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aMissingPedestalIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        InfusionAltarTileEntity altar = placeAltar(helper);
        List<InfusionPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size() - 1);

        IPatternDetails details = patternFor(helper, eightPedestalRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Infusion Altar missing a pedestal accepted a pattern");
        assertNothingStranded(helper, altar, pedestals);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anOccupiedPedestalIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        InfusionAltarTileEntity altar = placeAltar(helper);
        List<InfusionPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());
        pedestals.get(0).getInventory().setStackInSlot(0, new ItemStack(Items.STONE));

        IPatternDetails details = patternFor(helper, eightPedestalRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Infusion Altar with an occupied pedestal accepted a pattern");
        helper.assertTrue(
                altar.getInventory().getStackInSlot(0).isEmpty(), "a refused push left an item in the Infusion Altar");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aStagedCraftCompletesInTheAltarsOutputSlot(GameTestHelper helper) {
        InfusionAltarTileEntity altar = placeAltar(helper);
        List<InfusionPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());

        RecipeHolder<IInfusionRecipe> holder = eightPedestalRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "the infusion pattern was rejected");

        ItemStack expected = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.runAfterDelay(160L, () -> {
            helper.assertTrue(
                    ItemStack.isSameItem(altar.getInventory().getStackInSlot(1), expected),
                    "the Infusion Altar did not finish the craft it was handed");
            for (InfusionPedestalTileEntity pedestal : pedestals) {
                helper.assertTrue(
                        pedestal.getInventory().getStackInSlot(0).isEmpty(),
                        "an Infusion Pedestal kept its ingredient after the craft");
            }
            helper.succeed();
        });
    }
}
