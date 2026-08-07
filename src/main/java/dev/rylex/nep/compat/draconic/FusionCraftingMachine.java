package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public class FusionCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final TileFusionCraftingCore core;

    public FusionCraftingMachine(TileFusionCraftingCore core) {
        this.core = core;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(DEContent.CRAFTING_CORE.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), DEContent.CRAFTING_CORE.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.draconicFusionCrafting();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = core.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.draconicFusionCrafting()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs, ejectionDirection);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(
            Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof FusionCraftingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not a fusion crafting or processing pattern");
        }

        FusionRecipeResolver.Plan plan = FusionRecipeResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no fusion recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("fusion crafting takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }

        if (core.isCrafting()) {
            return PushOutcome.retry("crafting core is already running a craft");
        }
        if (!core.getCatalystStack().isEmpty()) {
            return PushOutcome.retry("catalyst slot is occupied");
        }

        ItemStack result = plan.result().what() instanceof AEItemKey resultKey
                ? resultKey.toStack((int) plan.result().amount())
                : ItemStack.EMPTY;
        if (result.isEmpty()) {
            return PushOutcome.unsatisfiable("recipe result is not an item");
        }
        ItemStack held = core.getOutputStack();
        if (!held.isEmpty()
                && (!ItemStack.isSameItemSameComponents(held, result)
                        || held.getCount() + result.getCount() > result.getMaxStackSize())) {
            return PushOutcome.retry("output slot is obstructed");
        }

        if (!core.updateInjectors()) {
            return PushOutcome.retry("injectors are too close to the crafting core");
        }

        List<IFusionInjector> fillable = new ArrayList<>();
        List<Ingredient> unmatched = new ArrayList<>(plan.preloaded());
        for (IFusionInjector injector : core.getInjectors()) {
            ItemStack stack = injector.getInjectorStack();
            if (injector.getInjectorTier().index < plan.tier().index) {
                if (!stack.isEmpty()) {
                    return PushOutcome.retry(
                            "an injector below the recipe's tier is holding " + stack + ", which blocks the craft");
                }
                continue;
            }
            if (stack.isEmpty()) {
                fillable.add(injector);
                continue;
            }
            if (!removeFirstMatch(unmatched, stack)) {
                return PushOutcome.retry("injector is holding " + stack + ", which this recipe does not use");
            }
        }

        if (!unmatched.isEmpty()) {
            return PushOutcome.retry("this recipe keeps " + unmatched.size()
                    + " ingredient(s) that must be loaded into injectors by" + " hand, and they are not present");
        }
        int needed = plan.injectorItems().size() + plan.retainedItems().size();
        if (fillable.size() < needed) {
            return PushOutcome.retry("needs " + plan.injectorsNeeded() + " injectors of tier "
                    + plan.tier().name() + " or better, found "
                    + (fillable.size() + plan.preloaded().size()));
        }

        return commit(plan, fillable, ejectionDirection);
    }

    private static boolean removeFirstMatch(List<Ingredient> ingredients, ItemStack stack) {
        for (int i = 0; i < ingredients.size(); i++) {
            if (ingredients.get(i).test(stack)) {
                ingredients.remove(i);
                return true;
            }
        }
        return false;
    }

    private PushOutcome commit(
            FusionRecipeResolver.Plan plan, List<IFusionInjector> fillable, Direction ejectionDirection) {
        List<GenericStack> loading = new ArrayList<>(plan.injectorItems());
        loading.addAll(plan.retainedItems());

        List<IFusionInjector> filled = new ArrayList<>(loading.size());
        core.setCatalystStack(((AEItemKey) plan.catalyst().what())
                .toStack((int) plan.catalyst().amount()));
        for (int i = 0; i < loading.size(); i++) {
            IFusionInjector injector = fillable.get(i);
            injector.setInjectorStack(((AEItemKey) loading.get(i).what())
                    .toStack((int) loading.get(i).amount()));
            filled.add(injector);
        }
        FusionReclaimer.expect(core, plan.retainedItems(), ejectionDirection);

        core.startCraft();
        if (!core.isCrafting()) {
            core.setCatalystStack(ItemStack.EMPTY);
            for (IFusionInjector injector : filled) {
                injector.setInjectorStack(ItemStack.EMPTY);
            }
            FusionReclaimer.forget(core);
            return PushOutcome.retry("crafting core refused to start the craft");
        }
        return PushOutcome.accepted("fusion crafting " + plan.result());
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, core.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Fusion crafting core {} {} push: {} (output={})",
                core.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
