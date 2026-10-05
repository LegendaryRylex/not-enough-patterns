package dev.rylex.nep.decoder;

import appeng.api.networking.IGridNode;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PatternDecoderGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_pattern_decoder";
    private static final String REQUIRE = "REQUIRE_DECODER";

    private static final BlockPos DECODER = new BlockPos(2, 1, 2);

    private static ConfigOverrides.Restore required = () -> {};

    private PatternDecoderGameTest() {}

    @BeforeBatch(batch = BATCH)
    public static void requireTheDecoder(ServerLevel level) {
        required = ConfigOverrides.override(REQUIRE, true);
    }

    @AfterBatch(batch = BATCH)
    public static void stopRequiringTheDecoder(ServerLevel level) {
        required.undo();
    }

    private static PatternDecoderBlockEntity decoder(GameTestHelper helper, boolean powered) {
        helper.setBlock(DECODER, DecoderContent.PATTERN_DECODER.get());
        helper.setBlock(
                DECODER.above(),
                powered ? AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (PatternDecoderBlockEntity) helper.getBlockEntity(DECODER);
    }

    private static ItemStack module(DecoderModule module) {
        return new ItemStack(BuiltInRegistries.ITEM.get(Nep.id(module.itemPath())));
    }

    private static IGridNode node(PatternDecoderBlockEntity decoder) {
        return decoder.gridNodeHost().getGridNode(Direction.NORTH);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aPoweredDecoderUnlocksOnlyTheModulesItCarries(GameTestHelper helper) {
        PatternDecoderBlockEntity decoder = decoder(helper, true);
        decoder.moduleSlots().setStackInSlot(DecoderModule.CREATE.ordinal(), module(DecoderModule.CREATE));

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    int unlocked = PatternDecoding.unlocked(node(decoder));
                    helper.assertTrue(
                            decoder.status() == DecoderStatus.ONLINE,
                            "the decoder never came online on the energy cell's network: " + decoder.status());
                    helper.assertTrue(
                            PatternDecoding.allows(unlocked, DecoderModule.CREATE),
                            "a decoder carrying the Create module did not unlock Create patterns");
                    helper.assertFalse(
                            PatternDecoding.allows(unlocked, DecoderModule.DRACONIC_EVOLUTION),
                            "a decoder without the Draconic Evolution module unlocked its patterns anyway");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anUnpoweredDecoderUnlocksNothing(GameTestHelper helper) {
        PatternDecoderBlockEntity decoder = decoder(helper, false);
        decoder.moduleSlots().setStackInSlot(DecoderModule.CREATE.ordinal(), module(DecoderModule.CREATE));

        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    int unlocked = PatternDecoding.unlocked(node(decoder));
                    helper.assertFalse(
                            PatternDecoding.allows(unlocked, DecoderModule.CREATE),
                            "a decoder with no power still unlocked the module it carries");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void takingTheModuleOutLocksItsPatternsAgain(GameTestHelper helper) {
        PatternDecoderBlockEntity decoder = decoder(helper, true);
        decoder.moduleSlots().setStackInSlot(DecoderModule.MALUM.ordinal(), module(DecoderModule.MALUM));

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> decoder.moduleSlots().setStackInSlot(DecoderModule.MALUM.ordinal(), ItemStack.EMPTY))
                .thenExecute(() -> {
                    int unlocked = PatternDecoding.unlocked(node(decoder));
                    helper.assertFalse(
                            PatternDecoding.allows(unlocked, DecoderModule.MALUM),
                            "the Malum module stayed unlocked after it was taken out of the decoder");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_pattern_decoder_off")
    public static void withTheSettingOffEveryModuleIsUnlockedWithoutADecoder(GameTestHelper helper) {
        int unlocked = PatternDecoding.unlocked(null);
        for (DecoderModule module : DecoderModule.values()) {
            helper.assertTrue(
                    PatternDecoding.allows(unlocked, module),
                    module.modId() + " patterns needed a decoder although requireDecoder is off");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aDecoderRefusesAnythingButAnEncodingModule(GameTestHelper helper) {
        PatternDecoderBlockEntity decoder = decoder(helper, false);
        int arsSlot = DecoderModule.ARS_NOUVEAU.ordinal();
        ItemStack refused = decoder.moduleSlots().insertItem(arsSlot, new ItemStack(Blocks.DIRT), false);
        ItemStack misplaced =
                decoder.moduleSlots().insertItem(DecoderModule.CREATE.ordinal(), module(DecoderModule.MALUM), false);
        ItemStack accepted = decoder.moduleSlots()
                .insertItem(arsSlot, module(DecoderModule.ARS_NOUVEAU).copyWithCount(3), false);
        helper.assertTrue(refused.getCount() == 1, "a decoder slot took a block of dirt");
        helper.assertTrue(misplaced.getCount() == 1, "the Create slot took the Malum module");
        helper.assertTrue(accepted.getCount() == 2, "a decoder slot took more than one module");
        helper.succeed();
    }
}
