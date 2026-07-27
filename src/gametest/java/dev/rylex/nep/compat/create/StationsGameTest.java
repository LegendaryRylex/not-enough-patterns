package dev.rylex.nep.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationsGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_stations";
    private static final BlockPos STATION = new BlockPos(2, 1, 2);

    private StationsGameTest() {}

    private static StationKind place(GameTestHelper helper, BlockState state) {
        helper.setBlock(STATION, state);
        return Stations.detect(helper.getLevel().getBlockState(helper.absolutePos(STATION)));
    }

    private static boolean unpowered(GameTestHelper helper, StationKind kind) {
        return Stations.isUnpowered(helper.getLevel(), helper.absolutePos(STATION), kind);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aSpoutNeedsNoRotationalPower(GameTestHelper helper) {
        StationKind kind = place(helper, AllBlocks.SPOUT.getDefaultState());

        helper.assertTrue(kind == StationKind.SPOUT, "a placed Spout was not detected as a Spout station");
        helper.assertTrue(
                !unpowered(helper, kind),
                "a Spout was reported unpowered; it is not a kinetic block and runs on fluid alone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void kineticStationsWithoutASourceAreStillFlagged(GameTestHelper helper) {
        StationKind deployer = place(
                helper, AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.assertTrue(unpowered(helper, deployer), "a Deployer with no kinetic source was reported powered");

        StationKind press = place(helper, AllBlocks.MECHANICAL_PRESS.getDefaultState());
        helper.assertTrue(unpowered(helper, press), "a Mechanical Press with no kinetic source was reported powered");

        StationKind saw = place(helper, AllBlocks.MECHANICAL_SAW.getDefaultState());
        helper.assertTrue(unpowered(helper, saw), "a Mechanical Saw with no kinetic source was reported powered");
        helper.succeed();
    }
}
