package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParticleCollisionPatternGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_oritech_particle_collision";

    private ParticleCollisionPatternGameTest() {}

    private static ItemStack encodeFirstOptions(RecipeHolder<OritechRecipe> holder, EncodedIngredients ingredients) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : ingredients.inputs()) {
            inputs.add(options.get(0));
        }
        return ParticleCollisionPattern.encode(
                holder.id(), inputs, ingredients.outputs().get(0));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyCollisionRecipeEncodesAsASingleResultPattern(GameTestHelper helper) {
        OritechRecipeResolver.clearCache();
        List<RecipeHolder<OritechRecipe>> recipes = OritechRecipeResolver.collisionCandidates(helper.getLevel());
        helper.assertTrue(!recipes.isEmpty(), "Oritech registered no particle collision recipes");

        for (RecipeHolder<OritechRecipe> holder : recipes) {
            EncodedIngredients ingredients = OritechRecipeIngredients.collision(holder.value());
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
                            && details.getOutputs().get(0).amount() == 1,
                    "collision recipe " + holder.id() + " promises more than one result");
            helper.assertTrue(
                    OritechRecipeResolver.planCollision(holder, details) != null,
                    "collision recipe " + holder.id() + " does not plan from its own pattern");
            helper.assertTrue(
                    OritechRecipeResolver.collisionById(helper.getLevel(), holder.id()) != null,
                    "collision recipe " + holder.id() + " is not found again by its id");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void otherOritechRecipesAreNotCollisions(GameTestHelper helper) {
        List<RecipeHolder<OritechRecipe>> pulverizer =
                helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeContent.PULVERIZER);
        helper.assertTrue(!pulverizer.isEmpty(), "Oritech registered no pulverizer recipes");

        RecipeHolder<OritechRecipe> holder = pulverizer.get(0);
        helper.assertFalse(
                OritechRecipeResolver.isCollision(holder.value()), "a pulverizer recipe counted as a collision");
        helper.assertTrue(
                OritechRecipeResolver.collisionById(helper.getLevel(), holder.id()) == null,
                "a pulverizer recipe resolved as a collision by its id");
        helper.succeed();
    }
}
