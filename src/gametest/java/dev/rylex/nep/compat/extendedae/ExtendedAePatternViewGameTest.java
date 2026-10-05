package dev.rylex.nep.compat.extendedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.InfusionPattern;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class ExtendedAePatternViewGameTest {

    private ExtendedAePatternViewGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add("the_view_opens_on_our_patterns", ExtendedAePatternViewGameTest::theViewOpensOnOurPatterns)
                .add(
                        "the_view_lays_out_inputs_and_result",
                        ExtendedAePatternViewGameTest::theViewLaysOutInputsAndResult);
    }

    private static ItemStack pattern() {
        return InfusionPattern.encode(
                Identifier.withDefaultNamespace("oak_door"),
                List.of(new GenericStack(AEItemKey.of(Items.OAK_PLANKS), 6)),
                new GenericStack(AEItemKey.of(Items.OAK_DOOR), 3));
    }

    private static void theViewOpensOnOurPatterns(GameTestHelper helper) {
        ItemStack pattern = pattern();
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "the infusion pattern did not decode");
        helper.assertTrue(
                PatternGuiHandler.open(helper.makeMockPlayer(GameType.SURVIVAL), details, pattern),
                "ExtendedAE has no view handler for our patterns; it would tell the player to report a bug to it");
        helper.succeed();
    }

    private static void theViewLaysOutInputsAndResult(GameTestHelper helper) {
        PatternViewMenu menu = new PatternViewMenu(PatternViewMenu.TYPE, 0, helper.getLevel(), pattern());

        helper.assertTrue(
                menu.stillValid(helper.makeMockPlayer(GameType.SURVIVAL)), "the view refused an infusion pattern");
        helper.assertTrue(
                menu.getSlot(0).getItem().is(Items.OAK_PLANKS), "the ingredient did not land in the first input slot");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "a second input slot was filled in");
        helper.assertTrue(
                menu.getSlot(PatternViewMenu.INPUT_SLOTS).getItem().is(Items.OAK_DOOR),
                "the result did not land in the first output slot");
        helper.succeed();
    }
}
