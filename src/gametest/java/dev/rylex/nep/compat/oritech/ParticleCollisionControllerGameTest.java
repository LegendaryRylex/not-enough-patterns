package dev.rylex.nep.compat.oritech;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.lumengrid.oritechthings.api.MagneticFieldController;
import com.lumengrid.oritechthings.block.ModBlocks;
import com.lumengrid.oritechthings.entity.custom.AcceleratorMagneticFieldBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.init.BlockContent;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParticleCollisionControllerGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_oritech_collision_controller";

    private static final BlockPos WEST_RING = new BlockPos(2, 1, 2);
    private static final BlockPos MIDDLE_RING = new BlockPos(3, 1, 2);
    private static final BlockPos EAST_RING = new BlockPos(4, 1, 2);
    private static final BlockPos EAST_ACCELERATOR = EAST_RING.north();
    private static final BlockPos WEST_ACCELERATOR = WEST_RING.south();

    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 4);
    private static final BlockPos ME_CONTROLLER = CONTROLLER.south();
    private static final BlockPos ENERGY = ME_CONTROLLER.above();
    private static final BlockPos FIELD = new BlockPos(1, 1, 3);

    private static final ResourceLocation UNREACHABLE =
            ResourceLocation.fromNamespaceAndPath("test", "particle_controller_unreachable");
    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "particle_controller_mixed");

    private static final int BOOT_TICKS = 60;

    private ParticleCollisionControllerGameTest() {}

    private record Ring(
            ParticleCollisionControllerBlockEntity controller,
            AcceleratorControllerBlockEntity east,
            AcceleratorControllerBlockEntity west) {}

    private record BuiltRing(GameTestSequence sequence, AtomicReference<Ring> reference) {

        Ring get() {
            return reference.get();
        }
    }

    private static BuiltRing ring(GameTestHelper helper) {
        AtomicReference<Ring> built = new AtomicReference<>();
        return new BuiltRing(buildRing(helper, built), built);
    }

    private static GameTestSequence buildRing(GameTestHelper helper, AtomicReference<Ring> built) {
        return helper.startSequence()
                .thenExecute(() -> {
                    for (BlockPos ring : List.of(WEST_RING, MIDDLE_RING, EAST_RING)) {
                        helper.setBlock(
                                ring,
                                BlockContent.ACCELERATOR_RING
                                        .defaultBlockState()
                                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
                    }
                    helper.setBlock(
                            EAST_ACCELERATOR,
                            BlockContent.ACCELERATOR_CONTROLLER
                                    .defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
                    helper.setBlock(
                            WEST_ACCELERATOR,
                            BlockContent.ACCELERATOR_CONTROLLER
                                    .defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                    helper.setBlock(CONTROLLER, NepOritechContent.CONTROLLER.get());
                    helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
                    helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
                })
                .thenIdle(BOOT_TICKS)
                .thenExecute(() -> {
                    ParticleCollisionControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER);
                    helper.assertTrue(
                            controller.applyLinks(List.of(
                                    helper.absolutePos(EAST_ACCELERATOR), helper.absolutePos(WEST_ACCELERATOR))),
                            "the idle Controller refused its links");
                    built.set(new Ring(
                            controller,
                            helper.getBlockEntity(EAST_ACCELERATOR),
                            helper.getBlockEntity(WEST_ACCELERATOR)));
                });
    }

    private static boolean push(GameTestHelper helper, Item first, Item second, Item result) {
        return push(helper, RECIPE, first, second, result);
    }

    private static boolean push(GameTestHelper helper, ResourceLocation recipe, Item first, Item second, Item result) {
        ItemStack encoded = ParticleCollisionPattern.encode(
                recipe,
                List.of(new GenericStack(AEItemKey.of(first), 1), new GenericStack(AEItemKey.of(second), 1)),
                new GenericStack(AEItemKey.of(result), 1));
        IPatternDetails pattern = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(pattern instanceof ParticleCollisionPattern, "the test pattern did not decode");
        ICraftingMachine machine = helper.getLevel()
                .getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CONTROLLER), Direction.NORTH);
        helper.assertTrue(machine != null, "the Controller offers no crafting machine");
        KeyCounter inputs = new KeyCounter();
        inputs.add(AEItemKey.of(first), 1);
        inputs.add(AEItemKey.of(second), 1);
        return machine.pushPattern(pattern, new KeyCounter[] {inputs}, Direction.NORTH);
    }

    private static long held(ParticleCollisionControllerBlockEntity controller, Item item) {
        long total = 0;
        for (int slot = 0; slot < controller.getOutputBuffer().getSlots(); slot++) {
            ItemStack stack = controller.getOutputBuffer().getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void linkField(GameTestHelper helper, Ring ring) {
        helper.setBlock(FIELD, ModBlocks.ACCELERATOR_MAGNETIC_FIELD.get());
        AcceleratorMagneticFieldBlockEntity field = helper.getBlockEntity(FIELD);
        field.energyStorage.amount = field.energyStorage.capacity;
        ((MagneticFieldController) ring.west()).addMagneticField(helper.absolutePos(FIELD));
    }

    private static String describe(Ring ring) {
        ParticleCollisionControllerBlockEntity controller = ring.controller();
        return "phase " + controller.phase() + ", stall " + controller.stall() + ", refusal " + controller.refusal()
                + ", top speed " + controller.topSpeed() + ", east holds "
                + ring.east().inventory.getItem(AcceleratorProbe.INPUT_SLOT) + " / "
                + ring.east().inventory.getItem(AcceleratorProbe.OUTPUT_SLOT) + ", west holds "
                + ring.west().inventory.getItem(AcceleratorProbe.INPUT_SLOT) + " / "
                + ring.west().inventory.getItem(AcceleratorProbe.OUTPUT_SLOT);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anUnmeasuredRingRefusesEveryPattern(GameTestHelper helper) {
        BuiltRing built = ring(helper);
        built.sequence()
                .thenExecute(() -> {
                    Ring ring = built.get();
                    helper.assertValueEqual(ring.controller().topSpeed(), 0L, "top speed of a ring nothing has flown");
                    helper.assertFalse(
                            push(helper, Items.CLAY_BALL, Items.FLINT, Items.EMERALD),
                            "the Controller took a pattern before any particle was measured");
                    helper.assertValueEqual(
                            ring.controller().refusal(),
                            ParticleCollisionControllerBlockEntity.Refusal.TOO_FAST,
                            "refusal for an unmeasured ring");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aMeasuredRingRunsAStaggeredCollision(GameTestHelper helper) {
        BuiltRing built = ring(helper);
        built.sequence()
                .thenExecute(() -> built.get()
                        .east()
                        .inventory
                        .setItem(AcceleratorProbe.INPUT_SLOT, new ItemStack(Items.COBBLESTONE)))
                .thenWaitUntil(() -> helper.assertTrue(
                        built.get().controller().topSpeed() > 0,
                        "the Controller never measured the hand-fired particle (" + describe(built.get()) + ")"))
                .thenWaitUntil(() -> helper.assertTrue(
                        AcceleratorProbe.idle(built.get().east())
                                && AcceleratorProbe.idle(built.get().west()),
                        "the hand-fired particle never left the ring (" + describe(built.get()) + ")"))
                .thenExecute(() -> helper.assertTrue(
                        push(helper, Items.CLAY_BALL, Items.FLINT, Items.EMERALD),
                        "the measured Controller refused the pattern (" + describe(built.get()) + ")"))
                .thenWaitUntil(() -> helper.assertTrue(
                        built.get().controller().phase() != ParticleCollisionControllerBlockEntity.Phase.IDLE,
                        "the Controller never launched (" + describe(built.get()) + ")"))
                .thenExecute(() -> {
                    ParticleCollisionControllerBlockEntity controller =
                            built.get().controller();
                    helper.assertValueEqual(
                            controller.manualDemandFor(new ItemStack(Items.CLAY_BALL)),
                            0,
                            "fast particles still wanted while one is in flight");
                    helper.assertValueEqual(
                            controller.manualDemandFor(new ItemStack(Items.FLINT)),
                            0,
                            "slow particles still wanted while one is held");
                })
                .thenWaitUntil(() -> helper.assertValueEqual(
                        held(built.get().controller(), Items.EMERALD),
                        1L,
                        "collision results the Controller collected (" + describe(built.get()) + ")"))
                .thenExecute(() -> {
                    Ring ring = built.get();
                    helper.assertValueEqual(ring.controller().pendingJobs(), 0L, "collisions still owed");
                    helper.assertTrue(
                            AcceleratorProbe.idle(ring.east()) && AcceleratorProbe.idle(ring.west()),
                            "an Accelerator Controller still holds an item (" + describe(ring) + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aMagneticFieldLiftsTheSpeedLimitAndTakesTheFastParticle(GameTestHelper helper) {
        BuiltRing built = ring(helper);
        built.sequence()
                .thenExecute(() -> linkField(helper, built.get()))
                .thenIdle(2)
                .thenExecute(() -> {
                    Ring ring = built.get();
                    helper.assertTrue(
                            ring.controller().fieldHeld(),
                            "the Controller did not see the powered field (" + describe(ring) + ")");
                    helper.assertValueEqual(ring.controller().topSpeed(), 0L, "top speed of a ring nothing has flown");
                    helper.assertTrue(
                            push(helper, Items.CLAY_BALL, Items.FLINT, Items.EMERALD),
                            "the field-held Controller refused the pattern (" + describe(ring) + ")");
                })
                .thenIdle(1)
                .thenExecute(() -> helper.assertFalse(
                        built.get()
                                        .east()
                                        .inventory
                                        .getItem(AcceleratorProbe.INPUT_SLOT)
                                        .is(Items.CLAY_BALL)
                                || AcceleratorProbe.live(built.get().east()) != null,
                        "the fast particle launched from the accelerator without the field (" + describe(built.get())
                                + ")"))
                .thenWaitUntil(() -> helper.assertValueEqual(
                        held(built.get().controller(), Items.EMERALD),
                        1L,
                        "collision results the Controller collected (" + describe(built.get()) + ")"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void aParticleLostUnderTheFieldCapsItAndStopsTheRetries(GameTestHelper helper) {
        BuiltRing built = ring(helper);
        built.sequence()
                .thenExecute(() -> linkField(helper, built.get()))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        push(helper, UNREACHABLE, Items.CLAY_BALL, Items.GUNPOWDER, Items.DIAMOND),
                        "the field-held Controller refused the pattern (" + describe(built.get()) + ")"))
                .thenWaitUntil(() -> helper.assertValueEqual(
                        built.get().controller().speedLimit(),
                        2L,
                        "field cap after the lost particle (" + describe(built.get()) + ")"))
                .thenIdle(260)
                .thenExecute(() -> {
                    ParticleCollisionControllerBlockEntity controller =
                            built.get().controller();
                    helper.assertValueEqual(
                            controller.phase(),
                            ParticleCollisionControllerBlockEntity.Phase.IDLE,
                            "phase once the capped field refuses the recipe (" + describe(built.get()) + ")");
                    helper.assertValueEqual(
                            controller.stall(),
                            ParticleCollisionControllerBlockEntity.Stall.TOO_SLOW,
                            "stall once the capped field refuses the recipe (" + describe(built.get()) + ")");
                })
                .thenSucceed();
    }
}
