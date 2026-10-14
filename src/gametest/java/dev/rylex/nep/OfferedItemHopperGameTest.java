package dev.rylex.nep;

import dev.rylex.nep.util.OfferedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OfferedItemHopperGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final BlockPos OFFERED_HOPPER = new BlockPos(1, 1, 2);
    private static final BlockPos PLAIN_HOPPER = new BlockPos(3, 1, 2);

    private OfferedItemHopperGameTest() {}

    private static ItemEntity dropAbove(GameTestHelper helper, BlockPos hopper) {
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(hopper.above()));
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.RAW_IRON));
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    @GameTest(template = TEMPLATE, batch = "nep_offered_item_hopper", timeoutTicks = 100)
    public static void aHopperLeavesAnOfferedItemWhereItLies(GameTestHelper helper) {
        helper.setBlock(OFFERED_HOPPER, Blocks.HOPPER);
        helper.setBlock(PLAIN_HOPPER, Blocks.HOPPER);
        ItemEntity offered = dropAbove(helper, OFFERED_HOPPER);
        OfferedItems.mark(offered);
        ItemEntity plain = dropAbove(helper, PLAIN_HOPPER);

        helper.runAtTickTime(60, () -> {
            HopperBlockEntity offeredHopper = (HopperBlockEntity) helper.getBlockEntity(OFFERED_HOPPER);
            HopperBlockEntity plainHopper = (HopperBlockEntity) helper.getBlockEntity(PLAIN_HOPPER);
            helper.assertTrue(
                    plain.isRemoved() && !plainHopper.isEmpty(), "the control hopper never took an ordinary item");
            helper.assertTrue(offered.isAlive(), "the hopper swallowed an item set out for a spirit");
            helper.assertTrue(offeredHopper.isEmpty(), "the hopper holds the item set out for a spirit");
            helper.succeed();
        });
    }
}
