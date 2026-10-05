package dev.rylex.nep.compat.extendedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import java.util.Arrays;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtendedAePatternViewGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_extendedae";

    private ExtendedAePatternViewGameTest() {}

    private static ItemStack mechanicalPattern() {
        GenericStack plank = new GenericStack(AEItemKey.of(Items.OAK_PLANKS), 1);
        GenericStack stick = new GenericStack(AEItemKey.of(Items.STICK), 1);
        return MechanicalCraftingPattern.encode(
                3,
                2,
                Arrays.asList(plank, null, plank, null, stick, null),
                new GenericStack(AEItemKey.of(Items.OAK_DOOR), 1));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theViewOpensOnOurPatterns(GameTestHelper helper) {
        ItemStack pattern = mechanicalPattern();
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "the mechanical crafting pattern did not decode");
        helper.assertTrue(
                PatternGuiHandler.open(helper.makeMockPlayer(GameType.SURVIVAL), details, pattern),
                "ExtendedAE has no view handler for our patterns; it would tell the player to report a bug to it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theViewKeepsAMechanicalPatternOnItsGrid(GameTestHelper helper) {
        PatternViewMenu menu = new PatternViewMenu(PatternViewMenu.TYPE, 0, helper.getLevel(), mechanicalPattern());

        helper.assertTrue(
                menu.stillValid(helper.makeMockPlayer(GameType.SURVIVAL)),
                "the view refused a mechanical crafting pattern");
        int indent = (PatternViewMenu.COLUMNS - 3) / 2;
        helper.assertTrue(
                menu.getSlot(indent).getItem().is(Items.OAK_PLANKS),
                "the top left cell of the grid did not land in the first column of the pattern");
        helper.assertTrue(
                menu.getSlot(indent + 2).getItem().is(Items.OAK_PLANKS),
                "the top right cell of the grid did not land three columns along");
        helper.assertTrue(
                menu.getSlot(indent + 1).getItem().isEmpty(), "the empty cell between the two planks was filled in");
        helper.assertTrue(
                menu.getSlot(PatternViewMenu.COLUMNS + indent + 1).getItem().is(Items.STICK),
                "the second row of the grid did not start a full nine slots along");
        helper.assertTrue(
                menu.getSlot(PatternViewMenu.INPUT_SLOTS).getItem().is(Items.OAK_DOOR),
                "the result did not land in the first output slot");
        helper.succeed();
    }
}
