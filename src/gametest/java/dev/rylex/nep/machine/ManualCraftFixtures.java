package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class ManualCraftFixtures {
    private ManualCraftFixtures() {}

    public static Player playerWith(GameTestHelper helper, List<ManualRequirement> requirements) {
        return playerWith(helper, requirements, 1);
    }

    public static Player playerWith(GameTestHelper helper, List<ManualRequirement> requirements, int batches) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (ManualRequirement requirement : requirements) {
            List<AEItemKey> options = IngredientMatching.itemOptions(requirement.ingredient(), helper.getLevel());
            helper.assertFalse(options.isEmpty(), "a manual requirement matched no item at all");
            int copies = requirement.consume() ? batches : 1;
            for (int batch = 0; batch < copies; batch++) {
                player.getInventory().add(options.get(0).toStack(requirement.count()));
            }
        }
        return player;
    }

    public static int inventoryCount(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            total += stack.getCount();
        }
        return total;
    }

    public static int bufferedCount(MatrixBuffer buffer) {
        int total = 0;
        for (int slot = 0; slot < buffer.size(); slot++) {
            total += buffer.stackAt(slot).getCount();
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
