package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.init.ModBlocks;
import com.blakebr0.mysticalagriculture.tileentity.AwakeningAltarTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.AwakeningPedestalTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.EssenceVesselTileEntity;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class AwakeningAltarCraftingMachineGameTest {

    private static final BlockPos ALTAR = new BlockPos(4, 1, 4);

    private static final List<BlockPos> PEDESTAL_OFFSETS =
            List.of(new BlockPos(2, 0, 2), new BlockPos(-2, 0, -2), new BlockPos(2, 0, -2), new BlockPos(-2, 0, 2));

    private static final List<BlockPos> VESSEL_OFFSETS =
            List.of(new BlockPos(-3, 0, 0), new BlockPos(3, 0, 0), new BlockPos(0, 0, -3), new BlockPos(0, 0, 3));

    private AwakeningAltarCraftingMachineGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "a_full_altar_array_stages_everything_and_activates",
                        AwakeningAltarCraftingMachineGameTest::aFullAltarArrayStagesEverythingAndActivates)
                .add(
                        "a_missing_vessel_is_refused_without_stranding_items",
                        AwakeningAltarCraftingMachineGameTest::aMissingVesselIsRefusedWithoutStrandingItems)
                .add(
                        "a_missing_pedestal_is_refused_without_stranding_items",
                        AwakeningAltarCraftingMachineGameTest::aMissingPedestalIsRefusedWithoutStrandingItems)
                .add(
                        "a_staged_craft_completes_and_drains_the_vessels",
                        400,
                        AwakeningAltarCraftingMachineGameTest::aStagedCraftCompletesAndDrainsTheVessels);
    }

    private static AwakeningAltarTileEntity placeAltar(GameTestHelper helper) {
        AwakeningAltarCraftingMachine.clearCache();
        MysticalRecipeResolver.clearCache();
        helper.setBlock(ALTAR, ModBlocks.AWAKENING_ALTAR.get().defaultBlockState());
        return helper.getBlockEntity(ALTAR, AwakeningAltarTileEntity.class);
    }

    private static List<AwakeningPedestalTileEntity> placePedestals(GameTestHelper helper, int count) {
        List<AwakeningPedestalTileEntity> pedestals = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BlockPos pos = ALTAR.offset(PEDESTAL_OFFSETS.get(index));
            helper.setBlock(pos, ModBlocks.AWAKENING_PEDESTAL.get().defaultBlockState());
            pedestals.add(helper.getBlockEntity(pos, AwakeningPedestalTileEntity.class));
        }
        return pedestals;
    }

    private static List<EssenceVesselTileEntity> placeVessels(GameTestHelper helper, int count) {
        List<EssenceVesselTileEntity> vessels = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BlockPos pos = ALTAR.offset(VESSEL_OFFSETS.get(index));
            helper.setBlock(pos, ModBlocks.ESSENCE_VESSEL.get().defaultBlockState());
            vessels.add(helper.getBlockEntity(pos, EssenceVesselTileEntity.class));
        }
        return vessels;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(ALTAR), null);
        helper.assertTrue(machine != null, "the Awakening Altar exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<IAwakeningRecipe> simplestRecipe(GameTestHelper helper) {
        RecipeHolder<IAwakeningRecipe> best = null;
        for (RecipeHolder<IAwakeningRecipe> holder : MysticalRecipeResolver.awakeningCandidates(helper.getLevel())) {
            if (MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel()) == null) {
                continue;
            }
            if (best == null || holder.id().identifier().compareTo(best.id().identifier()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no awakening recipe was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<IAwakeningRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = AwakeningPattern.encode(
                holder.id().identifier(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the awakening pattern did not decode");
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

    private static long essenceTotal(RecipeHolder<IAwakeningRecipe> holder) {
        long total = 0;
        for (SizedIngredient essence : holder.value().getEssenceIngredients()) {
            total += essence.count();
        }
        return total;
    }

    private static long vesselTotal(List<EssenceVesselTileEntity> vessels) {
        long total = 0;
        for (EssenceVesselTileEntity vessel : vessels) {
            total += vessel.getInventory().getAmountAsLong(0);
        }
        return total;
    }

    private static void assertNothingStranded(
            GameTestHelper helper,
            AwakeningAltarTileEntity altar,
            List<AwakeningPedestalTileEntity> pedestals,
            List<EssenceVesselTileEntity> vessels) {
        helper.assertTrue(
                MysticalInventories.isEmpty(altar.getInventory(), 0),
                "a refused push left an item in the Awakening Altar");
        for (AwakeningPedestalTileEntity pedestal : pedestals) {
            helper.assertTrue(
                    MysticalInventories.isEmpty(pedestal.getInventory(), 0),
                    "a refused push left an item on an Awakening Pedestal");
        }
        for (EssenceVesselTileEntity vessel : vessels) {
            helper.assertTrue(
                    MysticalInventories.isEmpty(vessel.getInventory(), 0),
                    "a refused push left essence in an Essence Vessel");
        }
    }

    public static void aFullAltarArrayStagesEverythingAndActivates(GameTestHelper helper) {
        AwakeningAltarTileEntity altar = placeAltar(helper);
        List<AwakeningPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());
        List<EssenceVesselTileEntity> vessels = placeVessels(helper, VESSEL_OFFSETS.size());

        RecipeHolder<IAwakeningRecipe> holder = simplestRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an awakening pattern with four pedestals and four vessels in place was rejected");
        helper.assertTrue(!MysticalInventories.isEmpty(altar.getInventory(), 0), "the altar item was not staged");
        for (AwakeningPedestalTileEntity pedestal : pedestals) {
            helper.assertTrue(
                    !MysticalInventories.isEmpty(pedestal.getInventory(), 0), "an Awakening Pedestal was left empty");
        }
        long staged = vesselTotal(vessels);
        long needed = essenceTotal(holder);
        helper.assertTrue(
                staged == needed,
                "the vessels hold " + staged + " essence rather than the " + needed + " the recipe needs");
        helper.assertTrue(altar.isActive(), "the altar was loaded but never activated, so the craft cannot start");
        helper.succeed();
    }

    public static void aMissingVesselIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        AwakeningAltarTileEntity altar = placeAltar(helper);
        List<AwakeningPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());
        List<EssenceVesselTileEntity> vessels = placeVessels(helper, VESSEL_OFFSETS.size() - 1);

        IPatternDetails details = patternFor(helper, simplestRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Awakening Altar missing an Essence Vessel accepted a pattern");
        assertNothingStranded(helper, altar, pedestals, vessels);
        helper.succeed();
    }

    public static void aMissingPedestalIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        AwakeningAltarTileEntity altar = placeAltar(helper);
        List<AwakeningPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size() - 1);
        List<EssenceVesselTileEntity> vessels = placeVessels(helper, VESSEL_OFFSETS.size());

        IPatternDetails details = patternFor(helper, simplestRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Awakening Altar missing a pedestal accepted a pattern");
        assertNothingStranded(helper, altar, pedestals, vessels);
        helper.succeed();
    }

    public static void aStagedCraftCompletesAndDrainsTheVessels(GameTestHelper helper) {
        AwakeningAltarTileEntity altar = placeAltar(helper);
        List<AwakeningPedestalTileEntity> pedestals = placePedestals(helper, PEDESTAL_OFFSETS.size());
        List<EssenceVesselTileEntity> vessels = placeVessels(helper, VESSEL_OFFSETS.size());

        RecipeHolder<IAwakeningRecipe> holder = simplestRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "the awakening pattern was rejected");

        ItemStack expected = MysticalRecipeResolver.resultOf(holder.value());
        helper.runAfterDelay(160L, () -> {
            helper.assertTrue(
                    altar.getInventory().getResource(1).matches(expected),
                    "the Awakening Altar did not finish the craft it was handed");
            for (AwakeningPedestalTileEntity pedestal : pedestals) {
                helper.assertTrue(
                        MysticalInventories.isEmpty(pedestal.getInventory(), 0),
                        "an Awakening Pedestal kept its ingredient after the craft");
            }
            helper.assertTrue(
                    vesselTotal(vessels) == 0, "the craft left " + vesselTotal(vessels) + " essence in the vessels");
            helper.succeed();
        });
    }
}
