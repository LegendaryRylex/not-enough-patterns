package dev.rylex.nep.compat.draconic;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    private record InjectorSite(BlockPos pos, Direction facing) {}

    private static final List<InjectorSite> SITES = List.of(
            new InjectorSite(new BlockPos(1, 2, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 2, 4), Direction.WEST),
            new InjectorSite(new BlockPos(4, 2, 1), Direction.SOUTH),
            new InjectorSite(new BlockPos(4, 2, 7), Direction.NORTH),
            new InjectorSite(new BlockPos(1, 3, 4), Direction.EAST),
            new InjectorSite(new BlockPos(1, 1, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 3, 4), Direction.WEST),
            new InjectorSite(new BlockPos(7, 1, 4), Direction.WEST));

    private FusionChainGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void chainedFusionInputsAcceptWhatTheirOwnPatternMakes(GameTestHelper helper) {
        Level level = helper.getLevel();
        FusionRecipeResolver.clearCache();

        List<String> broken = new ArrayList<>();
        for (RecipeHolder<IFusionRecipe> holder : FusionRecipeResolver.candidates(level)) {
            EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, level);
            if (encoded == null) {
                continue;
            }
            for (int slot = 0; slot < encoded.inputs().size(); slot++) {
                List<GenericStack> options = encoded.inputs().get(slot);
                Item item = options.get(0).what() instanceof AEItemKey key ? key.getItem() : null;
                RecipeHolder<IFusionRecipe> maker =
                        item == null ? null : FusionRecipeResolver.resolveByOutputItem(level, item);
                if (maker == null || maker == holder) {
                    continue;
                }
                AEItemKey produced = AEItemKey.of(FusionResults.expectedResult(maker.value(), level));
                if (produced == null) {
                    continue;
                }
                boolean accepted = options.stream().anyMatch(option -> produced.equals(option.what()));
                if (!accepted) {
                    broken.add(holder.id().getPath() + "#" + slot);
                }
            }
        }

        helper.assertTrue(
                broken.isEmpty(), broken.size() + " chained fusion inputs reject their own pattern: " + broken);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aChainedTierCraftsFromTheKeyItsOwnPatternProduces(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();
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
        for (InjectorSite site : SITES) {
            helper.setBlock(
                    site.pos(),
                    DEContent.CHAOTIC_CRAFTING_INJECTOR
                            .get()
                            .defaultBlockState()
                            .setValue(CraftingInjector.FACING, site.facing()));
        }

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

        KeyCounter[] pushed = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < pushed.length; slot++) {
            pushed[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            pushed[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }

        ICraftingMachine machine = level.getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CORE), null);
        helper.assertTrue(machine != null, "the fusion crafting core exposed no crafting machine capability");
        helper.assertTrue(
                machine.pushPattern(details, pushed, Direction.UP),
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
            if (!java.util.Objects.equals(a, b)) {
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
        for (RecipeHolder<IFusionRecipe> holder : FusionRecipeResolver.candidates(level)) {
            EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, level);
            if (encoded == null) {
                continue;
            }
            for (int slot = 0; slot < encoded.inputs().size(); slot++) {
                GenericStack preferred = encoded.inputs().get(slot).get(0);
                if (!(preferred.what() instanceof AEItemKey key)) {
                    continue;
                }
                if (!key.equals(FusionResults.canonical(key, level))) {
                    broken.add(holder.id().getPath() + "#" + slot);
                }
            }
        }

        helper.assertTrue(broken.isEmpty(), broken.size() + " fusion inputs prefer a non canonical key: " + broken);
        helper.succeed();
    }
}
