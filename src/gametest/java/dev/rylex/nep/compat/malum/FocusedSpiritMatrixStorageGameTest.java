package dev.rylex.nep.compat.malum;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.MatrixGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixStorageGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_storage";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private static final int UMBRAL_SLOT = 4;
    private static final int BANKED = 20;

    private FocusedSpiritMatrixStorageGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        SpiritInfusionResolver.clearCache();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    private static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static MatrixGridNode node(GameTestHelper helper, FocusedSpiritMatrixBlockEntity matrix) {
        helper.assertTrue(
                matrix.gridNodeHost() instanceof MatrixGridNode, "the Matrix is not backed by a matrix grid node");
        return (MatrixGridNode) matrix.gridNodeHost();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theBankIsReadableAsNetworkStorage(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        SpiritBankStorage storage = new SpiritBankStorage(matrix);
        AEItemKey umbral = AEItemKey.of(MalumItems.UMBRAL_SPIRIT.get());
        IActionSource outside = IActionSource.empty();

        helper.assertValueEqual(
                storage.insert(umbral, BANKED, Actionable.MODULATE, outside),
                (long) BANKED,
                "the spirits the network managed to store in the bank");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == BANKED,
                "spirits inserted through the network never reached the bank");

        KeyCounter listed = new KeyCounter();
        storage.getAvailableStacks(listed);
        helper.assertValueEqual(listed.get(umbral), (long) BANKED, "the spirits the bank advertises to the network");

        helper.assertValueEqual(
                storage.extract(umbral, 5, Actionable.SIMULATE, outside),
                5L,
                "the spirits a simulated extraction reports");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == BANKED,
                "a simulated extraction took spirits out of the bank");

        helper.assertValueEqual(
                storage.extract(umbral, 5, Actionable.MODULATE, outside), 5L, "the spirits a real extraction reports");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == BANKED - 5,
                "a real extraction did not take the spirits it reported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theBankRefusesToFeedTheMatrixItBelongsTo(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        SpiritBankStorage storage = new SpiritBankStorage(matrix);
        AEItemKey umbral = AEItemKey.of(MalumItems.UMBRAL_SPIRIT.get());
        storage.insert(umbral, BANKED, Actionable.MODULATE, IActionSource.empty());

        IActionSource itself = IActionSource.ofMachine(node(helper, matrix));
        helper.assertValueEqual(
                storage.extract(umbral, BANKED, Actionable.SIMULATE, itself),
                0L,
                "the spirits the Matrix is allowed to draw out of its own bank; anything above zero lets a craft "
                        + "source its ingredients from the buffer it is about to spend");
        helper.assertValueEqual(
                storage.extract(umbral, BANKED, Actionable.MODULATE, itself),
                0L,
                "the spirits the Matrix actually drew out of its own bank");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == BANKED,
                "the Matrix drained its own bank through the network");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theBankTakesNothingButSpirits(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        SpiritBankStorage storage = new SpiritBankStorage(matrix);
        IActionSource outside = IActionSource.empty();

        helper.assertValueEqual(
                storage.insert(AEItemKey.of(Items.DIAMOND), 4, Actionable.MODULATE, outside),
                0L,
                "what the bank stored of an item that is not a spirit");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), Items.DIAMOND) == 0, "the bank stored something that is not a spirit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theBankAndItsUpgradesSurviveASaveAndLoad(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int obelisks = FocusedSpiritMatrixUpgrades.maxObelisks();
        int catalyzers = FocusedSpiritMatrixUpgrades.maxCatalyzers();
        int wear = 7;

        matrix.getSpiritBank().insertItem(UMBRAL_SLOT, new ItemStack(MalumItems.UMBRAL_SPIRIT.get(), BANKED), false);
        matrix.getUpgradeSlot().insertItem(0, new ItemStack(MalumBlocks.RUNEWOOD_OBELISK.get(), obelisks), false);
        matrix.getCatalyzerSlot()
                .insertItem(0, new ItemStack(NepMalumContent.MATRIX_CATALYZER.get(), catalyzers), false);
        ItemStack impetus = new ItemStack(NepMalumContent.MATRIX_IMPETUS.get());
        impetus.setDamageValue(wear);
        matrix.getImpetusSlot().insertItem(0, impetus, false);
        matrix.toggleSpiritRestock();
        int craftTicks = matrix.craftTicks();

        CompoundTag saved = matrix.saveWithFullMetadata(helper.getLevel().registryAccess());
        FocusedSpiritMatrixBlockEntity reloaded = new FocusedSpiritMatrixBlockEntity(
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                helper.absolutePos(MATRIX),
                NepMalumContent.MATRIX.get().defaultBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(
                (long) countIn(reloaded.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()),
                (long) BANKED,
                "the spirits read back from disk");
        helper.assertValueEqual(
                (long) reloaded.getUpgradeSlot().getStackInSlot(0).getCount(),
                (long) obelisks,
                "the obelisks read back from disk");
        helper.assertValueEqual(
                (long) reloaded.catalyzerCount(), (long) catalyzers, "the catalyzers read back from disk");
        helper.assertValueEqual(
                (long) reloaded.getImpetusSlot().getStackInSlot(0).getDamageValue(),
                (long) wear,
                "the wear on the Matrix Impetus read back from disk");
        helper.assertTrue(!reloaded.spiritRestock(), "Restock Spirits came back on after a save and load");
        helper.assertValueEqual(
                (long) reloaded.craftTicks(),
                (long) craftTicks,
                "the craft time a reloaded Matrix runs at; obelisks that do not re-apply on load leave it slower "
                        + "than it was before the chunk unloaded");
        helper.succeed();
    }
}
