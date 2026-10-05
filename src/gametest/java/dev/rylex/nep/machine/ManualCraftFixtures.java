package dev.rylex.nep.machine;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.items.IItemHandler;

public final class ManualCraftFixtures {
    private ManualCraftFixtures() {}

    public static Player playerWith(GameTestHelper helper, List<ManualRequirement> requirements) {
        return playerWith(helper, requirements, 1);
    }

    public static Player playerWith(GameTestHelper helper, List<ManualRequirement> requirements, int batches) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (ManualRequirement requirement : requirements) {
            ItemStack[] options = requirement.ingredient().getItems();
            helper.assertTrue(options.length > 0, "a manual requirement matched no item at all");
            int copies = requirement.consume() ? batches : 1;
            for (int batch = 0; batch < copies; batch++) {
                player.getInventory().add(options[0].copyWithCount(requirement.count()));
            }
        }
        return player;
    }

    public static int inventoryCount(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            total += stack.getCount();
        }
        return total;
    }

    public static int bufferedCount(IItemHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return total;
    }

    public static void assertQueued(GameTestHelper helper, ManualCraftOutcome outcome, String machine) {
        helper.assertTrue(
                outcome.status() == ManualCraftResult.STARTED,
                machine + " refused a hand-started craft it could pay for: " + outcome.status());
        helper.assertTrue(outcome.batches() == 1, machine + " queued " + outcome.batches() + " crafts instead of one");
    }
}
