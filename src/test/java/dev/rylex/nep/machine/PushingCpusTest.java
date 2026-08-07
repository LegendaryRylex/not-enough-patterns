package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class PushingCpusTest {

    private static final String POSITIONS_KEY = "PushCpus";

    @Test
    void aPushWithNoCpuBehindItIsRememberedAsPartial() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();

        CompoundTag tag = new CompoundTag();
        cpus.save(tag);

        assertTrue(tag.getBoolean("PushCpusPartial"), "a hand pushed pattern was not recorded as an unattributed push");
    }

    @Test
    void anUntouchedRecordWritesNothingIntoTheSave() {
        CompoundTag tag = new CompoundTag();
        new PushingCpus().save(tag);

        assertTrue(tag.isEmpty(), "an idle machine wrote pushing-cpu bookkeeping into its block entity tag");
    }

    @Test
    void aRecordedPushSurvivesASaveAndLoad() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();
        CompoundTag tag = new CompoundTag();
        cpus.save(tag);

        PushingCpus loaded = new PushingCpus();
        loaded.load(tag);

        CompoundTag again = new CompoundTag();
        loaded.save(again);
        assertEquals(tag.getBoolean("PushCpusPartial"), again.getBoolean("PushCpusPartial"));
        assertEquals(tag.getLongArray(POSITIONS_KEY).length, again.getLongArray(POSITIONS_KEY).length);
    }

    @Test
    void aWorldSavedBeforeCpusWereRecordedLoadsClean() {
        PushingCpus cpus = new PushingCpus();
        cpus.load(new CompoundTag());

        CompoundTag tag = new CompoundTag();
        cpus.save(tag);
        assertTrue(tag.isEmpty(), "loading a tag with no pushing-cpu data invented some");
    }

    @Test
    void clearingForgetsEveryRecordedPush() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();
        cpus.clear();

        CompoundTag tag = new CompoundTag();
        cpus.save(tag);
        assertTrue(tag.isEmpty(), "clearing left pushing-cpu bookkeeping behind");
    }

    @Test
    void positionsRoundTripThroughTheSaveTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLongArray(POSITIONS_KEY, new long[] {new BlockPos(3, 4, 5).asLong()});
        tag.putBoolean("PushCpusPartial", false);

        PushingCpus cpus = new PushingCpus();
        cpus.load(tag);

        CompoundTag again = new CompoundTag();
        cpus.save(again);
        assertEquals(new BlockPos(3, 4, 5).asLong(), again.getLongArray(POSITIONS_KEY)[0]);
        assertFalse(again.getBoolean("PushCpusPartial"));
    }

    @Test
    void cancellingWithoutAGridOrOutputsDoesNothing() {
        PushingCpus cpus = new PushingCpus();
        cpus.record();

        assertEquals(0, cpus.cancelJobsFor(null, Set.of(Items.DIAMOND)));
        assertEquals(0, cpus.cancelJobsFor(null, Set.of()));
    }
}
