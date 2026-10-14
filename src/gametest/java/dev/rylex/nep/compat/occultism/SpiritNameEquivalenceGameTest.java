package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import com.klikli_dev.occultism.registry.OccultismDataComponents;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.util.ItemNBTUtil;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.machine.IncidentalComponents;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SpiritNameEquivalenceGameTest {

    private SpiritNameEquivalenceGameTest() {}

    public static void register(NepGameTests.Batch batch0) {
        batch0.add(
                        "two_bound_books_differ_only_by_their_spirit_name",
                        SpiritNameEquivalenceGameTest::twoBoundBooksDifferOnlyByTheirSpiritName)
                .add(
                        "any_other_difference_still_separates_the_books",
                        SpiritNameEquivalenceGameTest::anyOtherDifferenceStillSeparatesTheBooks)
                .add(
                        "book_matches_the_waiting_key_with_a_different_name",
                        SpiritNameEquivalenceGameTest::bookMatchesTheWaitingKeyWithADifferentName);
    }

    private static AEItemKey freshBook() {
        ItemStack book = new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get());
        ItemNBTUtil.generateBoundSpiritName(book);
        return AEItemKey.of(book);
    }

    public static void twoBoundBooksDifferOnlyByTheirSpiritName(GameTestHelper helper) {
        AEItemKey first = freshBook();
        AEItemKey second = freshBook();
        helper.assertTrue(
                first.get(OccultismDataComponents.SPIRIT_NAME.get()) != null,
                "a bound book built on the server carried no spirit name");
        helper.assertFalse(first.equals(second), "two bound books drew the same random spirit name");
        helper.assertTrue(IncidentalComponents.carries(first), "the spirit name is not registered as incidental");
        helper.assertTrue(
                IncidentalComponents.equivalent(first, second),
                "two bound books of one kind were not treated as the same item");
        helper.succeed();
    }

    public static void anyOtherDifferenceStillSeparatesTheBooks(GameTestHelper helper) {
        ItemStack renamed = new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get());
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Ledger"));
        helper.assertFalse(
                IncidentalComponents.equivalent(freshBook(), AEItemKey.of(renamed)),
                "a renamed bound book was treated as an unnamed one");
        helper.assertFalse(
                IncidentalComponents.equivalent(freshBook(), AEItemKey.of(Items.BOOK)),
                "a bound book was treated as a plain book");
        helper.succeed();
    }

    public static void bookMatchesTheWaitingKeyWithADifferentName(GameTestHelper helper) {
        AEItemKey waiting = freshBook();
        AEItemKey delivered = freshBook();
        helper.assertTrue(
                waiting.equals(IncidentalComponents.match(List.of(AEItemKey.of(Items.BOOK), waiting), delivered)),
                "the crafting job could not find the book it was waiting for under another spirit name");
        helper.succeed();
    }
}
