package dev.rylex.nep;

import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

public final class NepRecipesGameTest {

    private static final List<String> ALWAYS_LOADED = List.of("import_card", "matrix_circuitry");

    private static final List<String> WITH_MYSTICAL_AGRICULTURE = List.of("infused_awakening_matrix");

    private NepRecipesGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                "every_shipped_recipe_the_mod_set_allows_is_loaded",
                NepRecipesGameTest::everyShippedRecipeTheModSetAllowsIsLoaded);
    }

    public static void everyShippedRecipeTheModSetAllowsIsLoaded(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        List<String> expected = new ArrayList<>(ALWAYS_LOADED);
        expected.addAll(WITH_MYSTICAL_AGRICULTURE);

        for (String path : expected) {
            if (Recipes.byId(helper.getLevel(), Identifier.fromNamespaceAndPath(Nep.MOD_ID, path)) == null) {
                missing.add(path);
            }
        }

        helper.assertTrue(
                missing.isEmpty(),
                "a recipe nep ships did not load, so the items it makes cannot be crafted at all; a json the codecs"
                        + " reject is dropped with only a line in the log: " + missing);
        helper.succeed();
    }
}
