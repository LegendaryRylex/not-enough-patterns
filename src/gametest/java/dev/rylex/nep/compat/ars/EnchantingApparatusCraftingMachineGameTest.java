package dev.rylex.nep.compat.ars;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.hollingsworth.arsnouveau.api.registry.PerkRegistry;
import com.hollingsworth.arsnouveau.common.armor.AnimatedMagicArmor;
import com.hollingsworth.arsnouveau.common.block.tile.ArcanePedestalTile;
import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ArmorUpgradeRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantmentRecipe;
import com.hollingsworth.arsnouveau.common.items.data.ArmorPerkHolder;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantingApparatusCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_ars_apparatus";

    private static final BlockPos CORE = new BlockPos(4, 1, 4);
    private static final BlockPos APPARATUS = CORE.above();
    private static final BlockPos JAR = new BlockPos(7, 1, 4);

    private static final List<BlockPos> PEDESTAL_SPOTS = List.of(
            new BlockPos(2, 1, 4),
            new BlockPos(6, 1, 4),
            new BlockPos(4, 1, 2),
            new BlockPos(4, 1, 6),
            new BlockPos(2, 1, 2),
            new BlockPos(6, 1, 6),
            new BlockPos(2, 1, 6),
            new BlockPos(6, 1, 2));

    private static final int MAX_JAR_SOURCE = 9_000;

    private EnchantingApparatusCraftingMachineGameTest() {}

    private record Fixture(
            RecipeHolder<EnchantingApparatusRecipe> holder, IPatternDetails details, EncodedIngredients expected) {}

    private static Fixture fixture(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<RecipeHolder<EnchantingApparatusRecipe>> candidates = ArsRecipeResolver.apparatusCandidates(level).stream()
                .filter(holder -> holder.id().getNamespace().equals("ars_nouveau")
                        && holder.value().pedestalItems().size() <= PEDESTAL_SPOTS.size())
                .sorted(Comparator.comparingInt((RecipeHolder<EnchantingApparatusRecipe> holder) ->
                                holder.value().sourceCost())
                        .thenComparingInt(
                                holder -> holder.value().pedestalItems().size())
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
            ItemStack encoded = ApparatusPattern.encode(
                    holder.id(), inputs, expected.outputs().get(0));
            IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
            if (details != null && ArsRecipeResolver.resolve(details, level) != null) {
                return new Fixture(holder, details, expected);
            }
        }
        helper.fail("no enchanting apparatus recipe could be encoded and resolved");
        return null;
    }

    private static Fixture openFixture(
            GameTestHelper helper,
            Predicate<EnchantingApparatusRecipe> wanted,
            Function<EnchantingApparatusRecipe, ItemStack> reagentFor) {
        Level level = helper.getLevel();
        List<RecipeHolder<EnchantingApparatusRecipe>> candidates = ArsRecipeResolver.apparatusCandidates(level).stream()
                .filter(holder -> ArsRecipeResolver.opensReagent(holder.value().getType())
                        && wanted.test(holder.value())
                        && holder.value().pedestalItems().size() <= PEDESTAL_SPOTS.size())
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        for (RecipeHolder<EnchantingApparatusRecipe> holder : candidates) {
            ItemStack reagent = reagentFor.apply(holder.value());
            EncodedIngredients expected =
                    reagent.isEmpty() ? null : ArsRecipeIngredients.openReagent(holder.value(), reagent, level);
            if (expected == null) {
                continue;
            }
            List<GenericStack> inputs =
                    expected.inputs().stream().map(options -> options.get(0)).toList();
            IPatternDetails details = PatternDetailsHelper.decodePattern(
                    ApparatusPattern.encode(
                            holder.id(), inputs, expected.outputs().get(0)),
                    level);
            if (details != null && ArsRecipeResolver.resolve(details, level) != null) {
                return new Fixture(holder, details, expected);
            }
        }
        helper.fail("no enchantment or armor upgrade recipe could be encoded and resolved");
        return null;
    }

    private static ItemStack lowerLevelBook(EnchantingApparatusRecipe recipe, Level level) {
        EnchantmentRecipe enchantment = (EnchantmentRecipe) recipe;
        return EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(enchantment.holderFor(level), enchantment.enchantLevel - 1));
    }

    private static ItemStack armorBelow(EnchantingApparatusRecipe recipe) {
        int tier = ((ArmorUpgradeRecipe) recipe).tier();
        for (Item item : PerkRegistry.getPerkProviderItems()) {
            if (item instanceof AnimatedMagicArmor armor && armor.getMinTier() < tier) {
                ItemStack stack = new ItemStack(item);
                stack.set(
                        DataComponentRegistry.ARMOR_PERKS.get(),
                        stack.getOrDefault(DataComponentRegistry.ARMOR_PERKS.get(), new ArmorPerkHolder())
                                .setTier(tier - 1));
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void enchantsInto(GameTestHelper helper, Fixture fixture, Item expected) {
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);
        helper.assertTrue(
                machine(helper, APPARATUS)
                        .pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "a built apparatus refused " + fixture.holder().id());
        helper.succeedWhen(() -> {
            assertProduces(helper, fixture, apparatus);
            helper.assertTrue(apparatus.getStack().is(expected), "the apparatus made " + apparatus.getStack());
        });
    }

    private static EnchantingApparatusTile placeApparatus(GameTestHelper helper, Fixture fixture) {
        helper.setBlock(APPARATUS, BlockRegistry.ENCHANTING_APP_BLOCK.get().defaultBlockState());
        int pedestals = fixture.holder().value().pedestalItems().size();
        for (int index = 0; index < pedestals; index++) {
            helper.setBlock(
                    PEDESTAL_SPOTS.get(index),
                    BlockRegistry.ARCANE_PEDESTAL.get().defaultBlockState());
        }
        helper.setBlock(JAR, BlockRegistry.SOURCE_JAR.get().defaultBlockState());
        SourceJarTile jar = helper.getBlockEntity(JAR);
        jar.addSource(Math.min(
                MAX_JAR_SOURCE, Math.max(1_000, fixture.holder().value().sourceCost() * 2)));
        return helper.getBlockEntity(APPARATUS);
    }

    private static void placeCore(GameTestHelper helper, Direction facing) {
        helper.setBlock(
                CORE,
                BlockRegistry.ARCANE_CORE_BLOCK
                        .get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing));
    }

    private static ICraftingMachine machine(GameTestHelper helper, BlockPos pos) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(pos), null);
        helper.assertTrue(machine != null, "nothing at " + pos + " exposed a crafting machine capability");
        return machine;
    }

    private static void assertNothingStranded(GameTestHelper helper, EnchantingApparatusTile apparatus) {
        helper.assertTrue(apparatus.getStack().isEmpty(), "a refused push left an item in the apparatus");
        helper.assertTrue(!apparatus.isCrafting, "a refused push started a craft");
        for (BlockPos spot : PEDESTAL_SPOTS) {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(spot)) instanceof ArcanePedestalTile pedestal) {
                helper.assertTrue(pedestal.getStack().isEmpty(), "a refused push left an item on a pedestal");
            }
        }
    }

    private static void assertProduces(GameTestHelper helper, Fixture fixture, EnchantingApparatusTile apparatus) {
        GenericStack result = fixture.expected().outputs().get(0);
        helper.assertTrue(!apparatus.isCrafting, "the apparatus is still crafting");
        helper.assertTrue(
                result.what().equals(AEItemKey.of(apparatus.getStack()))
                        && apparatus.getStack().getCount() == result.amount(),
                "the apparatus holds " + apparatus.getStack() + " rather than " + result);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void aBuiltApparatusAcceptsTheRecipeAndProducesTheResult(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        placeCore(helper, Direction.UP);
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);

        helper.assertTrue(
                machine(helper, APPARATUS)
                        .pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "a correctly built apparatus refused " + fixture.holder().id());
        helper.assertTrue(apparatus.isCrafting, "the push was accepted but no craft began");
        for (int index = 0; index < fixture.holder().value().pedestalItems().size(); index++) {
            ArcanePedestalTile pedestal = helper.getBlockEntity(PEDESTAL_SPOTS.get(index));
            helper.assertTrue(!pedestal.getStack().isEmpty(), "pedestal " + index + " was never staged");
        }
        helper.succeedWhen(() -> assertProduces(helper, fixture, apparatus));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anApparatusWithNoArcaneCoreRefusesThePush(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);

        helper.assertTrue(
                !machine(helper, APPARATUS)
                        .pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "an apparatus with no Arcane Core behind it accepted a pattern");
        assertNothingStranded(helper, apparatus);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anArcaneCoreOnTheWrongAxisRefusesThePush(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        placeCore(helper, Direction.NORTH);
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);

        helper.assertTrue(
                !machine(helper, APPARATUS)
                        .pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "an apparatus whose core faces the wrong axis accepted a pattern");
        assertNothingStranded(helper, apparatus);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void aPushThroughTheArcaneCoreDrivesTheApparatus(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = fixture(helper);
        placeCore(helper, Direction.UP);
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);

        helper.assertTrue(
                machine(helper, CORE)
                        .pushPattern(fixture.details(), ArsFixtures.inputsOf(fixture.details()), Direction.UP),
                "the Arcane Core refused a pattern its apparatus could take");
        helper.assertTrue(apparatus.isCrafting, "the push through the core never reached the apparatus");
        helper.succeedWhen(() -> assertProduces(helper, fixture, apparatus));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theArcaneCoreHandsBackWhatTheApparatusHolds(GameTestHelper helper) {
        placeCore(helper, Direction.UP);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CORE), null);
        helper.assertTrue(handler != null, "the Arcane Core exposed no item handler");
        helper.assertTrue(handler.getSlots() == 0, "a core with no apparatus reported slots");

        helper.setBlock(APPARATUS, BlockRegistry.ENCHANTING_APP_BLOCK.get().defaultBlockState());
        EnchantingApparatusTile apparatus = helper.getBlockEntity(APPARATUS);
        apparatus.setStack(new ItemStack(Items.DIAMOND));

        helper.assertTrue(handler.getSlots() > 0, "the cached handler did not notice the apparatus placed above it");
        int slot = -1;
        for (int index = 0; index < handler.getSlots(); index++) {
            if (handler.getStackInSlot(index).is(Items.DIAMOND)) {
                slot = index;
            }
        }
        helper.assertTrue(slot >= 0, "the core did not show the item held by its apparatus");
        helper.assertTrue(
                handler.extractItem(slot, 1, true).is(Items.DIAMOND),
                "a simulated extract through the core came up empty");
        helper.assertTrue(
                handler.extractItem(slot, 1, false).is(Items.DIAMOND), "the core would not give up the finished item");
        helper.assertTrue(apparatus.getStack().isEmpty(), "extracting through the core left the item in the apparatus");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void aFirstLevelEnchantmentPatternTurnsABookIntoAnEnchantedBook(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Fixture fixture = openFixture(
                helper,
                recipe -> recipe instanceof EnchantmentRecipe enchantment && enchantment.enchantLevel == 1,
                recipe -> new ItemStack(Items.BOOK));
        placeCore(helper, Direction.UP);
        enchantsInto(helper, fixture, Items.ENCHANTED_BOOK);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void aHigherLevelEnchantmentPatternTakesTheBookOneLevelBelow(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Level level = helper.getLevel();
        Fixture fixture = openFixture(
                helper,
                recipe -> recipe instanceof EnchantmentRecipe enchantment && enchantment.enchantLevel >= 2,
                recipe -> lowerLevelBook(recipe, level));
        placeCore(helper, Direction.UP);
        enchantsInto(helper, fixture, Items.ENCHANTED_BOOK);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void anArmorUpgradeKeepsThePiecesOwnDataAndCountsAsThePromisedPiece(GameTestHelper helper) {
        EnchantingApparatusCraftingMachine.clearCache();
        ArsRecipeResolver.clearCache();
        Level level = helper.getLevel();
        Fixture fixture = openFixture(
                helper,
                recipe -> ArsRecipeResolver.upgradesArmor(recipe.getType()),
                EnchantingApparatusCraftingMachineGameTest::armorBelow);
        placeCore(helper, Direction.UP);
        EnchantingApparatusTile apparatus = placeApparatus(helper, fixture);

        AEItemKey template =
                (AEItemKey) fixture.expected().inputs().get(0).get(0).what();
        ItemStack worn = template.toStack();
        worn.set(DataComponents.CUSTOM_NAME, Component.literal("Worn Robes"));
        AEItemKey wornKey = AEItemKey.of(worn);

        KeyCounter[] inputs = ArsFixtures.inputsOf(fixture.details());
        boolean swapped = false;
        for (int slot = 0; slot < inputs.length; slot++) {
            if (inputs[slot].get(template) > 0) {
                helper.assertTrue(
                        fixture.details().getInputs()[slot].isValid(wornKey, level),
                        "the pattern would not take the same armor piece carrying its own data");
                inputs[slot].remove(template, 1);
                inputs[slot].removeZeros();
                inputs[slot].add(wornKey, 1);
                swapped = true;
            }
        }
        helper.assertTrue(swapped, "the pattern has no slot for the armor piece");

        helper.assertTrue(
                machine(helper, APPARATUS).pushPattern(fixture.details(), inputs, Direction.UP),
                "the apparatus refused an armor piece carrying its own data");
        AEItemKey promised = (AEItemKey) fixture.expected().outputs().get(0).what();
        helper.succeedWhen(() -> {
            helper.assertTrue(!apparatus.isCrafting, "the apparatus is still crafting");
            AEItemKey made = AEItemKey.of(apparatus.getStack());
            helper.assertTrue(made != null && !made.equals(promised), "the upgrade dropped the piece's own data");
            helper.assertTrue(apparatus.getStack().has(DataComponents.CUSTOM_NAME), "the upgraded piece lost its name");
            helper.assertTrue(
                    promised.equals(CraftedOutputs.declaredFor(made)),
                    "the upgraded piece does not count as the piece the pattern promised");
        });
    }
}
