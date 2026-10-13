package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public final class ParticleCollisionPatternGameTest {

    private ParticleCollisionPatternGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "every_collision_recipe_encodes_as_a_single_result_pattern",
                        ParticleCollisionPatternGameTest::everyCollisionRecipeEncodesAsASingleResultPattern)
                .add(
                        "other_oritech_recipes_are_not_collisions",
                        ParticleCollisionPatternGameTest::otherOritechRecipesAreNotCollisions);
    }

    private static ItemStack encodeFirstOptions(RecipeHolder<OritechRecipe> holder, EncodedIngredients ingredients) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : ingredients.inputs()) {
            inputs.add(options.get(0));
        }
        return ParticleCollisionPattern.encode(
                Recipes.idOf(holder), inputs, ingredients.outputs().get(0));
    }

    public static void everyCollisionRecipeEncodesAsASingleResultPattern(GameTestHelper helper) {
        OritechRecipeResolver.clearCache();
        List<RecipeHolder<OritechRecipe>> recipes = OritechRecipeResolver.collisionCandidates(helper.getLevel());
        helper.assertTrue(!recipes.isEmpty(), "Oritech registered no particle collision recipes");

        for (RecipeHolder<OritechRecipe> holder : recipes) {
            EncodedIngredients ingredients = OritechRecipeIngredients.collision(holder.value(), helper.getLevel());
            helper.assertTrue(ingredients != null, "collision recipe " + holder.id() + " has no encodable ingredients");
            helper.assertTrue(
                    ingredients.inputs().size() == OritechRecipeIngredients.PARTICLES,
                    "collision recipe " + holder.id() + " does not take exactly two particles");

            IPatternDetails details =
                    PatternDetailsHelper.decodePattern(encodeFirstOptions(holder, ingredients), helper.getLevel());
            helper.assertTrue(
                    details instanceof ParticleCollisionPattern,
                    "collision recipe " + holder.id() + " did not decode as a Particle Collision Pattern");
            helper.assertTrue(
                    details.getOutputs().size() == 1
                            && details.getOutputs().get(0).amount()
                                    == OritechRecipeIngredients.fullResult(holder.value())
                                            .getCount(),
                    "collision recipe " + holder.id() + " does not promise exactly its full result");
            helper.assertTrue(
                    OritechRecipeResolver.planCollision(holder, details, helper.getLevel()) != null,
                    "collision recipe " + holder.id() + " does not plan from its own pattern");
            helper.assertTrue(
                    OritechRecipeResolver.collisionById(helper.getLevel(), Recipes.idOf(holder)) != null,
                    "collision recipe " + holder.id() + " is not found again by its id");
        }
        helper.succeed();
    }

    public static void otherOritechRecipesAreNotCollisions(GameTestHelper helper) {
        List<RecipeHolder<OritechRecipe>> pulverizer = Recipes.allOf(helper.getLevel(), RecipeContent.PULVERIZER.get());
        helper.assertTrue(!pulverizer.isEmpty(), "Oritech registered no pulverizer recipes");

        RecipeHolder<OritechRecipe> holder = pulverizer.get(0);
        helper.assertFalse(
                OritechRecipeResolver.isCollision(holder.value()), "a pulverizer recipe counted as a collision");
        helper.assertTrue(
                OritechRecipeResolver.collisionById(helper.getLevel(), Recipes.idOf(holder)) == null,
                "a pulverizer recipe resolved as a collision by its id");
        helper.succeed();
    }
}
