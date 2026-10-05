package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * The sibling of sandpaper: an ordinary deploying recipe whose tool a Deployer damages instead of consuming, because
 * {@code keep_held_item} is absent and the held item is damageable.
 */
@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeployerToolFateGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_deployer_tool_fate";

    private static final ResourceLocation WORN_TOOL =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_deploying_worn_tool");
    private static final ResourceLocation KEPT_TOOL =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_deploying_kept_tool");

    private DeployerToolFateGameTest() {}

    private static RecipeHolder<DeployerApplicationRecipe> deploying(GameTestHelper helper, ResourceLocation id) {
        Level level = helper.getLevel();
        for (RecipeHolder<DeployerApplicationRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType())) {
            if (holder.id().equals(id)) {
                return holder;
            }
        }
        helper.fail("the test recipe " + id + " was not loaded");
        return null;
    }

    private static AEItemKey shovel(int damage) {
        ItemStack stack = new ItemStack(Items.WOODEN_SHOVEL);
        stack.setDamageValue(damage);
        return AEItemKey.of(stack);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aDamageableToolWithoutKeepHeldItemCountsAsWorn(GameTestHelper helper) {
        RecipeHolder<DeployerApplicationRecipe> worn = deploying(helper, WORN_TOOL);
        helper.assertTrue(
                DeployerToolFate.of(WORN_TOOL, worn.value(), helper.getLevel()) == DeployerToolFate.WORN,
                "a deploying recipe whose damageable tool Create only hurts by a point read as consumed, so the tool"
                        + " would be written off and the worn one left jamming the deployer");

        RecipeHolder<DeployerApplicationRecipe> kept = deploying(helper, KEPT_TOOL);
        helper.assertTrue(
                DeployerToolFate.of(KEPT_TOOL, kept.value(), helper.getLevel()) == DeployerToolFate.KEPT,
                "a keep_held_item recipe read as wearing its tool, which would hand the network back a damaged copy of"
                        + " a tool Create never touched");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theEncoderMarksTheWornToolSlot(GameTestHelper helper) {
        EncodedIngredients encoded =
                CreateRecipeIngredients.itemApplication(deploying(helper, WORN_TOOL), helper.getLevel());
        helper.assertTrue(encoded != null, "the worn tool recipe encoded no ingredients");
        helper.assertTrue(
                encoded.isWorn(1) && !encoded.isRetained(1),
                "the tool slot was not marked worn, so the pattern would ask for the tool back exactly as it left");

        EncodedIngredients keptEncoded =
                CreateRecipeIngredients.itemApplication(deploying(helper, KEPT_TOOL), helper.getLevel());
        helper.assertTrue(keptEncoded != null, "the kept tool recipe encoded no ingredients");
        helper.assertTrue(
                keptEncoded.isRetained(1) && !keptEncoded.isWorn(1),
                "a kept tool was marked worn, so its pattern would ask for a damaged copy back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aWornToolPatternAsksForTheToolBackDamaged(GameTestHelper helper) {
        IPatternDetails details = PatternDetailsHelper.decodePattern(
                AndesiteCraftingPattern.encode(
                        WORN_TOOL,
                        List.of(new GenericStack(AEItemKey.of(Items.OAK_PLANKS), 1)),
                        List.of(),
                        List.of(new GenericStack(shovel(0), 1)),
                        new GenericStack(AEItemKey.of(Items.STICK), 1)),
                helper.getLevel());
        helper.assertTrue(details != null, "the worn tool test pattern did not decode");

        IPatternDetails.IInput tool = details.getInputs()[details.getInputs().length - 1];
        helper.assertTrue(
                tool.isValid(shovel(20), helper.getLevel()),
                "a part-used shovel is not a valid input, so a tool the deployer already wore is dead stock");
        helper.assertTrue(
                shovel(21).equals(tool.getRemainingKey(shovel(20))),
                "the pattern does not ask for the shovel back one point worse");
        helper.succeed();
    }
}
