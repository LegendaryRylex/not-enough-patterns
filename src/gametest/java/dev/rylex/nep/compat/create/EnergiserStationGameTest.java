package dev.rylex.nep.compat.create;

import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.antarcticgardens.cna.CNABlocks;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergiserStationGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_stations";
    private static final BlockPos STATION = new BlockPos(2, 1, 2);

    private EnergiserStationGameTest() {}

    private static StationKind place(GameTestHelper helper, BlockState state) {
        helper.setBlock(STATION, state);
        return Stations.detect(helper.getLevel().getBlockState(helper.absolutePos(STATION)));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyEnergiserTierIsAnEnergizerStation(GameTestHelper helper) {
        StationKind basic = place(helper, CNABlocks.BASIC_ENERGISER.getDefaultState());
        helper.assertTrue(basic == StationKind.ENERGIZER, "a Basic Energiser was not detected as an Energizer station");

        StationKind advanced = place(helper, CNABlocks.ADVANCED_ENERGISER.getDefaultState());
        helper.assertTrue(
                advanced == StationKind.ENERGIZER, "an Advanced Energiser was not detected as an Energizer station");

        StationKind reinforced = place(helper, CNABlocks.REINFORCED_ENERGISER.getDefaultState());
        helper.assertTrue(
                reinforced == StationKind.ENERGIZER, "a Reinforced Energiser was not detected as an Energizer station");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anEnergiserWithoutAKineticSourceIsFlagged(GameTestHelper helper) {
        StationKind kind = place(helper, CNABlocks.BASIC_ENERGISER.getDefaultState());
        helper.assertTrue(
                Stations.isUnpowered(helper.getLevel(), helper.absolutePos(STATION), kind),
                "an Energiser with no kinetic source was reported powered");
        helper.succeed();
    }
}
