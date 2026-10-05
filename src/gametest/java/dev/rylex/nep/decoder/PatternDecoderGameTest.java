package dev.rylex.nep.decoder;

import appeng.api.networking.IGridNode;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class PatternDecoderGameTest {

    private static final String REQUIRE = "REQUIRE_DECODER";

    private static final BlockPos POWERED = new BlockPos(1, 1, 1);
    private static final BlockPos UNPOWERED = new BlockPos(3, 1, 3);

    private static final int NETWORK_TICKS = 200;

    private PatternDecoderGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "a_decoder_unlocks_only_what_it_carries_while_powered",
                        NETWORK_TICKS,
                        PatternDecoderGameTest::aDecoderUnlocksOnlyWhatItCarriesWhilePowered)
                .add(
                        "a_decoder_refuses_anything_but_an_encoding_module",
                        PatternDecoderGameTest::aDecoderRefusesAnythingButAnEncodingModule);
    }

    private static PatternDecoderBlockEntity decoder(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, DecoderContent.PATTERN_DECODER.get());
        PatternDecoderBlockEntity decoder = PatternDecoderBlockEntity.at(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(decoder != null, "the Pattern Decoder placed no block entity");
        return decoder;
    }

    private static ItemResource module(DecoderModule module) {
        return ItemResource.of(BuiltInRegistries.ITEM.getValue(Nep.id(module.itemPath())));
    }

    private static IGridNode node(PatternDecoderBlockEntity decoder) {
        return decoder.gridNodeHost().getGridNode(Direction.NORTH);
    }

    private static void aDecoderUnlocksOnlyWhatItCarriesWhilePowered(GameTestHelper helper) {
        int unrequired = PatternDecoding.unlocked(null);
        for (DecoderModule module : DecoderModule.values()) {
            helper.assertTrue(
                    PatternDecoding.allows(unrequired, module),
                    module.modId() + " patterns needed a decoder although requireDecoder is off");
        }

        ConfigOverrides.Restore restore = ConfigOverrides.override(REQUIRE, true);
        PatternDecoderBlockEntity powered = decoder(helper, POWERED);
        helper.setBlock(POWERED.above(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        powered.moduleSlots()
                .set(DecoderModule.MYSTICAL_AGRICULTURE.ordinal(), module(DecoderModule.MYSTICAL_AGRICULTURE), 1);
        PatternDecoderBlockEntity unpowered = decoder(helper, UNPOWERED);
        unpowered
                .moduleSlots()
                .set(DecoderModule.APOTHIC_ENCHANTING.ordinal(), module(DecoderModule.APOTHIC_ENCHANTING), 1);

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    int unlocked = PatternDecoding.unlocked(node(powered));
                    int stranded = PatternDecoding.unlocked(node(unpowered));
                    powered.moduleSlots().set(DecoderModule.MYSTICAL_AGRICULTURE.ordinal(), ItemResource.EMPTY, 0);
                    int emptied = PatternDecoding.unlocked(node(powered));
                    restore.undo();
                    helper.assertTrue(
                            powered.status() == DecoderStatus.ONLINE,
                            "the decoder never came online on the energy cell's network: " + powered.status());
                    helper.assertTrue(
                            PatternDecoding.allows(unlocked, DecoderModule.MYSTICAL_AGRICULTURE),
                            "a decoder carrying the Mystical Agriculture module did not unlock its patterns");
                    helper.assertFalse(
                            PatternDecoding.allows(unlocked, DecoderModule.APOTHIC_ENCHANTING),
                            "a decoder without the Apothic Enchanting module unlocked its patterns anyway");
                    helper.assertFalse(
                            PatternDecoding.allows(stranded, DecoderModule.APOTHIC_ENCHANTING),
                            "a decoder with no power still unlocked the module it carries");
                    helper.assertFalse(
                            PatternDecoding.allows(emptied, DecoderModule.MYSTICAL_AGRICULTURE),
                            "the Mystical Agriculture module stayed unlocked after it was taken out");
                })
                .thenSucceed();
    }

    private static void aDecoderRefusesAnythingButAnEncodingModule(GameTestHelper helper) {
        PatternDecoderBlockEntity decoder = decoder(helper, POWERED);
        int apothicSlot = DecoderModule.APOTHIC_ENCHANTING.ordinal();
        helper.assertFalse(
                decoder.moduleSlots().isValid(apothicSlot, ItemResource.of(Items.DIRT)),
                "a decoder slot took a block of dirt");
        ItemResource module = module(DecoderModule.APOTHIC_ENCHANTING);
        helper.assertTrue(
                decoder.moduleSlots().isValid(apothicSlot, module), "a decoder slot refused an Encoding Module");
        helper.assertFalse(
                decoder.moduleSlots().isValid(DecoderModule.MYSTICAL_AGRICULTURE.ordinal(), module),
                "the Mystical Agriculture slot took the Apothic Enchanting module");
        helper.assertTrue(
                decoder.moduleSlots().getCapacityAsLong(apothicSlot, module) == 1,
                "a decoder slot holds more than one module");
        helper.succeed();
    }
}
