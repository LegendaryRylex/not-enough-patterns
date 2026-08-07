package dev.rylex.nep.compat.create.newage;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import java.util.List;
import net.minecraft.world.level.block.Block;
import org.antarcticgardens.cna.CNABlocks;
import org.antarcticgardens.cna.content.energising.EnergiserBlock;
import org.antarcticgardens.cna.content.energising.recipe.EnergisingRecipe;

public final class CreateNewAgeCompat {

    public static final String MOD_ID = "create_new_age";

    private CreateNewAgeCompat() {}

    public static List<Block> energiserBlocks() {
        return List.of(
                CNABlocks.BASIC_ENERGISER.get(),
                CNABlocks.ADVANCED_ENERGISER.get(),
                CNABlocks.REINFORCED_ENERGISER.get());
    }

    public static boolean isEnergiser(Block block) {
        return block instanceof EnergiserBlock;
    }

    public static long energyCost(SequencedAssemblyRecipe recipe) {
        long perLoop = 0;
        for (SequencedRecipe<?> step : recipe.getSequence()) {
            if (step.getAsAssemblyRecipe() instanceof EnergisingRecipe energising) {
                perLoop += energising.getEnergyNeeded();
            }
        }
        return perLoop * Math.max(1, recipe.getLoops());
    }
}
