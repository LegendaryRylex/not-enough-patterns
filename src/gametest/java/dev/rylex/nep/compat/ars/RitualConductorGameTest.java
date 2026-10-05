package dev.rylex.nep.compat.ars;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RitualConductorGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_ars_conductor";
    private static final String WEATHER_BATCH = "nep_ars_conductor_weather";
    private static final String AUGMENT_BATCH = "nep_ars_conductor_augments";

    private static final BlockPos CONDUCTOR = ArsFixtures.CONSUMER;
    private static final BlockPos BRAZIER = new BlockPos(4, 1, 7);
    private static final BlockPos REDSTONE = CONDUCTOR.west();
    private static final int SETTLE_TICKS = 30;
    private static final int WAITING_SIGNAL = 5;
    private static final int PAUSED_SIGNAL = 2;

    private RitualConductorGameTest() {}

    private static RitualConductorBlockEntity placeConductor(GameTestHelper helper) {
        helper.setBlock(BRAZIER, BlockRegistry.RITUAL_BLOCK.get().defaultBlockState());
        helper.setBlock(CONDUCTOR, NepArsContent.RITUAL_CONDUCTOR.get().defaultBlockState());
        RitualConductorBlockEntity conductor = helper.getBlockEntity(CONDUCTOR);
        conductor.setRitual(ArsFixtures.sanctuary());
        return conductor;
    }

    private static void assertComparator(GameTestHelper helper, int expected) {
        int actual =
                helper.getBlockState(CONDUCTOR).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(CONDUCTOR));
        helper.assertTrue(actual == expected, "the comparator read " + actual + " instead of " + expected);
    }

    private static void tickThroughInterval(ServerLevel level, RitualConductorBlockEntity conductor) {
        for (int tick = 0; tick <= NepConfig.arsRitualConductorInterval(); tick++) {
            conductor.serverTick(level);
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void aRedstoneSignalPausesTheConductorAndTheComparatorSaysSo(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> placeConductor(helper))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> assertComparator(helper, WAITING_SIGNAL))
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> assertComparator(helper, PAUSED_SIGNAL))
                .thenExecute(() -> helper.setBlock(REDSTONE, Blocks.AIR))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> assertComparator(helper, WAITING_SIGNAL))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = WEATHER_BATCH, timeoutTicks = 300)
    public static void theRainSettingDoesNotFireDuringAThunderstorm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArsFixtures.poweredNetwork(helper, () -> placeConductor(helper), List.of())
                .thenExecute(() -> {
                    RitualConductorBlockEntity conductor = helper.getBlockEntity(CONDUCTOR);
                    RitualBrazierTile brazier = helper.getBlockEntity(BRAZIER);
                    try {
                        level.setWeatherParameters(0, 6000, true, true);
                        conductor.cycleWeather();
                        conductor.cycleWeather();
                        tickThroughInterval(level, conductor);
                        helper.assertTrue(
                                conductor.state() == RitualConductorBlockEntity.ConductorState.HELD,
                                "the Rain setting was not held back by a thunderstorm, state " + conductor.state());
                        helper.assertTrue(brazier.ritual == null, "a ritual was armed during the thunderstorm");
                        helper.assertTrue(
                                !RitualWeather.RAIN.satisfiedBy(level), "Rain reported itself satisfied in a storm");
                        helper.assertTrue(
                                RitualWeather.STORM.satisfiedBy(level), "Storm did not report itself satisfied");

                        conductor.cycleWeather();
                        tickThroughInterval(level, conductor);
                        helper.assertTrue(
                                conductor.state() != RitualConductorBlockEntity.ConductorState.HELD,
                                "the Storm setting was still held during a thunderstorm");

                        level.setWeatherParameters(0, 6000, true, false);
                        helper.assertTrue(
                                RitualWeather.RAIN.satisfiedBy(level), "Rain was not satisfied by plain rain");
                        helper.assertTrue(!RitualWeather.STORM.satisfiedBy(level), "Storm was satisfied by plain rain");
                        helper.assertTrue(!RitualWeather.CLEAR.satisfiedBy(level), "Clear was satisfied while raining");
                    } finally {
                        level.setWeatherParameters(6000, 0, false, false);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = AUGMENT_BATCH, timeoutTicks = 400)
    public static void eachAugmentSlotIsFedExactlyOncePerRun(GameTestHelper helper) {
        AEItemKey tablet = ArsRituals.tabletKey(ArsFixtures.sanctuary());
        AEItemKey flesh = AEItemKey.of(Items.ROTTEN_FLESH);
        AEItemKey dirt = AEItemKey.of(Items.DIRT);
        int[] consumed = new int[1];
        ArsFixtures.poweredNetwork(
                        helper,
                        () -> {
                            RitualConductorBlockEntity conductor = placeConductor(helper);
                            conductor.setAugment(0, flesh.toStack());
                            conductor.setAugment(1, flesh.toStack());
                            conductor.setAugment(2, dirt.toStack());
                            conductor.setAugment(3, flesh.toStack());
                        },
                        List.of(new GenericStack(tablet, 1), new GenericStack(flesh, 10), new GenericStack(dirt, 10)))
                .thenWaitUntil(() -> {
                    RitualBrazierTile brazier = helper.getBlockEntity(BRAZIER);
                    helper.assertTrue(
                            brazier.ritual != null && brazier.ritual.isRunning(), "the ritual has not started");
                    consumed[0] = brazier.ritual.itemConsumedCount(stack -> stack.is(Items.ROTTEN_FLESH));
                })
                .thenExecute(() -> {
                    helper.assertTrue(consumed[0] == 3, "the ritual consumed " + consumed[0] + " rotten flesh");
                    helper.assertTrue(
                            ArsFixtures.stored(helper, flesh) == 7,
                            "the network holds " + ArsFixtures.stored(helper, flesh) + " rotten flesh, not 7");
                    helper.assertTrue(
                            ArsFixtures.stored(helper, dirt) == 10,
                            "an augment the ritual refuses was taken from the network");
                    helper.assertTrue(ArsFixtures.stored(helper, tablet) == 0, "the tablet was not taken");
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertTrue(
                            ArsFixtures.stored(helper, flesh) == 7,
                            "the augments were fed again, " + ArsFixtures.stored(helper, flesh) + " flesh left");
                    helper.assertTrue(ArsFixtures.stored(helper, dirt) == 10, "dirt was taken on a later cycle");
                })
                .thenSucceed();
    }
}
