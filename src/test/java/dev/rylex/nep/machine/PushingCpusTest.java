package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import com.mojang.serialization.Codec;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class PushingCpusTest {

    private static final String POSITIONS_KEY = "PushCpus";
    private static final String PARTIAL_KEY = "PushCpusPartial";
    private static final Codec<List<BlockPos>> POSITIONS_CODEC = BlockPos.CODEC.listOf();

    private static TagValueOutput output() {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, MinecraftBootstrap.registries());
    }

    private static CompoundTag save(PushingCpus cpus) {
        TagValueOutput out = output();
        cpus.save(out);
        return out.buildResult();
    }

    private static PushingCpus load(CompoundTag tag) {
        PushingCpus cpus = new PushingCpus();
        cpus.load(TagValueInput.create(ProblemReporter.DISCARDING, MinecraftBootstrap.registries(), tag));
        return cpus;
    }

    @Test
    void aPushWithNoCpuBehindItIsRememberedAsPartial() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();

        assertTrue(
                save(cpus).getBooleanOr(PARTIAL_KEY, false),
                "a hand pushed pattern was not recorded as an unattributed push");
    }

    @Test
    void anUntouchedRecordWritesNothingIntoTheSave() {
        assertTrue(
                save(new PushingCpus()).isEmpty(),
                "an idle machine wrote pushing-cpu bookkeeping into its block entity tag");
    }

    @Test
    void aRecordedPushSurvivesASaveAndLoad() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();
        CompoundTag tag = save(cpus);

        assertEquals(tag, save(load(tag)));
    }

    @Test
    void aWorldSavedBeforeCpusWereRecordedLoadsClean() {
        assertTrue(save(load(new CompoundTag())).isEmpty(), "loading a tag with no pushing-cpu data invented some");
    }

    @Test
    void clearingForgetsEveryRecordedPush() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();
        cpus.clear();

        assertTrue(save(cpus).isEmpty(), "clearing left pushing-cpu bookkeeping behind");
    }

    @Test
    void positionsRoundTripThroughTheSaveTag() {
        BlockPos pos = new BlockPos(3, 4, 5);
        TagValueOutput seed = output();
        seed.store(POSITIONS_KEY, POSITIONS_CODEC, List.of(pos));
        seed.putBoolean(PARTIAL_KEY, false);

        CompoundTag written = save(load(seed.buildResult()));

        assertEquals(
                List.of(pos),
                TagValueInput.create(ProblemReporter.DISCARDING, MinecraftBootstrap.registries(), written)
                        .read(POSITIONS_KEY, POSITIONS_CODEC)
                        .orElseThrow());
        assertFalse(written.getBooleanOr(PARTIAL_KEY, true));
    }

    @Test
    void cancellingWithoutAGridOrOutputsDoesNothing() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();

        assertEquals(0, cpus.cancelJobsFor(null, Set.of(AEItemKey.of(Items.DIAMOND))));
        assertEquals(0, cpus.cancelJobsFor(null, Set.of()));
    }
}
