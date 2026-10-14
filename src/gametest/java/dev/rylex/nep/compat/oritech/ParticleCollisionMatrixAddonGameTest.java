package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.addons.HeartOfTheMachineAddonEntity;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.MachineAddonController;

public final class ParticleCollisionMatrixAddonGameTest {

    private static final String CRAFT_TICKS_FIELD = "ORITECH_MATRIX_CRAFT_TICKS";
    private static final int SLOW_CRAFT_TICKS = 200;
    private static final Identifier RECIPE = Identifier.fromNamespaceAndPath("test", "particle_matrix_triple");

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();
    private static final BlockPos WEST = MATRIX.west();
    private static final BlockPos EAST = MATRIX.east();
    private static final BlockPos ABOVE = MATRIX.above();
    private static final BlockPos BELOW = MATRIX.below();

    private ParticleCollisionMatrixAddonGameTest() {}

    public static void register(
            NepGameTests.Batch main,
            NepGameTests.Batch speed,
            NepGameTests.Batch efficiency,
            NepGameTests.Batch chambers) {
        main.add(
                "an_addon_placed_on_a_face_lights_up_and_sets_the_stats",
                ParticleCollisionMatrixAddonGameTest::anAddonPlacedOnAFaceLightsUpAndSetsTheStats);
        main.add("a_cable_face_stays_free", ParticleCollisionMatrixAddonGameTest::aCableFaceStaysFree);
        main.add(
                "addons_with_nothing_to_do_for_a_collision_are_ignored",
                ParticleCollisionMatrixAddonGameTest::addonsWithNothingToDoForACollisionAreIgnored);
        main.add(
                "removing_an_addon_restores_the_stats",
                100,
                ParticleCollisionMatrixAddonGameTest::removingAnAddonRestoresTheStats);
        main.add(
                "a_late_addon_is_picked_up_without_a_placement_event",
                100,
                ParticleCollisionMatrixAddonGameTest::aLateAddonIsPickedUpWithoutAPlacementEvent);
        main.add(
                "the_core_slot_only_takes_real_machine_cores",
                ParticleCollisionMatrixAddonGameTest::theCoreSlotOnlyTakesRealMachineCores);
        main.add(
                "a_better_core_links_more_extenders_and_removing_it_drops_them_again",
                100,
                ParticleCollisionMatrixAddonGameTest::aBetterCoreLinksMoreExtendersAndRemovingItDropsThemAgain);
        speed.add(
                "a_speed_addon_finishes_the_craft_early",
                400,
                ParticleCollisionMatrixAddonGameTest::aSpeedAddonFinishesTheCraftEarly);
        efficiency.add(
                "an_efficiency_addon_cuts_the_crafts_cost",
                260,
                ParticleCollisionMatrixAddonGameTest::anEfficiencyAddonCutsTheCraftsCost);
        chambers.add(
                "a_processing_addon_runs_collisions_in_parallel",
                360,
                ParticleCollisionMatrixAddonGameTest::aProcessingAddonRunsCollisionsInParallel);
        main.add(
                "a_capacitor_addon_grows_the_buffer",
                ParticleCollisionMatrixAddonGameTest::aCapacitorAddonGrowsTheBuffer);
        main.add(
                "the_extra_capacity_and_its_energy_survive_a_save_and_load",
                ParticleCollisionMatrixAddonGameTest::theExtraCapacityAndItsEnergySurviveASaveAndLoad);
        main.add(
                "an_acceptor_addon_feeds_the_buffer",
                ParticleCollisionMatrixAddonGameTest::anAcceptorAddonFeedsTheBuffer);
        main.add(
                "a_bare_matrix_face_still_takes_energy",
                ParticleCollisionMatrixAddonGameTest::aBareMatrixFaceStillTakesEnergy);
        main.add(
                "a_synergy_matrix_counts_for_speed_efficiency_and_chambers",
                ParticleCollisionMatrixAddonGameTest::aSynergyMatrixCountsForSpeedEfficiencyAndChambers);
        main.add(
                "a_heart_of_the_machine_carries_its_stored_stats",
                ParticleCollisionMatrixAddonGameTest::aHeartOfTheMachineCarriesItsStoredStats);
    }

    private static final List<BlockPos> EXTENDER_CHAIN = List.of(
            new BlockPos(2, 2, 2),
            new BlockPos(2, 3, 2),
            new BlockPos(2, 4, 2),
            new BlockPos(1, 4, 2),
            new BlockPos(0, 4, 2));

    private static void layExtenderChain(GameTestHelper helper) {
        for (BlockPos pos : EXTENDER_CHAIN) {
            attach(helper, pos, BlockContent.MACHINE_EXTENDER.get());
        }
    }

    private static ParticleCollisionMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepOritechContent.MATRIX.get().defaultBlockState());
        ParticleCollisionMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX, ParticleCollisionMatrixBlockEntity.class);
        helper.assertTrue(matrix != null, "the Particle Collision Matrix has no block entity");
        return matrix;
    }

    private static void attach(GameTestHelper helper, BlockPos pos, Block addon) {
        helper.setBlock(pos, addon.defaultBlockState());
        helper.assertTrue(addon instanceof MachineAddonBlock, "the test block is not an Oritech addon");
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof MachineAddonBlock && state.getValue(MachineAddonBlock.ADDON_USED);
    }

    private static void withSlowCrafts(
            GameTestHelper helper, int timeoutTicks, Consumer<ConfigOverrides.Restore> body) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CRAFT_TICKS_FIELD, SLOW_CRAFT_TICKS);
        helper.runAtTickTime(timeoutTicks - 5, restore::undo);
        try {
            body.accept(restore);
        } catch (RuntimeException e) {
            restore.undo();
            throw e;
        }
    }

    private static MachineAddonBlock.AddonSettings settings(Block addon) {
        return ((MachineAddonBlock) addon).getAddonSettings();
    }

    static float expectedSpeed(Block addon) {
        float speed = settings(addon).speedMultiplier();
        return OritechConfig.additiveAddons.get() ? 1.0F / (1.0F + (1.0F - speed)) : speed;
    }

    static float expectedEfficiency(Block addon) {
        float efficiency = settings(addon).efficiencyMultiplier();
        if (!OritechConfig.additiveAddons.get()) {
            return efficiency;
        }
        float summed = 1.0F + (1.0F - efficiency);
        return summed > 1.0F ? 1.0F / summed : 1.0F + Math.abs(summed - 1.0F);
    }

    private static int insert(EnergyHandler storage, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = storage.insert(amount, tx);
            tx.commit();
            return accepted;
        }
    }

    private static void keepCharged(GameTestHelper helper, ParticleCollisionMatrixBlockEntity matrix, int ticks) {
        for (int tick = 1; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage().receive(Long.MAX_VALUE, false));
        }
    }

    private static RecipeHolder<OritechRecipe> recipe(GameTestHelper helper) {
        OritechRecipeResolver.clearCache();
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(helper.getLevel(), RECIPE);
        helper.assertTrue(holder != null, "the test collision recipe was not loaded");
        return holder;
    }

    private static boolean push(GameTestHelper helper, ParticleCollisionMatrixBlockEntity matrix, long batch) {
        ItemStack encoded = ParticleCollisionPattern.encode(
                RECIPE,
                List.of(
                        new GenericStack(AEItemKey.of(Items.SUGAR), batch),
                        new GenericStack(AEItemKey.of(Items.BONE_MEAL), batch)),
                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), batch * 3));
        IPatternDetails pattern = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(pattern instanceof ParticleCollisionPattern, "the test pattern did not decode");
        KeyCounter inputs = new KeyCounter();
        inputs.add(AEItemKey.of(Items.SUGAR), batch);
        inputs.add(AEItemKey.of(Items.BONE_MEAL), batch);
        return matrix.pushMatrixPattern(pattern, new KeyCounter[] {inputs}, Direction.UP);
    }

    private static int lapisIn(ParticleCollisionMatrixBlockEntity matrix) {
        int found = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().size(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().stackAt(slot);
            if (stack.is(Items.LAPIS_LAZULI)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    public static void anAddonPlacedOnAFaceLightsUpAndSetsTheStats(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.MACHINE_SPEED_ADDON.get());
        helper.assertTrue(!lit(helper, WEST), "the addon was lit before the Matrix scanned for it");

        matrix.initAddons();

        helper.assertTrue(lit(helper, WEST), "the Matrix did not claim the speed addon on its west face");
        helper.assertTrue(matrix.addonCount() == 1, "the Matrix counts " + matrix.addonCount() + " addons, not 1");
        float expected = expectedSpeed(BlockContent.MACHINE_SPEED_ADDON.get());
        helper.assertTrue(
                matrix.speedMultiplier() == expected,
                "the speed multiplier is " + matrix.speedMultiplier() + ", not " + expected);
        helper.assertTrue(
                matrix.craftTicks() < matrix.baseCraftTicks(),
                "a speed addon did not shorten the craft: " + matrix.craftTicks() + " vs " + matrix.baseCraftTicks());
        helper.succeed();
    }

    public static void aCableFaceStaysFree(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, BELOW, BlockContent.MACHINE_SPEED_ADDON.get());

        matrix.initAddons();

        helper.assertTrue(!lit(helper, BELOW), "the bottom face took an addon, leaving no face for a cable");
        helper.assertTrue(matrix.addonCount() == 0, "an addon on the bottom face was counted");
        helper.succeed();
    }

    public static void addonsWithNothingToDoForACollisionAreIgnored(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.MACHINE_YIELD_ADDON.get());
        attach(helper, EAST, BlockContent.MACHINE_HUNTER_ADDON.get());
        attach(helper, ABOVE, BlockContent.MACHINE_BURST_ADDON.get());

        matrix.initAddons();

        helper.assertTrue(matrix.addonCount() == 0, "the Matrix counted " + matrix.addonCount() + " useless addons");
        helper.assertTrue(
                !lit(helper, WEST) && !lit(helper, EAST) && !lit(helper, ABOVE), "a useless addon was lit as used");
        helper.assertTrue(matrix.speedMultiplier() == 1.0F, "a useless addon changed the speed");
        helper.succeed();
    }

    public static void removingAnAddonRestoresTheStats(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.MACHINE_SPEED_ADDON.get());
        attach(helper, EAST, BlockContent.MACHINE_EFFICIENCY_ADDON.get());
        matrix.initAddons();
        helper.assertTrue(matrix.addonCount() == 2, "the Matrix did not claim both addons");

        helper.setBlock(WEST, Blocks.AIR);
        helper.setBlock(EAST, Blocks.AIR);

        helper.succeedWhen(() -> {
            helper.assertTrue(matrix.addonCount() == 0, "the Matrix still counts a removed addon");
            helper.assertTrue(matrix.speedMultiplier() == 1.0F, "the speed bonus outlived its addon");
            helper.assertTrue(matrix.efficiencyMultiplier() == 1.0F, "the efficiency bonus outlived its addon");
        });
    }

    public static void aLateAddonIsPickedUpWithoutAPlacementEvent(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.MACHINE_SPEED_ADDON.get());

        helper.succeedWhen(() -> {
            helper.assertTrue(lit(helper, WEST), "the Matrix never noticed the addon beside it");
            helper.assertTrue(matrix.speedMultiplier() < 1.0F, "the late addon did not speed the Matrix up");
        });
    }

    public static void aSpeedAddonFinishesTheCraftEarly(GameTestHelper helper) {
        withSlowCrafts(helper, 400, restore -> {
            ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
            attach(helper, WEST, BlockContent.MACHINE_SPEED_ADDON.get());
            matrix.initAddons();
            recipe(helper);
            keepCharged(helper, matrix, 380);
            int shortened = matrix.craftTicks();
            helper.assertTrue(
                    shortened < SLOW_CRAFT_TICKS - 20,
                    "the speed addon only shortened " + SLOW_CRAFT_TICKS + " ticks to " + shortened);
            helper.assertTrue(push(helper, matrix, 1), "the Matrix refused a particle collision pattern");

            helper.runAtTickTime(
                    shortened - 10,
                    () -> helper.assertTrue(lapisIn(matrix) == 0, "the craft finished before the shortened time"));
            helper.succeedWhen(() -> {
                helper.assertTrue(lapisIn(matrix) == 3, "the sped-up craft has not finished, lapis " + lapisIn(matrix));
                helper.assertTrue(
                        helper.getTick() < SLOW_CRAFT_TICKS - 10,
                        "the craft finished, but no sooner than an unmodified one would");
                restore.undo();
            });
        });
    }

    public static void anEfficiencyAddonCutsTheCraftsCost(GameTestHelper helper) {
        withSlowCrafts(helper, 260, restore -> {
            ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
            attach(helper, WEST, BlockContent.MACHINE_EFFICIENCY_ADDON.get());
            matrix.initAddons();
            RecipeHolder<OritechRecipe> holder = recipe(helper);
            keepCharged(helper, matrix, 250);
            float efficiency = expectedEfficiency(BlockContent.MACHINE_EFFICIENCY_ADDON.get());
            long base = ParticleCollisionMatrixBlockEntity.energyCost(holder.value());
            long expected = Math.round(base * (double) efficiency);
            helper.assertTrue(expected < base, "the efficiency addon does not lower the cost, " + efficiency);
            helper.assertTrue(push(helper, matrix, 1), "the Matrix refused a particle collision pattern");

            helper.succeedWhen(() -> {
                helper.assertTrue(matrix.craftCost() > 0, "no craft started");
                helper.assertTrue(
                        matrix.craftCost() == expected,
                        "the craft costs " + matrix.craftCost() + " FE, not the discounted " + expected + " (base "
                                + base + ")");
                restore.undo();
            });
        });
    }

    public static void aProcessingAddonRunsCollisionsInParallel(GameTestHelper helper) {
        withSlowCrafts(helper, 360, restore -> {
            ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
            attach(helper, WEST, BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get());
            matrix.initAddons();
            recipe(helper);
            keepCharged(helper, matrix, 340);
            helper.assertTrue(matrix.extraChambers() >= 1, "the processing addon added no chambers");
            helper.assertTrue(push(helper, matrix, 2), "the Matrix refused a batched particle collision pattern");

            helper.runAtTickTime(
                    40, () -> helper.assertTrue(matrix.activeBatch() == 2, "two collisions were not claimed together"));
            helper.succeedWhen(() -> {
                helper.assertTrue(
                        lapisIn(matrix) == 6, "two parallel collisions should make 6 lapis, " + lapisIn(matrix));
                helper.assertTrue(matrix.pendingJobs() == 0, "the parallel craft left jobs queued");
                helper.assertTrue(
                        helper.getTick() < SLOW_CRAFT_TICKS * 2 - 20,
                        "both collisions finished, but no sooner than one after the other");
                restore.undo();
            });
        });
    }

    public static void aCapacitorAddonGrowsTheBuffer(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        long base = matrix.energyCapacity();
        attach(helper, WEST, BlockContent.MACHINE_CAPACITOR_ADDON.get());

        matrix.initAddons();

        long bonus = settings(BlockContent.MACHINE_CAPACITOR_ADDON.get()).addedCapacity();
        helper.assertTrue(bonus > 0, "the capacitor addon adds no capacity");
        helper.assertTrue(
                matrix.energyCapacity() == base + bonus,
                "the buffer holds " + matrix.energyCapacity() + " FE, not " + (base + bonus));
        helper.succeed();
    }

    public static void theExtraCapacityAndItsEnergySurviveASaveAndLoad(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.MACHINE_CAPACITOR_ADDON.get());
        matrix.initAddons();
        for (int i = 0; i < 400 && matrix.storedEnergy() <= NepConfig.oritechMatrixCapacity(); i++) {
            matrix.energyStorage().receive(Long.MAX_VALUE, false);
        }
        long beyondBase = matrix.storedEnergy();
        helper.assertTrue(
                beyondBase > NepConfig.oritechMatrixCapacity(), "the buffer did not take energy past its base size");

        CompoundTag saved = matrix.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(
                helper.absolutePos(MATRIX),
                matrix.getBlockState(),
                saved,
                helper.getLevel().registryAccess());

        helper.assertTrue(loaded instanceof ParticleCollisionMatrixBlockEntity, "the Matrix did not load back");
        ParticleCollisionMatrixBlockEntity reloaded = (ParticleCollisionMatrixBlockEntity) loaded;
        helper.assertTrue(
                reloaded.storedEnergy() == beyondBase,
                "a reload clamped the stored energy to " + reloaded.storedEnergy());
        helper.assertTrue(reloaded.energyCapacity() == matrix.energyCapacity(), "a reload lost the extra capacity");
        helper.succeed();
    }

    public static void anAcceptorAddonFeedsTheBuffer(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, EAST, BlockContent.MACHINE_ACCEPTOR_ADDON.get());
        matrix.initAddons();
        helper.assertTrue(lit(helper, EAST), "the acceptor addon was not claimed");

        EnergyHandler storage =
                helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(EAST), null);
        helper.assertTrue(storage != null, "the acceptor addon exposes no energy storage");
        int accepted = insert(storage, 5_000);

        helper.assertTrue(accepted == 5_000, "the acceptor took " + accepted + " FE of 5000");
        helper.assertTrue(matrix.storedEnergy() == 5_000, "the Matrix buffer holds " + matrix.storedEnergy() + " FE");
        helper.succeed();
    }

    public static void aBareMatrixFaceStillTakesEnergy(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, EAST, BlockContent.MACHINE_SPEED_ADDON.get());
        matrix.initAddons();

        EnergyHandler storage =
                helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(MATRIX), Direction.WEST);
        helper.assertTrue(storage != null, "a face without an addon lost its energy storage");
        helper.assertTrue(insert(storage, 1_000) == 1_000, "a bare face refused energy");
        helper.succeed();
    }

    public static void aSynergyMatrixCountsForSpeedEfficiencyAndChambers(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.SYNERGY_MATRIX_ADDON.get());

        matrix.initAddons();

        MachineAddonBlock.AddonSettings ultimate = settings(BlockContent.SYNERGY_MATRIX_ADDON.get());
        helper.assertTrue(lit(helper, WEST), "the synergy addon was not claimed");
        helper.assertTrue(
                matrix.speedMultiplier() == expectedSpeed(BlockContent.SYNERGY_MATRIX_ADDON.get()),
                "the synergy speed is not applied");
        helper.assertTrue(
                matrix.efficiencyMultiplier() == expectedEfficiency(BlockContent.SYNERGY_MATRIX_ADDON.get()),
                "the synergy efficiency is not applied");
        helper.assertTrue(matrix.extraChambers() == ultimate.chamberCount(), "the synergy chambers are not applied");
        helper.succeed();
    }

    public static void aHeartOfTheMachineCarriesItsStoredStats(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        attach(helper, WEST, BlockContent.HEART_OF_THE_MACHINE_ADDON.get());
        helper.assertTrue(
                helper.getBlockEntity(WEST, HeartOfTheMachineAddonEntity.class) != null,
                "the Heart has no block entity");
        helper.getBlockEntity(WEST, HeartOfTheMachineAddonEntity.class).storedData =
                new AddonSplicerBlockEntity.ShrunkAddonData(
                        new MachineAddonController.BaseAddonData(0.5F, 0.8F, 1_000_000L, 1_000L, 1, 0),
                        false,
                        0,
                        0,
                        0,
                        false,
                        false);

        matrix.initAddons();

        helper.assertTrue(lit(helper, WEST), "the combined addon was not claimed");
        helper.assertTrue(matrix.addonCount() == 1, "the combined addon was not counted");
        helper.assertTrue(matrix.speedMultiplier() == 0.5F, "the stored speed is not applied");
        helper.assertTrue(matrix.efficiencyMultiplier() == 0.8F, "the stored efficiency is not applied");
        helper.assertTrue(matrix.extraChambers() == 1, "the stored chamber is not applied");
        helper.assertTrue(matrix.addonCapacityBonus() == 1_000_000L, "the stored capacity is not applied");
        helper.succeed();
    }

    public static void theCoreSlotOnlyTakesRealMachineCores(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(
                matrix.coreSlot()
                        .isValid(
                                0,
                                ItemResource.of(
                                        BlockContent.MACHINE_CORE_4.get().asItem())),
                "a machine core is refused");
        helper.assertTrue(!matrix.coreSlot().isValid(0, ItemResource.of(Items.STONE)), "stone is accepted as a core");
        helper.assertTrue(
                matrix.coreSlot()
                                .getCapacityAsLong(
                                        0,
                                        ItemResource.of(BlockContent.MACHINE_CORE_4
                                                .get()
                                                .asItem()))
                        == 1,
                "the core slot takes more than one");
        helper.succeed();
    }

    public static void aBetterCoreLinksMoreExtendersAndRemovingItDropsThemAgain(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        layExtenderChain(helper);
        matrix.initAddons();
        int bare = matrix.addonCount();
        helper.assertTrue(bare >= 1 && bare < EXTENDER_CHAIN.size(), "the bare Matrix linked " + bare + " extenders");

        matrix.coreSlot().put(new ItemStack(BlockContent.MACHINE_CORE_7.get()));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(
                    matrix.addonCount() > bare,
                    "the core did not link more extenders: " + matrix.addonCount() + " against " + bare);
            helper.assertTrue(
                    matrix.addonEntries().size() == matrix.addonCount(), "the synced addon list is out of step");
            helper.assertTrue(
                    matrix.extenderLimit() > 1, "the extender limit is " + matrix.extenderLimit() + " with a core");

            matrix.coreSlot().put(ItemStack.EMPTY);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(
                        matrix.addonCount() == bare,
                        "removing the core left " + matrix.addonCount() + " extenders linked, not " + bare);
                helper.assertTrue(
                        !lit(helper, EXTENDER_CHAIN.get(EXTENDER_CHAIN.size() - 1)),
                        "the far extender stayed lit without the core");
                helper.succeed();
            });
        });
    }
}
