package dev.rylex.nep.compat.ars;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.lib.RitualLib;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;

final class ArsFixtures {

    static final BlockPos CONTROLLER = new BlockPos(4, 1, 4);
    static final BlockPos ENERGY = CONTROLLER.above();
    static final BlockPos DRIVE = CONTROLLER.north();
    static final BlockPos CONSUMER = CONTROLLER.south();

    private static final int BOOT_TICKS = 60;
    private static final int CELL_TICKS = 20;
    private static final int SETTLE_TICKS = 2;

    private ArsFixtures() {}

    static GameTestSequence poweredNetwork(GameTestHelper helper, Runnable placeConsumer, List<GenericStack> stock) {
        return helper.startSequence()
                .thenExecute(() -> {
                    helper.setBlock(CONTROLLER, AEBlocks.CONTROLLER.block());
                    helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
                    helper.setBlock(DRIVE, AEBlocks.DRIVE.block());
                    placeConsumer.run();
                })
                .thenIdle(BOOT_TICKS)
                .thenExecute(() -> {
                    DriveBlockEntity drive = helper.getBlockEntity(DRIVE);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_1K.stack());
                })
                .thenIdle(CELL_TICKS)
                .thenExecute(() -> {
                    MEStorage storage = storage(helper);
                    for (GenericStack entry : stock) {
                        long inserted = storage.insert(entry.what(), entry.amount(), Actionable.MODULATE, source());
                        helper.assertTrue(
                                inserted == entry.amount(),
                                "the network took " + inserted + " of " + entry.amount() + " " + entry.what());
                    }
                })
                .thenIdle(SETTLE_TICKS);
    }

    static MEStorage storage(GameTestHelper helper) {
        DriveBlockEntity drive = helper.getBlockEntity(DRIVE);
        IGrid grid = drive.getMainNode().getGrid();
        helper.assertTrue(grid != null, "the network never formed");
        return grid.getStorageService().getInventory();
    }

    static long stored(GameTestHelper helper, AEKey key) {
        return storage(helper).extract(key, Long.MAX_VALUE, Actionable.SIMULATE, source());
    }

    static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    static ResourceLocation sanctuary() {
        return ArsNouveau.prefix(RitualLib.SANCTUARY);
    }

    private static IActionSource source() {
        return IActionSource.empty();
    }
}
