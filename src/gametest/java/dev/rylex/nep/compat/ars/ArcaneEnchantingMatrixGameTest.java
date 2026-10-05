package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEBlocks;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneEnchantingMatrixGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_ars_matrix";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 5);
    private static final BlockPos ENERGY = CONTROLLER.above();
    private static final BlockPos JAR = new BlockPos(7, 1, 4);

    private static final int JAR_SPARE = 2_000;

    private ArcaneEnchantingMatrixGameTest() {}

    private record Apparatus(
            RecipeHolder<EnchantingApparatusRecipe> holder, IPatternDetails details, ItemStack result) {}

    private record Imbuement(
            RecipeHolder<ImbuementRecipe> holder,
            IPatternDetails reagentOnly,
            IPatternDetails withPedestals,
            List<GenericStack> pedestals,
            ItemStack result) {

        Map<AEKey, Long> lent() {
            Map<AEKey, Long> lent = new HashMap<>();
            pedestals.forEach(stack -> lent.merge(stack.what(), stack.amount(), Long::sum));
            return lent;
        }
    }

    private static Apparatus apparatus(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<RecipeHolder<EnchantingApparatusRecipe>> candidates = ArsRecipeResolver.apparatusCandidates(level).stream()
                .filter(holder -> holder.id().getNamespace().equals("ars_nouveau")
                        && holder.value().getType() == ArsRecipeResolver.apparatusType()
                        && holder.value().sourceCost() > 0)
                .sorted(Comparator.comparingInt((RecipeHolder<EnchantingApparatusRecipe> holder) ->
                                holder.value().sourceCost())
                        .thenComparing(holder -> holder.id().toString()))
                .toList();
        for (RecipeHolder<EnchantingApparatusRecipe> holder : candidates) {
            EncodedIngredients expected = ArsRecipeIngredients.apparatus(holder.value(), level);
            if (expected == null) {
                continue;
            }
            List<GenericStack> inputs = new ArrayList<>();
            for (List<GenericStack> options : expected.inputs()) {
                inputs.add(options.get(0));
            }
            IPatternDetails details = PatternDetailsHelper.decodePattern(
                    ApparatusPattern.encode(
                            holder.id(), inputs, expected.outputs().get(0)),
                    level);
            if (details != null && ArsRecipeResolver.resolve(details, level) != null) {
                return new Apparatus(
                        holder,
                        details,
                        ArsRecipeResolver.toStack(expected.outputs().get(0)));
            }
        }
        helper.fail("no enchanting apparatus recipe with a source cost could be encoded and resolved");
        return null;
    }

    private static Imbuement imbuement(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<RecipeHolder<ImbuementRecipe>> candidates = ArsRecipeResolver.imbuementCandidates(level).stream()
                .filter(holder -> holder.value().getClass() == ImbuementRecipe.class
                        && holder.id().getNamespace().equals("ars_nouveau")
                        && !holder.value().getPedestalItems().isEmpty())
                .sorted(Comparator.comparingInt((RecipeHolder<ImbuementRecipe> holder) ->
                                holder.value().getSource())
                        .thenComparing(holder -> holder.id().toString()))
                .toList();
        for (RecipeHolder<ImbuementRecipe> holder : candidates) {
            EncodedIngredients expected = ArsRecipeIngredients.imbuement(holder.value(), level);
            if (expected == null || expected.inputs().size() < 2) {
                continue;
            }
            List<GenericStack> all = new ArrayList<>();
            for (List<GenericStack> options : expected.inputs()) {
                all.add(options.get(0));
            }
            GenericStack output = expected.outputs().get(0);
            IPatternDetails reagentOnly = PatternDetailsHelper.decodePattern(
                    ImbuementPattern.encode(holder.id(), List.of(all.get(0)), output), level);
            IPatternDetails withPedestals =
                    PatternDetailsHelper.decodePattern(ImbuementPattern.encode(holder.id(), all, output), level);
            if (reagentOnly != null && withPedestals != null) {
                return new Imbuement(
                        holder,
                        reagentOnly,
                        withPedestals,
                        List.copyOf(all.subList(1, all.size())),
                        ArsRecipeResolver.toStack(output));
            }
        }
        helper.fail("no imbuement recipe with pedestal items could be encoded");
        return null;
    }

    private static ArcaneEnchantingMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        return placeMatrixAt(helper, MATRIX);
    }

    private static ArcaneEnchantingMatrixBlockEntity placeMatrixAt(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, NepArsContent.MATRIX.get().defaultBlockState());
        ArcaneEnchantingMatrixBlockEntity matrix =
                helper.getBlockEntity(pos) instanceof ArcaneEnchantingMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Arcane Enchanting Matrix has no block entity");
        return matrix;
    }

    private static boolean outputHolds(ArcaneEnchantingMatrixBlockEntity matrix, ItemStack expected) {
        for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            if (ItemStack.isSameItemSameComponents(matrix.getOutputBuffer().getStackInSlot(slot), expected)) {
                return true;
            }
        }
        return false;
    }

    private static int shelved(ArcaneEnchantingMatrixBlockEntity matrix) {
        int count = 0;
        for (int slot = 0; slot < matrix.getCatalystShelf().getSlots(); slot++) {
            count += matrix.getCatalystShelf().getStackInSlot(slot).getCount();
        }
        return count;
    }

    private static void clearCaches() {
        ArsRecipeResolver.clearCache();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void anApparatusCraftDrawsExactlyItsCostFromANearbyJar(GameTestHelper helper) {
        clearCaches();
        Apparatus fixture = apparatus(helper);
        int cost = fixture.holder().value().sourceCost();
        ArcaneEnchantingMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(JAR, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        SourceJarTile jar = helper.getBlockEntity(JAR);
        jar.addSource(cost + JAR_SPARE);

        helper.assertTrue(
                matrix.pushMatrixPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the Matrix refused " + fixture.holder().id());
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputHolds(matrix, fixture.result()),
                    "the Matrix never finished " + fixture.holder().id());
            helper.assertTrue(
                    jar.getSource() == JAR_SPARE,
                    "the jar holds " + jar.getSource() + " rather than the " + JAR_SPARE + " the craft left it");
            helper.assertTrue(matrix.storedSource() == 0, "the Matrix drew more than its craft needed");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void dampenGlyphsCutTheSourceACraftSpends(GameTestHelper helper) {
        clearCaches();
        Apparatus fixture = apparatus(helper);
        int cost = fixture.holder().value().sourceCost();
        ArcaneEnchantingMatrixBlockEntity matrix = placeMatrix(helper);
        int glyphs = ArcaneEnchantingMatrixUpgrades.maxDampen();
        ItemStack dampen = ArcaneEnchantingMatrixUpgrades.dampenIcon().copyWithCount(glyphs);
        helper.assertTrue(
                matrix.getDampenSlot().insertItem(0, dampen, false).isEmpty(),
                "the dampen slot refused a full stack of Dampen glyphs");
        matrix.sourceStorage().add(cost);
        int spent = ArcaneEnchantingMatrixUpgrades.sourceCost(cost, glyphs);
        helper.assertTrue(spent < cost, "a full stack of Dampen glyphs took nothing off " + cost);

        helper.assertTrue(
                matrix.pushMatrixPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the Matrix refused " + fixture.holder().id());
        helper.succeedWhen(() -> {
            helper.assertTrue(outputHolds(matrix, fixture.result()), "the dampened craft never finished");
            helper.assertTrue(
                    matrix.storedSource() == cost - spent,
                    "the store holds " + matrix.storedSource() + " rather than " + (cost - spent));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void accelerateGlyphsFinishACraftLongBeforeItsBaseTime(GameTestHelper helper) {
        clearCaches();
        Apparatus fixture = apparatus(helper);
        ArcaneEnchantingMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack accelerate = ArcaneEnchantingMatrixUpgrades.accelerateIcon()
                .copyWithCount(ArcaneEnchantingMatrixUpgrades.maxAccelerate());
        helper.assertTrue(
                matrix.getAccelerateSlot().insertItem(0, accelerate, false).isEmpty(),
                "the accelerate slot refused a full stack of Accelerate glyphs");
        matrix.sourceStorage().add(fixture.holder().value().sourceCost());

        helper.assertTrue(
                matrix.pushMatrixPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the Matrix refused " + fixture.holder().id());
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, fixture.result()), "a fully accelerated craft did not finish in 100 ticks"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRelayCanFillTheStoreButNothingCanDrainIt(GameTestHelper helper) {
        placeMatrix(helper);
        ISourceCap cap =
                helper.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, helper.absolutePos(MATRIX), null);
        helper.assertTrue(cap != null, "the Matrix exposes no source capability");
        helper.assertTrue(cap.receiveSource(500, false) == 500, "the Matrix refused source pushed into it");
        helper.assertTrue(cap.extractSource(500, false) == 0, "source could be drawn back out of the Matrix");
        helper.assertTrue(cap.getSourceCapacity() == 100_000, "the store is not 100,000 source");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternCarryingThePedestalItemsIsRefused(GameTestHelper helper) {
        clearCaches();
        Imbuement fixture = imbuement(helper);
        ArcaneEnchantingMatrixBlockEntity matrix = placeMatrix(helper);

        helper.assertTrue(
                !matrix.pushMatrixPattern(
                        fixture.withPedestals(), ArsFixtures.inputsOf(fixture.withPedestals()), Direction.UP),
                "the Matrix accepted a pattern that carries the imbuement pedestal items");
        helper.assertTrue(
                matrix.refusal() == ArcaneEnchantingMatrixBlockEntity.Refusal.CATALYSTS_IN_PATTERN,
                "the refusal reads " + matrix.refusal());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void anImbuementBorrowsItsPedestalItemsAndHandsThemBack(GameTestHelper helper) {
        clearCaches();
        Imbuement fixture = imbuement(helper);
        ArcaneEnchantingMatrixBlockEntity[] matrix = new ArcaneEnchantingMatrixBlockEntity[1];
        ArsFixtures.poweredNetwork(
                        helper, () -> matrix[0] = placeMatrixAt(helper, ArsFixtures.CONSUMER), fixture.pedestals())
                .thenExecute(() -> {
                    matrix[0].sourceStorage().add(fixture.holder().value().getSource());
                    helper.assertTrue(
                            matrix[0].pushMatrixPattern(
                                    fixture.reagentOnly(), ArsFixtures.inputsOf(fixture.reagentOnly()), Direction.UP),
                            "the Matrix refused a reagent-only imbuement pattern for "
                                    + fixture.holder().id());
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            shelved(matrix[0]) == fixture.pedestals().size(),
                            "the shelf holds " + shelved(matrix[0]) + " of "
                                    + fixture.pedestals().size() + " pedestal items");
                    for (GenericStack pedestal : fixture.pedestals()) {
                        helper.assertTrue(
                                ArsFixtures.stored(helper, pedestal.what()) == 0,
                                pedestal.what() + " is still in the network while it is borrowed");
                    }
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            outputHolds(matrix[0], fixture.result()),
                            "the Matrix never finished " + fixture.holder().id());
                    helper.assertTrue(shelved(matrix[0]) == 0, "the pedestal items never left the shelf");
                    fixture.lent()
                            .forEach((key, amount) -> helper.assertTrue(
                                    ArsFixtures.stored(helper, key) == amount,
                                    key + " did not come back to the network"));
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void queuesAnApparatusCraftFromThePlayersInventory(GameTestHelper helper) {
        clearCaches();
        Apparatus fixture = apparatus(helper);
        ArcaneEnchantingMatrixBlockEntity matrix = placeMatrix(helper);
        List<ManualRequirement> requirements =
                ArsRecipeIngredients.apparatusRequirements(fixture.holder().value());
        helper.assertTrue(requirements != null, "the apparatus recipe gave no manual requirements");
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftFixtures.assertQueued(
                helper, matrix.startManualCraft(player, fixture.holder().id(), 1), "the Arcane Enchanting Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) == 0, "the Matrix left ingredients in the inventory");
        helper.assertTrue(matrix.pendingJobs() == 1, "the manual craft did not queue a job");
        helper.succeed();
    }
}
