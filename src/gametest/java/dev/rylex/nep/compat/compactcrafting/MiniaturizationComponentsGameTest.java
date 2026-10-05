package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MiniaturizationComponentsGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_miniaturization_components";

    private static final ResourceLocation AIR_COMPONENT_RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "miniaturization_air_component");

    private MiniaturizationComponentsGameTest() {}

    private static RecipeHolder<MiniaturizationRecipe> airComponentRecipe(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder =
                MiniaturizationRecipeResolver.resolveById(helper.getLevel(), AIR_COMPONENT_RECIPE);
        helper.assertTrue(holder != null, AIR_COMPONENT_RECIPE + " did not load as a miniaturization recipe");
        return holder;
    }

    private static long amountOf(EncodedIngredients encoded, Item item) {
        for (List<GenericStack> slot : encoded.inputs()) {
            GenericStack option = slot.get(0);
            if (option.what() instanceof AEItemKey key && key.getItem() == item) {
                return option.amount();
            }
        }
        return 0;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRecipeSpellingItsEmptyPositionsAsAirStillEncodes(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = airComponentRecipe(helper);
        EncodedIngredients encoded = MiniaturizationRecipeIngredients.miniaturization(holder, helper.getLevel());
        helper.assertTrue(
                encoded != null,
                "a recipe writing its empty positions as a block component holding air produced no ingredient list,"
                        + " so it would offer no recipe transfer button");

        for (List<GenericStack> slot : encoded.inputs()) {
            helper.assertTrue(
                    !(slot.get(0).what() instanceof AEItemKey key) || key.getItem() != Items.AIR,
                    "air was charged as an ingredient");
        }
        helper.assertTrue(
                encoded.inputs().size() == 3,
                "expected the catalyst, glass and iron blocks as the only three inputs, got "
                        + encoded.inputs().size());
        helper.assertTrue(
                amountOf(encoded, Items.GLASS) == 12,
                "eight glass in the lower layer and four in the upper should cost 12, got "
                        + amountOf(encoded, Items.GLASS));
        helper.assertTrue(amountOf(encoded, Items.IRON_BLOCK) == 1, "the single iron block should cost 1");
        helper.assertTrue(amountOf(encoded, Items.REDSTONE) == 1, "the catalyst should cost 1");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theLayoutPlacesNothingForAnAirComponent(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = airComponentRecipe(helper);
        List<MiniaturizationLayout.Placement> layout = MiniaturizationLayout.plan(holder.value(), BlockPos.ZERO);
        helper.assertTrue(layout != null, "the recipe produced no layout");

        for (MiniaturizationLayout.Placement placement : layout) {
            helper.assertTrue(
                    !placement.state().isAir(),
                    "the layout queued an air placement at " + placement.pos()
                            + ", which costs a build tick and is counted as unrecovered when a build is abandoned");
        }
        helper.assertTrue(
                layout.size() == 13,
                "twelve glass and one iron block should be the whole layout, got " + layout.size());
        helper.succeed();
    }
}
