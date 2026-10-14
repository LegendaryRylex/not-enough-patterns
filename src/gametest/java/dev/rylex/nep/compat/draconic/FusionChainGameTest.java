package dev.rylex.nep.compat.draconic;

import static dev.rylex.nep.compat.draconic.DraconicFixtures.clearCaches;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
import dev.rylex.nep.compat.draconic.DraconicFixtures.InjectorSite;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionChainGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_fusion_chain";

    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static final List<InjectorSite> SITES = DraconicFixtures.SITES;

    private FusionChainGameTest() {}

    private interface FusionInputVisitor {
        void visit(RecipeHolder<IFusionRecipe> holder, int slot, List<GenericStack> options);
    }

    private static void forEachFusionInput(Level level, FusionInputVisitor visitor) {
        for (RecipeHolder<IFusionRecipe> holder : FusionRecipeResolver.candidates(level)) {
            EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, level);
            if (encoded == null) {
                continue;
            }
            for (int slot = 0; slot < encoded.inputs().size(); slot++) {
                visitor.visit(holder, slot, encoded.inputs().get(slot));
            }
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void chainedFusionInputsAcceptWhatTheirOwnPatternMakes(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();

        List<String> broken = new ArrayList<>();
        forEachFusionInput(level, (holder, slot, options) -> {
            Item item = options.get(0).what() instanceof AEItemKey key ? key.getItem() : null;
            RecipeHolder<IFusionRecipe> maker =
                    item == null ? null : FusionRecipeResolver.resolveByOutputItem(level, item);
            if (maker == null || maker == holder) {
                return;
            }
            AEItemKey produced = AEItemKey.of(FusionResults.expectedResult(maker.value(), level));
            if (produced == null) {
                return;
            }
            boolean accepted = options.stream().anyMatch(option -> produced.equals(option.what()));
            if (!accepted) {
                broken.add(holder.id().getPath() + "#" + slot);
            }
        });

        helper.assertTrue(
                broken.isEmpty(), broken.size() + " chained fusion inputs reject their own pattern: " + broken);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aChainedTierCraftsFromTheKeyItsOwnPatternProduces(GameTestHelper helper) {
        clearCaches();
        Level level = helper.getLevel();

        RecipeHolder<IFusionRecipe> maker =
                FusionRecipeResolver.resolveByOutputItem(level, DEContent.PICKAXE_DRACONIC.get());
        RecipeHolder<IFusionRecipe> consumer =
                FusionRecipeResolver.resolveByOutputItem(level, DEContent.PICKAXE_CHAOTIC.get());
        helper.assertTrue(maker != null && consumer != null, "the draconic and chaotic pickaxe recipes did not load");

        EncodedIngredients expected = DraconicRecipeIngredients.fusion(consumer, level);
        helper.assertTrue(expected != null, "the chaotic pickaxe recipe produced no ingredient list");

        AEItemKey produced = AEItemKey.of(FusionResults.expectedResult(maker.value(), level));
        AEItemKey catalyst = (AEItemKey) expected.inputs().get(0).get(0).what();
        helper.assertTrue(
                produced != null && produced.equals(catalyst),
                "the chaotic pickaxe asks for a draconic pickaxe its own pattern never makes");

        helper.setBlock(CORE, DEContent.CRAFTING_CORE.get().defaultBlockState());
        TileFusionCraftingCore core = (TileFusionCraftingCore) helper.getBlockEntity(CORE);
        DraconicFixtures.placeInjectors(helper, DEContent.CHAOTIC_CRAFTING_INJECTOR.get(), SITES.size());

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        helper.assertTrue(
                inputs.size() == SITES.size() + 1,
                "this test lays out " + SITES.size() + " injectors but the recipe needs " + (inputs.size() - 1));

        ItemStack encoded = FusionCraftingPattern.encode(
                consumer.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
        helper.assertTrue(details != null, "the chaotic pickaxe pattern did not decode");

        ICraftingMachine machine = GameTestFixtures.craftingMachine(
                helper, CORE, "the fusion crafting core exposed no crafting machine capability");
        helper.assertTrue(
                machine.pushPattern(details, GameTestFixtures.inputsOf(details), Direction.UP),
                "the fusion core refused a chained craft driven by the previous tier's own output");
        helper.assertTrue(core.isCrafting(), "the core took the pattern but never started the craft");

        core.setFusionState(IFusionStateMachine.FusionState.CRAFTING);
        core.setCounter(Integer.MAX_VALUE - 1);
        core.tick();
        helper.assertTrue(!core.isCrafting(), "the core never finished the craft");

        AEItemKey crafted = AEItemKey.of(core.getOutputStack());
        helper.assertTrue(crafted != null, "the craft finished but the output slot is empty");
        AEItemKey promised = (AEItemKey) details.getOutputs().get(0).what();
        helper.assertTrue(
                promised.equals(crafted),
                "the core made a chaotic pickaxe the Import Card cannot collect; components differing from the "
                        + "pattern's promise: " + differingComponents(core.getOutputStack(), promised.toStack()));
        helper.succeed();
    }

    private static List<String> differingComponents(ItemStack made, ItemStack promised) {
        List<String> differing = new ArrayList<>();
        Set<DataComponentType<?>> types = new LinkedHashSet<>();
        made.getComponents().keySet().forEach(types::add);
        promised.getComponents().keySet().forEach(types::add);
        for (DataComponentType<?> type : types) {
            Object a = made.get(type);
            Object b = promised.get(type);
            if (!Objects.equals(a, b)) {
                differing.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)
                        + (a == null ? " (pattern only)" : b == null ? " (craft only)" : " (different value)"));
            }
        }
        return differing;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void chainedFusionPatternsShareOneKeyPerItem(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();

        List<String> broken = new ArrayList<>();
        forEachFusionInput(level, (holder, slot, options) -> {
            if (options.get(0).what() instanceof AEItemKey key && !key.equals(FusionResults.canonical(key, level))) {
                broken.add(holder.id().getPath() + "#" + slot);
            }
        });

        helper.assertTrue(broken.isEmpty(), broken.size() + " fusion inputs prefer a non canonical key: " + broken);
        helper.succeed();
    }
}
