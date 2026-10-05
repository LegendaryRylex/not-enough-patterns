package dev.rylex.nep.machine;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface ManualCraftHost {

    ManualCraftOutcome startManualCraft(Player player, Identifier recipe, int batches);
}
