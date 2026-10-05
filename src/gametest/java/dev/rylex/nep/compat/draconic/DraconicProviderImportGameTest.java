package dev.rylex.nep.compat.draconic;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.provider.ImportUpgradeHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DraconicProviderImportGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_dfa_import";

    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    private static final BlockPos PROVIDER = new BlockPos(4, 1, 4);
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 4);
    private static final BlockPos ENERGY = new BlockPos(5, 1, 4);
    private static final BlockPos DRIVE = new BlockPos(4, 1, 3);

    private record InjectorSite(BlockPos pos, Direction facing) {}

    private static final List<InjectorSite> SITES = List.of(
            new InjectorSite(new BlockPos(1, 2, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 2, 4), Direction.WEST),
            new InjectorSite(new BlockPos(4, 2, 1), Direction.SOUTH),
            new InjectorSite(new BlockPos(4, 2, 7), Direction.NORTH),
            new InjectorSite(new BlockPos(1, 3, 4), Direction.EAST),
            new InjectorSite(new BlockPos(1, 1, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 3, 4), Direction.WEST),
            new InjectorSite(new BlockPos(7, 1, 4), Direction.WEST));

    private DraconicProviderImportGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aDraconicProviderCollectsWhatItsFusionCoreMade(GameTestHelper helper) {
        var level = helper.getLevel();
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();
        RecipeHolder<IFusionRecipe> consumer =
                FusionRecipeResolver.resolveByOutputItem(level, DEContent.PICKAXE_WYVERN.get());
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(consumer, level);
        helper.assertTrue(expected != null, "the chaotic pickaxe recipe produced no ingredient list");

        var provider = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
                "ae2_draconic_fusion_autocrafter", "me_draconic_pattern_provider"));

        helper.startSequence()
                .thenExecute(() -> {
                    helper.setBlock(CONTROLLER, AEBlocks.CONTROLLER.block());
                    helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
                    helper.setBlock(DRIVE, AEBlocks.DRIVE.block());
                    helper.setBlock(PROVIDER, provider);
                    helper.setBlock(CORE, DEContent.CRAFTING_CORE.get().defaultBlockState());
                    for (InjectorSite site : SITES) {
                        helper.setBlock(
                                site.pos(),
                                DEContent.WYVERN_CRAFTING_INJECTOR
                                        .get()
                                        .defaultBlockState()
                                        .setValue(CraftingInjector.FACING, site.facing()));
                    }
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    DriveBlockEntity drive = helper.getBlockEntity(DRIVE);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_1K.stack());
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    ((ImportUpgradeHost) be).nepImportUpgrades().addItems(new ItemStack(NepItems.IMPORT_CARD.get()));
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    List<GenericStack> inputs = new ArrayList<>();
                    for (List<GenericStack> options : expected.inputs()) {
                        inputs.add(options.get(0));
                    }
                    ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                            inputs, List.of(expected.outputs().get(0)));
                    IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
                    helper.assertTrue(details != null, "the pattern did not decode");
                    KeyCounter[] pushed = new KeyCounter[details.getInputs().length];
                    for (int slot = 0; slot < pushed.length; slot++) {
                        pushed[slot] = new KeyCounter();
                        IPatternDetails.IInput input = details.getInputs()[slot];
                        pushed[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
                    }
                    helper.assertTrue(be.getLogic().pushPattern(details, pushed), "the provider refused the push");
                    TileFusionCraftingCore core = helper.getBlockEntity(CORE);
                    helper.assertTrue(core.isCrafting(), "the core never started crafting");
                    core.setFusionState(IFusionStateMachine.FusionState.CRAFTING);
                    core.setCounter(Integer.MAX_VALUE - 1);
                    core.tick();
                    helper.assertTrue(!core.isCrafting(), "the core never finished");
                    helper.assertTrue(!core.getOutputStack().isEmpty(), "the core made nothing");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    TileFusionCraftingCore core = helper.getBlockEntity(CORE);
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    IGrid grid = be.getMainNode().getGrid();
                    helper.assertTrue(grid != null, "the provider never joined a grid");
                    long inNetwork = grid.getStorageService()
                            .getInventory()
                            .extract(
                                    AEItemKey.of(DEContent.PICKAXE_WYVERN.get()),
                                    Long.MAX_VALUE,
                                    Actionable.SIMULATE,
                                    IActionSource.empty());
                    helper.assertTrue(
                            core.getOutputStack().isEmpty()
                                    && inNetwork == expected.outputs().get(0).amount(),
                            "the result was not imported: core holds " + core.getOutputStack() + ", network holds "
                                    + inNetwork);
                })
                .thenSucceed();
    }
}
