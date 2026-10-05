package dev.rylex.nep.compat.ars;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.block.tile.ArcanePedestalTile;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImbuementCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_ars_imbuement";

    private static final BlockPos CHAMBER = new BlockPos(4, 1, 4);
    private static final BlockPos JAR = CHAMBER.above();

    private static final List<BlockPos> PEDESTAL_SPOTS = List.of(
            new BlockPos(3, 1, 4),
            new BlockPos(5, 1, 4),
            new BlockPos(4, 1, 3),
            new BlockPos(4, 1, 5),
            new BlockPos(3, 1, 3),
            new BlockPos(5, 1, 5),
            new BlockPos(3, 1, 5),
            new BlockPos(5, 1, 3));

    private static final int MAX_JAR_SOURCE = 9_000;

    private ImbuementCraftingMachineGameTest() {}

    private record Fixture(RecipeHolder<ImbuementRecipe> holder, IPatternDetails details, EncodedIngredients expected) {

        List<ItemStack> pedestalStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            for (int index = 1; index < expected.inputs().size(); index++) {
                stacks.add(
                        ArsRecipeResolver.toStack(expected.inputs().get(index).get(0)));
            }
            return stacks;
        }
    }

    private static Fixture fixture(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<RecipeHolder<ImbuementRecipe>> candidates = ArsRecipeResolver.imbuementCandidates(level).stream()
                .filter(holder -> holder.value().getClass() == ImbuementRecipe.class
                        && holder.id().getNamespace().equals("ars_nouveau")
                        && holder.value().getPedestalItems().size() <= PEDESTAL_SPOTS.size())
                .sorted(Comparator.comparingInt((RecipeHolder<ImbuementRecipe> holder) ->
                                holder.value().getSource())
                        .thenComparingInt(
                                holder -> holder.value().getPedestalItems().size())
                        .thenComparing(holder -> holder.id().toString()))
                .toList();
        for (RecipeHolder<ImbuementRecipe> holder : candidates) {
            EncodedIngredients expected = ArsRecipeIngredients.imbuement(holder.value(), level);
            if (expected == null || expected.inputs().isEmpty()) {
                continue;
            }
            ItemStack encoded = ImbuementPattern.encode(
                    holder.id(),
                    List.of(expected.inputs().get(0).get(0)),
                    expected.outputs().get(0));
            IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
            if (details != null && ArsRecipeResolver.resolveImbuement(details, level) != null) {
                return new Fixture(holder, details, expected);
            }
        }
        helper.fail("no imbuement recipe could be encoded and resolved");
        return null;
    }

    private static ImbuementTile placeChamber(GameTestHelper helper, Fixture fixture, List<ItemStack> pedestalItems) {
        helper.setBlock(CHAMBER, BlockRegistry.IMBUEMENT_BLOCK.get().defaultBlockState());
        for (int index = 0; index < pedestalItems.size(); index++) {
            helper.setBlock(
                    PEDESTAL_SPOTS.get(index),
                    BlockRegistry.ARCANE_PEDESTAL.get().defaultBlockState());
            ArcanePedestalTile pedestal = helper.getBlockEntity(PEDESTAL_SPOTS.get(index));
            pedestal.setStack(pedestalItems.get(index).copy());
        }
        helper.setBlock(JAR, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        SourceJarTile jar = helper.getBlockEntity(JAR);
        jar.addSource(Math.min(
                MAX_JAR_SOURCE, Math.max(1_000, fixture.holder().value().getSource() * 2)));
        return helper.getBlockEntity(CHAMBER);
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CHAMBER), null);
        helper.assertTrue(machine != null, "the imbuement chamber exposed no crafting machine capability");
        return machine;
    }

    private static void assertPedestalsHold(GameTestHelper helper, List<ItemStack> expected) {
        for (int index = 0; index < expected.size(); index++) {
            ArcanePedestalTile pedestal = helper.getBlockEntity(PEDESTAL_SPOTS.get(index));
            helper.assertTrue(
                    ItemStack.isSameItemSameComponents(pedestal.getStack(), expected.get(index)),
                    "pedestal " + index + " holds " + pedestal.getStack() + " instead of " + expected.get(index));
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aReagentOnlyPatternIsAcceptedOverHandPlacedPedestals(GameTestHelper helper) {
        ImbuementCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        ImbuementTile chamber = placeChamber(helper, fixture, fixture.pedestalStacks());

        helper.assertTrue(
                machine(helper).pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the chamber refused a reagent-only pattern over correctly stocked pedestals for "
                        + fixture.holder().id());
        helper.assertTrue(!chamber.getItem(0).isEmpty(), "an accepted push left the chamber empty");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 700)
    public static void theImbuementCompletesAndThePedestalItemsRemain(GameTestHelper helper) {
        ImbuementCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        List<ItemStack> pedestalItems = fixture.pedestalStacks();
        ImbuementTile chamber = placeChamber(helper, fixture, pedestalItems);
        GenericStack result = fixture.expected().outputs().get(0);

        helper.assertTrue(
                machine(helper).pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the chamber refused " + fixture.holder().id());
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    result.what().equals(AEItemKey.of(chamber.getItem(0)))
                            && chamber.getItem(0).getCount() == result.amount(),
                    "the chamber holds " + chamber.getItem(0) + " rather than " + result);
            assertPedestalsHold(helper, pedestalItems);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void wrongItemsOnThePedestalsRefuseThePush(GameTestHelper helper) {
        ImbuementCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        List<ItemStack> wrong = new ArrayList<>();
        for (int index = 0; index < fixture.pedestalStacks().size(); index++) {
            wrong.add(new ItemStack(Items.DIRT));
        }
        ImbuementTile chamber = placeChamber(helper, fixture, wrong);

        helper.assertTrue(
                !machine(helper).pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the chamber accepted a pattern while its pedestals held dirt");
        helper.assertTrue(chamber.getItem(0).isEmpty(), "a refused push left an item in the chamber");
        helper.succeed();
    }
}
