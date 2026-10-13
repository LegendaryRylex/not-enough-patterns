package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechPatternEncoders {
    private OritechPatternEncoders() {}

    static void register() {
        PatternConverters.register(DecoderModule.ORITECH, OritechRecipe.class, OritechPatternEncoders::collision);
    }

    @Nullable
    private static ItemStack collision(IPatternDetails encoded, RecipeHolder<OritechRecipe> holder, Level level) {
        if (!NepConfig.oritechParticleCollision()
                || !OritechRecipeResolver.isCollision(holder.value())
                || OritechRecipeResolver.planCollision(holder, encoded) == null) {
            return null;
        }
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        return ParticleCollisionPattern.encode(holder.id(), inputs, result);
    }
}
