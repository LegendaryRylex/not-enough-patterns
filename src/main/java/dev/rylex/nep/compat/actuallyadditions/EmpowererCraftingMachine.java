package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import de.ellpeck.actuallyadditions.api.misc.IDisplayStandItem;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsRecipeResolver.EmpoweringPlan;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.EmpoweringPattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public class EmpowererCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final TileEntityEmpowerer empowerer;

    public EmpowererCraftingMachine(TileEntityEmpowerer empowerer) {
        this.empowerer = empowerer;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(ActuallyBlocks.EMPOWERER.getItem());
        return new PatternContainerGroup(
                AEItemKey.of(icon), ActuallyBlocks.EMPOWERER.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.actuallyAdditionsEmpowering();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = empowerer.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.actuallyAdditionsEmpowering()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs) {
        if (!(patternDetails instanceof EmpoweringPattern pattern)) {
            return PushOutcome.unsatisfiable("not an empowering pattern");
        }

        RecipeHolder<EmpowererRecipe> holder = ActuallyAdditionsRecipeResolver.empoweringById(level, pattern.recipe());
        if (holder == null) {
            return PushOutcome.unsatisfiable("unknown empowering recipe " + pattern.recipe());
        }

        EmpoweringPlan plan = ActuallyAdditionsRecipeResolver.planEmpowering(holder, patternDetails);
        if (plan == null) {
            return PushOutcome.unsatisfiable("recipe " + pattern.recipe() + " does not match the pattern");
        }
        if (plan.batch() != 1) {
            return PushOutcome.unsatisfiable("an Empowerer stages one craft at a time, and every slot it uses holds a"
                    + " single item, but this pattern asks for " + plan.batch());
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("empowering takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }

        ItemStack base = single(plan.base());
        ItemStack[] modifiers = new ItemStack[EmpowererLayout.STANDS];
        for (int index = 0; index < EmpowererLayout.STANDS; index++) {
            modifiers[index] = single(plan.modifiers().get(index));
            if (modifiers[index].getItem() instanceof IDisplayStandItem) {
                return PushOutcome.unsatisfiable("a Display Stand runs " + modifiers[index]
                        + " as a lens instead of holding it as an ingredient");
            }
        }
        if (base.isEmpty()) {
            return PushOutcome.unsatisfiable("the recipe's base item is not an item");
        }
        if (!holder.value().matches(base, modifiers[0], modifiers[1], modifiers[2], modifiers[3])) {
            return PushOutcome.unsatisfiable("the Empowerer does not recognise this arrangement of the recipe's items");
        }

        TileEntityDisplayStand[] stands = EmpowererLayout.stands(level, empowerer.getBlockPos());
        if (stands == null) {
            return PushOutcome.retry("needs four Display Stands, three blocks out on each horizontal axis");
        }
        if (empowerer.processTime != 0 || empowerer.getCurrentRecipe() != null) {
            return PushOutcome.retry("the Empowerer is already running a craft");
        }
        if (!empowerer.inv.getStackInSlot(0).isEmpty()) {
            return PushOutcome.retry("the Empowerer is holding " + empowerer.inv.getStackInSlot(0));
        }

        int perTick = plan.energyPerStand() / Math.max(1, plan.time());
        for (int index = 0; index < EmpowererLayout.STANDS; index++) {
            TileEntityDisplayStand stand = stands[index];
            if (!stand.getStack().isEmpty()) {
                return PushOutcome.retry(
                        "the Display Stand at " + stand.getBlockPos() + " is holding " + stand.getStack());
            }
            if (stand.storage.getEnergyStored() < perTick) {
                return PushOutcome.retry("the Display Stand at " + stand.getBlockPos() + " cannot pay the " + perTick
                        + " FE/t this recipe costs");
            }
        }

        return commit(level, plan, stands, base, modifiers);
    }

    private PushOutcome commit(
            Level level, EmpoweringPlan plan, TileEntityDisplayStand[] stands, ItemStack base, ItemStack[] modifiers) {
        if (!empowerer.inv.insertItem(0, base.copy(), true).isEmpty()) {
            return PushOutcome.retry("the Empowerer refused " + base);
        }
        for (int index = 0; index < EmpowererLayout.STANDS; index++) {
            if (!stands[index].inv.insertItem(0, modifiers[index].copy(), true).isEmpty()) {
                return PushOutcome.retry(
                        "the Display Stand at " + stands[index].getBlockPos() + " refused " + modifiers[index]);
            }
        }

        empowerer.inv.insertItem(0, base.copy(), false);
        empowerer.setChanged();
        for (int index = 0; index < EmpowererLayout.STANDS; index++) {
            if (!stands[index].inv.insertItem(0, modifiers[index].copy(), false).isEmpty()) {
                rollBack(stands, index);
                return PushOutcome.retry("the Display Stand at " + stands[index].getBlockPos()
                        + " refused an item it had accepted a moment earlier");
            }
            stands[index].setChanged();
        }

        warnAboutTrappedResults(level, plan);
        return PushOutcome.accepted("empowering " + plan.result());
    }

    private void rollBack(TileEntityDisplayStand[] stands, int filled) {
        empowerer.inv.setStackInSlot(0, ItemStack.EMPTY);
        empowerer.setChanged();
        for (int index = 0; index < filled; index++) {
            stands[index].inv.setStackInSlot(0, ItemStack.EMPTY);
            stands[index].setChanged();
        }
    }

    private void warnAboutTrappedResults(Level level, EmpoweringPlan plan) {
        if (!NepConfig.debugLogging() || !(plan.result().what() instanceof AEItemKey key)) {
            return;
        }
        if (TileEntityEmpowerer.isPossibleInput(key.toStack(1))) {
            Nep.LOGGER.info(
                    "Empowerer {} is making {}, which is itself the base of an empowering recipe. Actually Additions"
                            + " only lets automation take an item out of an Empowerer when it is not a valid base, so"
                            + " an Import Card will not be able to collect this result.",
                    empowerer.getBlockPos(),
                    key.getItem());
        }
    }

    private static ItemStack single(GenericStack stack) {
        return stack.what() instanceof AEItemKey key ? key.toStack((int) stack.amount()) : ItemStack.EMPTY;
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, empowerer.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Empowerer {} {} push: {} (output={})",
                empowerer.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
