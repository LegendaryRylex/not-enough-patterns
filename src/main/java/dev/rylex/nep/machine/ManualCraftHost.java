package dev.rylex.nep.machine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface ManualCraftHost {

    ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches);
}
